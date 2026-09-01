package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.biz.ReactiveOAuthMcpRuntimeService;
import io.github.pnoker.common.auth.config.OAuthJwtKeyProvider;
import io.github.pnoker.common.auth.config.OAuthProperties;
import io.github.pnoker.common.auth.entity.bo.McpConnectionAddBO;
import io.github.pnoker.common.auth.entity.bo.OAuthClientRegistrationBO;
import io.github.pnoker.common.auth.entity.builder.McpConnectionBuilder;
import io.github.pnoker.common.auth.entity.builder.OAuthClientBuilder;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthConsentRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationResponseVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.auth.exception.OAuthProtocolException;
import io.github.pnoker.common.auth.repository.ReactiveOAuthMcpStore;
import io.github.pnoker.common.auth.service.ReactivePrincipalService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.auth.support.ReactiveAuthScheduler;
import io.github.pnoker.common.auth.tool.McpOpenApiAggregator;
import io.github.pnoker.common.auth.tool.ToolQuality;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpCallToolRequestDTO;
import io.github.pnoker.common.entity.dto.McpCallToolResponseDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpPrincipalContextDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.McpConfirmationStatusEnum;
import io.github.pnoker.common.enums.McpRiskLevelEnum;
import io.github.pnoker.common.enums.OAuthClientTypeEnum;
import io.github.pnoker.common.enums.OAuthGrantTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.OAuthJwtVerifier;
import io.github.pnoker.common.utils.PasswordUtil;
import io.github.pnoker.common.utils.UuidV7;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.HexFormat;

/** Fully non-blocking OAuth and MCP runtime backed exclusively by the R2DBC store. */
@Service
@RequiredArgsConstructor
public class ReactiveOAuthMcpRuntimeServiceImpl implements ReactiveOAuthMcpRuntimeService {

    private final ReactiveOAuthMcpStore store;
    private final ReactivePrincipalService principalService;
    private final ReactiveTenantMembershipService membershipService;
    private final OAuthProperties properties;
    private final OAuthJwtKeyProvider keyProvider;
    private final TransactionalOperator transactionalOperator;
    private final OAuthClientBuilder clientBuilder;
    private final McpConnectionBuilder connectionBuilder;
    private final McpOpenApiAggregator openApiAggregator;

    @Override
    public Mono<Map<String, Object>> authorizationServerMetadata() {
        return Mono.fromSupplier(() -> orderedMap(
                "issuer", properties.getIssuer(),
                "authorization_endpoint", properties.getIssuer() + McpConstant.OAUTH2_AUTHORIZE,
                "token_endpoint", properties.getIssuer() + McpConstant.OAUTH2_TOKEN,
                "jwks_uri", properties.getIssuer() + McpConstant.OAUTH2_JWKS,
                "revocation_endpoint", properties.getIssuer() + McpConstant.OAUTH2_REVOKE,
                "registration_endpoint", properties.getIssuer() + McpConstant.OAUTH2_REGISTER,
                "response_types_supported", List.of(McpConstant.OAuth.RESPONSE_TYPE_CODE),
                "grant_types_supported", List.of(McpConstant.OAuth.GRANT_AUTHORIZATION_CODE,
                        McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS, McpConstant.OAuth.GRANT_REFRESH_TOKEN),
                "code_challenge_methods_supported", List.of(McpConstant.OAuth.CODE_CHALLENGE_METHOD_S256),
                "scopes_supported", McpConstant.Scope.SUPPORTED,
                "token_endpoint_auth_methods_supported", List.of(McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_BASIC,
                        McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_POST, McpConstant.OAuth.AUTH_METHOD_NONE)));
    }

    @Override
    public Mono<Map<String, Object>> jwks() {
        return Mono.fromSupplier(() -> {
            RSAPublicKey key = keyProvider.verificationKey();
            return Map.of("keys", List.of(orderedMap("kty", "RSA", "use", "sig",
                    "kid", OAuthJwtKeyProvider.KEY_ID, "alg", "RS256",
                    "n", DecodeUtil.base64UrlWithoutLeadingZero(key.getModulus().toByteArray()),
                    "e", DecodeUtil.base64UrlWithoutLeadingZero(key.getPublicExponent().toByteArray()))));
        });
    }

    @Override
    public Mono<OAuthClientRegistrationResponseVO> registerClient(OAuthClientRegistrationBO request,
                                                                    RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMap(header -> {
            OAuthClientRegistrationBO value = request == null ? new OAuthClientRegistrationBO() : request;
            String name = StringUtils.trimToNull(value.getClientName());
            if (name == null) return oauthError(400, "invalid_client_metadata", "client_name is required");
            String type = value.getClientType() == null ? OAuthClientTypeEnum.PUBLIC.getValue()
                    : value.getClientType().getValue();
            Set<String> grants = value.getGrantTypes() == null ? new LinkedHashSet<>() : value.getGrantTypes().stream()
                    .filter(Objects::nonNull).map(OAuthGrantTypeEnum::getValue)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (grants.isEmpty()) grants.add(OAuthClientTypeEnum.PUBLIC.getValue().equals(type)
                    ? McpConstant.OAuth.GRANT_AUTHORIZATION_CODE : McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS);
            Set<String> scopes = registrationScopes(value.getScope());
            if (scopes.isEmpty()) scopes = new LinkedHashSet<>(List.of(McpConstant.Scope.TOOLS_LIST, McpConstant.Scope.TOOLS_CALL));
            Set<String> redirects = split(value.getRedirectUris());
            if (grants.contains(McpConstant.OAuth.GRANT_AUTHORIZATION_CODE) && redirects.isEmpty()) {
                return oauthError(400, "invalid_redirect_uri", "redirect_uris is required");
            }
            final Set<String> finalGrants = Set.copyOf(grants);
            final Set<String> finalScopes = Set.copyOf(scopes);
            final Set<String> finalRedirects = Set.copyOf(redirects);
            Long serviceAccount = value.getServiceAccountPrincipalId();
            Mono<Void> serviceAccountCheck = grants.contains(McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS)
                    ? requireServiceAccount(serviceAccount, header.getTenantId()) : Mono.empty();
            return serviceAccountCheck.then(Mono.defer(() -> {
                String clientId = McpConstant.OAuth.CLIENT_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
                boolean confidential = OAuthClientTypeEnum.CONFIDENTIAL.getValue().equals(type);
                String secret = confidential ? randomToken() : null;
                Mono<String> hash = confidential ? Mono.fromCallable(() -> PasswordUtil.encode(secret))
                        .subscribeOn(ReactiveAuthScheduler.CRYPTO) : Mono.just("");
                return hash.flatMap(secretHash -> {
                    OAuthRegisteredClientRecord record = new OAuthRegisteredClientRecord();
                    record.setClientId(clientId); record.setClientName(name); record.setClientType(type);
                    record.setOwnerPrincipalId(header.getPrincipalId()); record.setServiceAccountPrincipalId(serviceAccount == null ? 0L : serviceAccount);
                    record.setTenantId(header.getTenantId()); record.setClientSecretHash(secretHash);
                    record.setClientAuthMethods(confidential ? McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_BASIC + " " + McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_POST : McpConstant.OAuth.AUTH_METHOD_NONE);
                    record.setAuthorizationGrantTypes(String.join(" ", finalGrants)); record.setRedirectUris(String.join(" ", finalRedirects));
                    record.setScopes(String.join(" ", finalScopes)); record.setRequirePkce((byte) (finalGrants.contains(McpConstant.OAuth.GRANT_AUTHORIZATION_CODE) && !confidential ? 1 : 0));
                    record.setRequireConsent((byte) 0); record.setEnableFlag((byte) 0);
                    return transactionalOperator.transactional(store.insertClient(record)).flatMap(rows -> rows == 1
                            ? Mono.just(OAuthClientRegistrationResponseVO.builder().clientId(clientId).clientName(name).clientType(type)
                            .grantTypes(finalGrants).redirectUris(finalRedirects).scope(String.join(" ", finalScopes))
                            .tokenEndpointAuthMethod(confidential ? McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_BASIC : McpConstant.OAuth.AUTH_METHOD_NONE)
                            .clientSecret(secret).build())
                            : oauthError(500, "server_error", "client registration failed"));
                });
            }));
        });
    }

    @Override
    public Flux<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMapMany(header -> store.listClientsByOwner(header.getPrincipalId(), header.getTenantId()))
                .map(clientBuilder::buildVOByRecord);
    }

    @Override
    public Mono<URI> authorize(Map<String, String> params, RequestHeader.PrincipalHeader principal) {
        Map<String, String> request = params == null ? Map.of() : params;
        return requirePrincipal(principal).flatMap(header -> {
            if (!McpConstant.OAuth.RESPONSE_TYPE_CODE.equals(request.get("response_type"))) {
                return oauthError(400, "unsupported_response_type", "only code is supported");
            }
            String clientId = StringUtils.trimToNull(request.get(McpConstant.Field.CLIENT_ID));
            return requireClient(clientId).flatMap(client -> {
                if (!enabled(client.getEnableFlag())) return oauthError(400, "unauthorized_client", "client is disabled");
                if (!contains(client.getAuthorizationGrantTypes(), McpConstant.OAuth.GRANT_AUTHORIZATION_CODE)) {
                    return oauthError(400, "unauthorized_client", "authorization_code is not enabled");
                }
                String redirect = request.get(McpConstant.Field.REDIRECT_URI);
                if (!split(client.getRedirectUris()).contains(redirect)) return oauthError(400, "invalid_request", "redirect_uri mismatch");
                if (one(client.getRequirePkce()) && (!McpConstant.OAuth.CODE_CHALLENGE_METHOD_S256.equals(request.get(McpConstant.Field.CODE_CHALLENGE_METHOD))
                        || StringUtils.isBlank(request.get(McpConstant.Field.CODE_CHALLENGE)))) {
                    return oauthError(400, "invalid_request", "PKCE S256 is required");
                }
                Long tenantId = positiveLong(request.get(McpConstant.Field.TENANT_ID), header.getTenantId());
                return membershipService.isTenantMember(tenantId, header.getPrincipalId()).flatMap(member -> {
                    if (!member) return oauthError(400, "invalid_request", "principal is not a member of the tenant");
                    if (!Objects.equals(client.getTenantId(), tenantId)) return oauthError(400, "invalid_request", "client is outside the tenant");
                    Long connectionId = positiveLong(request.get(McpConstant.Field.MCP_CONNECTION_ID), null);
                    Mono<McpConnectionRecord> connection = connectionId == null
                            ? store.getActiveConnection(client.getClientId(), header.getPrincipalId(), tenantId, McpConstant.OAuth.GRANT_AUTHORIZATION_CODE)
                            : store.getConnection(connectionId);
                    return connection.flatMap(value -> validateConnection(value, client.getClientId(), header.getPrincipalId(), tenantId,
                                    McpConstant.OAuth.GRANT_AUTHORIZATION_CODE)
                            .then(ensureConsent(client, header, tenantId, request))
                            .then(issueAuthorization(client, value, request, header, tenantId, redirect)))
                            .switchIfEmpty(oauthError(400, "invalid_request", "active MCP connection is required"));
                });
            });
        });
    }

    @Override
    public Mono<Map<String, Object>> token(Map<String, String> form, String authorizationHeader) {
        Map<String, String> request = form == null ? Map.of() : form;
        String grant = request.get(McpConstant.Field.GRANT_TYPE);
        return switch (StringUtils.defaultString(grant)) {
            case McpConstant.OAuth.GRANT_AUTHORIZATION_CODE -> authorizationCodeToken(request, authorizationHeader);
            case McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS -> clientCredentialsToken(request, authorizationHeader);
            case McpConstant.OAuth.GRANT_REFRESH_TOKEN -> refreshToken(request, authorizationHeader);
            default -> oauthError(400, "unsupported_grant_type", "unsupported grant_type");
        };
    }

    @Override
    public Mono<McpIntrospectResponseDTO> introspect(String token) {
        return Mono.defer(() -> {
            try {
                Claims claims = verify(token);
                Long tenant = number(claims.get(McpConstant.Field.TENANT_ID));
                Long principal = number(claims.getSubject());
                Long connection = number(claims.get(McpConstant.Field.MCP_CONNECTION_ID));
                String clientId = text(claims.get(McpConstant.Field.CLIENT_ID));
                return store.getAuthorizationByAccessTokenJti(claims.getId())
                        .filter(this::activeAuthorization)
                        .flatMap(auth -> store.getConnection(connection).filter(value -> usable(value, clientId, principal, tenant)))
                        .flatMap(value -> principalService.getById(tenant, principal)
                                .filter(p -> p.getEnableFlag() == EnableFlagEnum.ENABLE)
                                .zipWith(membershipService.isTenantMember(tenant, principal)))
                        .filter(tuple -> tuple.getT2())
                        .map(tuple -> McpIntrospectResponseDTO.builder().active(true).iss(claims.getIssuer()).aud(claims.getAudience())
                                .sub(claims.getSubject()).jti(claims.getId()).exp(claims.getExpiration().toInstant().getEpochSecond())
                                .iat(claims.getIssuedAt().toInstant().getEpochSecond()).tenantId(String.valueOf(tenant)).principalId(String.valueOf(principal))
                                .principalType(text(claims.get(McpConstant.Field.PRINCIPAL_TYPE))).principalName(tuple.getT1().getPrincipalName())
                                .displayName(tuple.getT1().getDisplayName()).clientId(clientId).mcpConnectionId(String.valueOf(connection))
                                .grantType(text(claims.get(McpConstant.Field.GRANT_TYPE))).scope(text(claims.get(McpConstant.Field.SCOPE))).build())
                        .defaultIfEmpty(McpIntrospectResponseDTO.inactive())
                        .onErrorReturn(McpIntrospectResponseDTO.inactive());
            } catch (RuntimeException ignored) {
                return Mono.just(McpIntrospectResponseDTO.inactive());
            }
        });
    }

    @Override
    public Mono<Map<String, Object>> revoke(Map<String, String> form, String authorizationHeader) {
        String token = form == null ? null : form.get("token");
        if (StringUtils.isBlank(token)) return oauthError(400, "invalid_request", "token is required");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Mono.defer(() -> {
            try {
                Claims claims = verify(token);
                return revokeAccessToken(claims, form, authorizationHeader, now);
            } catch (RuntimeException ignored) {
                return revokeRefreshToken(token, form, authorizationHeader, now);
            }
        });
    }

    private Mono<Map<String, Object>> revokeAccessToken(Claims claims, Map<String, String> form,
                                                         String authorizationHeader, LocalDateTime revokedAt) {
        String clientId = text(claims.get(McpConstant.Field.CLIENT_ID));
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(claims.getId())) {
            return oauthError(401, "invalid_client", "token client authentication failed");
        }
        return requireClient(clientId)
                .flatMap(client -> authenticateClient(client, form, authorizationHeader)
                        .then(store.getAuthorizationByAccessTokenJti(claims.getId())
                                .filter(auth -> Objects.equals(auth.getClientId(), clientId))
                                .flatMap(auth -> store.revokeAuthorizationByAccessTokenJti(claims.getId(), "revoke", revokedAt))
                                .thenReturn(Map.of("revoked", true))));
    }

    private Mono<Map<String, Object>> revokeRefreshToken(String token, Map<String, String> form,
                                                          String authorizationHeader, LocalDateTime revokedAt) {
        String clientId = resolveClientId(form, authorizationHeader);
        if (StringUtils.isBlank(clientId)) return oauthError(401, "invalid_client", "client authentication is required");
        return requireClient(clientId)
                .flatMap(client -> authenticateClient(client, form, authorizationHeader)
                        .then(store.getAuthorizationByRefreshTokenHash(sha256(token)))
                        .flatMap(auth -> Objects.equals(auth.getClientId(), clientId)
                                ? store.revokeAuthorizationByRefreshTokenHash(sha256(token), "revoke", revokedAt)
                                : oauthError(401, "invalid_client", "token client authentication failed"))
                        .thenReturn(Map.of("revoked", true)));
    }

    @Override
    public Mono<Integer> refreshToolCatalog() {
        return Mono.fromCallable(() -> openApiAggregator.toolQualityByApiCode())
                .subscribeOn(ReactiveAuthScheduler.CRYPTO)
                .zipWith(store.listRegistryToolCandidates().collectList())
                .flatMapMany(tuple -> Flux.fromIterable(tuple.getT2()).concatMap(candidate -> {
                    ToolQuality quality = tuple.getT1().get(candidate.getApiCode());
                    applyQuality(candidate, quality);
                    return store.upsertTool(candidate);
                })).reduce(0, Integer::sum);
    }

    @Override
    public Mono<List<McpConnectionVO>> listConnections(RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMapMany(header -> store.listConnections(header.getTenantId(), header.getPrincipalId()))
                .map(connectionBuilder::buildVOByRecord).collectList();
    }

    @Override
    public Mono<McpConnectionVO> createConnection(McpConnectionAddBO request, RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMap(header -> {
            if (request == null || StringUtils.isBlank(request.getClientId())) return oauthError(400, "invalid_request", "client_id is required");
            Long owner = request.getPrincipalId() == null ? header.getPrincipalId() : request.getPrincipalId();
            Long tenant = header.getTenantId();
            String grant = request.getGrantType() == null ? McpConstant.OAuth.GRANT_AUTHORIZATION_CODE : request.getGrantType().getValue();
            return requireClient(request.getClientId()).filter(client -> Objects.equals(client.getTenantId(), tenant)
                            && Objects.equals(client.getOwnerPrincipalId(), header.getPrincipalId()))
                    .switchIfEmpty(oauthError(403, "access_denied", "client is outside the principal scope"))
                    .flatMap(client -> validateConnectionPrincipal(owner, tenant, request.getPrincipalType())
                    .then(Mono.defer(() -> {
                        McpConnectionRecord value = new McpConnectionRecord(); value.setConnectionName(request.getConnectionName()); value.setClientId(request.getClientId());
                        value.setPrincipalId(owner); value.setPrincipalType(request.getPrincipalType() == null ? PrincipalTypeEnum.USER.getValue() : request.getPrincipalType().getValue());
                        value.setTenantId(tenant); value.setGrantType(grant); value.setEnableFlag((byte) 0); value.setExpireTime(request.getExpireTime());
                        value.setCreatorId(header.getPrincipalId()); value.setCreatorName(header.getPrincipalName());
                        return transactionalOperator.transactional(store.insertConnection(value)).flatMap(rows -> rows == 1
                                ? Mono.just(connectionBuilder.buildVOByRecord(value)) : oauthError(500, "server_error", "connection creation failed"));
                    })));
        });
    }

    @Override
    public Mono<Void> revokeConnection(Long id, RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMap(header -> store.revokeConnection(id, header.getTenantId(), header.getPrincipalId(), LocalDateTime.now(ZoneOffset.UTC)))
                .flatMap(rows -> rows == 1 ? Mono.empty() : oauthError(404, "not_found", "connection not found"));
    }

    @Override
    public Mono<Void> replaceConnectionTools(Long id, List<String> toolIds, RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMap(header -> transactionalOperator.transactional(store.getConnection(id)
                .switchIfEmpty(oauthError(404, "not_found", "connection not found"))
                .flatMap(connection -> validateConnectionOwner(connection, header)
                        .then(validateToolIds(toolIds))
                        .then(store.deleteConnectionTools(id))
                        .thenMany(Flux.fromIterable(toolIds == null ? List.<String>of() : toolIds).distinct()
                                .concatMap(tool -> store.insertConnectionTool(nextId(), id, tool,
                                        header.getPrincipalId(), header.getPrincipalName())))
                        .then())));
    }

    private Mono<Void> validateToolIds(List<String> toolIds) {
        return Flux.fromIterable(toolIds == null ? List.<String>of() : toolIds).distinct()
                .concatMap(tool -> store.getTool(tool).switchIfEmpty(oauthError(400, "invalid_request", "tool id is invalid")))
                .then();
    }

    @Override
    public Flux<String> listConnectionToolIds(Long id, RequestHeader.PrincipalHeader principal) {
        return requirePrincipal(principal).flatMapMany(header -> store.listConnectionToolIds(id, header.getTenantId(), header.getPrincipalId()));
    }

    @Override
    public Mono<McpToolListResponseDTO> listTools(String token) {
        return context(token).flatMapMany(ctx -> {
            requireScope(ctx.scopes(), McpConstant.Scope.TOOLS_LIST, McpConstant.Scope.TOOLS_CALL);
            return store.listTools(ctx.tenantId(), ctx.principalId(), ctx.connectionId(), ctx.allowHighRisk()).map(this::toolDefinition);
        }).collectList().map(tools -> McpToolListResponseDTO.builder().tools(tools).build());
    }

    @Override
    public Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request) {
        McpCallToolRequestDTO value = request == null ? new McpCallToolRequestDTO() : request;
        return context(value.getToken()).flatMap(ctx -> {
            requireScope(ctx.scopes(), McpConstant.Scope.TOOLS_CALL);
            return store.resolveTool(ctx.tenantId(), ctx.principalId(), ctx.connectionId(), value.getToolName(), ctx.allowHighRisk())
                    .switchIfEmpty(oauthError(403, "access_denied", "tool is not visible"))
                    .flatMap(tool -> store.touchConnection(ctx.connectionId(), LocalDateTime.now(ZoneOffset.UTC))
                            .then(authorize(ctx, tool, value)).map(decision -> McpCallToolResponseDTO.builder().decision(decision.getDecision())
                                    .confirmId(decision.getConfirmId()).message(decision.getMessage()).riskLevel(tool.getRiskLevel())
                                    .tool(resolve(tool)).principal(ctx.principalContext()).build()));
        });
    }

    @Override
    public Mono<Void> audit(McpAuditCommandDTO source) {
        if (source == null) return Mono.empty();
        McpAuditCommand value = new McpAuditCommand(); value.setTraceId(source.getTraceId()); value.setTenantId(source.getTenantId());
        value.setPrincipalId(source.getPrincipalId()); value.setPrincipalType(source.getPrincipalType()); value.setClientId(source.getClientId());
        value.setConnectionId(source.getConnectionId()); value.setToolId(source.getToolId()); value.setToolName(source.getToolName());
        value.setPermissionCode(source.getPermissionCode()); value.setRiskLevel(source.getRiskLevel()); value.setConfirmId(source.getConfirmId());
        value.setIdempotencyKey(source.getIdempotencyKey()); value.setArgumentDigest(source.getArgumentDigest()); value.setStatus(source.getStatus());
        value.setErrorCode(source.getErrorCode()); value.setDurationMs(source.getDurationMs()); value.setClientName(source.getClientName());
        value.setClientVersion(source.getClientVersion()); value.setRemoteIp(source.getRemoteIp());
        return store.insertAudit(value).then();
    }

    private Mono<Map<String, Object>> authorizationCodeToken(Map<String, String> form, String authorizationHeader) {
        String code = form.get("code"); if (StringUtils.isBlank(code)) return oauthError(400, "invalid_request", "code is required");
        String clientId = StringUtils.trimToNull(form.get(McpConstant.Field.CLIENT_ID));
        return store.getAuthorizationByCodeHash(sha256(code)).switchIfEmpty(oauthError(400, "invalid_grant", "authorization code is invalid"))
                .flatMap(auth -> {
                    if (!activeCode(auth) || (clientId != null && !Objects.equals(clientId, auth.getClientId()))) return oauthError(400, "invalid_grant", "authorization code is invalid");
                    return requireClient(auth.getClientId()).flatMap(client -> authenticateClient(client, form, authorizationHeader)
                            .then(Mono.defer(() -> verifyRedirectUri(auth, form.get(McpConstant.Field.REDIRECT_URI)))
                                    .then(Mono.defer(() -> verifyPkce(auth, form.get("code_verifier"))))
                                    .then(Mono.defer(() -> issueTokens(auth, client, true, null, sha256(code))))));
                });
    }

    private Mono<Map<String, Object>> clientCredentialsToken(Map<String, String> form, String authorizationHeader) {
        String clientId = resolveClientId(form, authorizationHeader);
        return requireClient(clientId).flatMap(client -> {
            if (!contains(client.getAuthorizationGrantTypes(), McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS)) return oauthError(400, "unauthorized_client", "grant is not enabled");
            return authenticateClient(client, form, authorizationHeader).then(Mono.defer(() -> {
                Long principal = client.getServiceAccountPrincipalId(); Long tenant = client.getTenantId();
                if (principal == null || principal <= 0) return oauthError(400, "invalid_client", "service account is required");
                return principalService.getById(tenant, principal).filter(p -> p.getEnableFlag() == EnableFlagEnum.ENABLE)
                        .switchIfEmpty(oauthError(400, "invalid_client", "service account is inactive"))
                        .then(membershipService.isTenantMember(tenant, principal)).flatMap(member -> member
                                ? store.getActiveConnection(clientId, principal, tenant, McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS)
                                .switchIfEmpty(oauthError(400, "invalid_grant", "active MCP connection is required"))
                                .flatMap(connection -> {
                                    String scopes = String.join(" ", requestedScopes(form.get("scope"), client));
                                    OAuthAuthorizationRecord auth = authorization(client, principal, tenant, connection.getId(), McpConstant.OAuth.GRANT_CLIENT_CREDENTIALS, scopes);
                                    return transactionalOperator.transactional(store.insertAuthorization(auth)
                                            .then(issueTokens(auth, client, false, null, null)));
                                }) : oauthError(400, "invalid_client", "service account is not a tenant member"));
            }));
        });
    }

    private Mono<Map<String, Object>> refreshToken(Map<String, String> form, String authorizationHeader) {
        String refresh = form.get(McpConstant.Field.REFRESH_TOKEN); if (StringUtils.isBlank(refresh)) return oauthError(400, "invalid_request", "refresh_token is required");
        String presentedHash = sha256(refresh);
        return store.getAuthorizationByRefreshTokenHash(presentedHash)
                .switchIfEmpty(store.getAuthorizationByPreviousRefreshTokenHash(presentedHash)
                        .flatMap(replayed -> store.revokeAuthorizationByAccessTokenJti(replayed.getAccessTokenJti(), "refresh_token_replayed", LocalDateTime.now(ZoneOffset.UTC))
                                .then(oauthError(400, "invalid_grant", "refresh token has been revoked"))))
                .switchIfEmpty(oauthError(400, "invalid_grant", "refresh token is invalid or expired"))
                .flatMap(auth -> requireClient(auth.getClientId()).flatMap(client -> authenticateClient(client, form, authorizationHeader)
                        .then(activeRefresh(auth) ? issueTokens(auth, client, true, presentedHash, null)
                                : oauthError(400, "invalid_grant", "refresh token is invalid or expired"))));
    }

    private Mono<Map<String, Object>> issueTokens(OAuthAuthorizationRecord auth, OAuthRegisteredClientRecord client,
                                                   boolean refresh, String previousRefresh, String expectedCode) {
        return principalService.getById(auth.getTenantId(), auth.getPrincipalId()).filter(p -> p.getEnableFlag() == EnableFlagEnum.ENABLE)
                .switchIfEmpty(oauthError(400, "invalid_grant", "principal is inactive"))
                .zipWith(membershipService.isTenantMember(auth.getTenantId(), auth.getPrincipalId()))
                .flatMap(tuple -> tuple.getT2() ? Mono.defer(() -> {
                    LocalDateTime issued = LocalDateTime.now(ZoneOffset.UTC);
                    Set<String> scopes = split(auth.getAuthorizedScopes());
                    String jti = UUID.randomUUID().toString();
                    LocalDateTime accessExpires = issued.plus(scopes.contains(McpConstant.Scope.TOOLS_CALL) || scopes.contains(McpConstant.Scope.TOOLS_CALL_HIGH)
                            ? properties.getAccessTokenTtl() : properties.getReadOnlyAccessTokenTtl());
                    Map<String, Object> claims = orderedMap(McpConstant.Field.PRINCIPAL_TYPE, auth.getPrincipalType(),
                            McpConstant.Field.TENANT_ID, auth.getTenantId(), McpConstant.Field.CLIENT_ID, client.getClientId(),
                            McpConstant.Field.MCP_CONNECTION_ID, auth.getMcpConnectionId(), McpConstant.Field.GRANT_TYPE, auth.getAuthorizationGrantType(),
                            McpConstant.Field.SCOPE, String.join(" ", scopes));
                    String accessToken = Jwts.builder().header().keyId(OAuthJwtKeyProvider.KEY_ID).and().issuer(properties.getIssuer())
                            .audience().add(properties.getAudience()).and().id(jti).subject(String.valueOf(auth.getPrincipalId()))
                            .issuedAt(java.util.Date.from(issued.toInstant(ZoneOffset.UTC))).notBefore(java.util.Date.from(issued.minusSeconds(5).toInstant(ZoneOffset.UTC)))
                            .expiration(java.util.Date.from(accessExpires.toInstant(ZoneOffset.UTC))).claims(claims)
                            .signWith(keyProvider.signingKey(), Jwts.SIG.RS256).compact();
                    String refreshToken = refresh ? randomToken() : null;
                    LocalDateTime refreshExpires = refresh ? issued.plus(properties.getRefreshTokenTtl()) : null;
                    Mono<Integer> persisted = expectedCode != null
                            ? store.activateAuthorizationCode(expectedCode, auth.getId(), jti, issued, accessExpires,
                            refresh ? sha256(refreshToken) : null, previousRefresh, refresh ? issued : null, refreshExpires, JsonUtil.toJsonString(claims))
                            : previousRefresh != null
                            ? store.rotateAuthorizationRefreshToken(auth.getId(), previousRefresh, jti, issued, accessExpires,
                            sha256(refreshToken), issued, refreshExpires, JsonUtil.toJsonString(claims))
                            : store.activateAuthorizationTokens(auth.getId(), "", jti, issued, accessExpires,
                            refresh ? sha256(refreshToken) : null, null, refresh ? issued : null, refreshExpires, JsonUtil.toJsonString(claims));
                    return persisted.flatMap(rows -> rows == 1 ? Mono.just(tokenResponse(accessToken, refreshToken, scopes, accessExpires, issued))
                            : oauthError(400, "invalid_grant", "authorization has already been used"));
                }) : oauthError(400, "invalid_grant", "principal is not a tenant member"));
    }

    private Mono<Void> verifyPkce(OAuthAuthorizationRecord auth, String verifier) {
        Map<String, Object> metadata = parseMap(auth.getTokenMetadata()); String challenge = text(metadata.get(McpConstant.Field.CODE_CHALLENGE));
        if (StringUtils.isBlank(challenge)) return Mono.empty();
        if (StringUtils.isBlank(verifier) || !challenge.equals(sha256Base64(verifier))) return oauthError(400, "invalid_grant", "PKCE verification failed");
        return Mono.empty();
    }

    private Mono<Void> verifyRedirectUri(OAuthAuthorizationRecord auth, String redirectUri) {
        String expected = text(parseMap(auth.getTokenMetadata()).get(McpConstant.Field.REDIRECT_URI));
        if (StringUtils.isBlank(expected) || !Objects.equals(expected, redirectUri)) {
            return oauthError(400, "invalid_grant", "redirect_uri mismatch");
        }
        return Mono.empty();
    }

    private Mono<URI> issueAuthorization(OAuthRegisteredClientRecord client, McpConnectionRecord connection,
                                          Map<String, String> params, RequestHeader.PrincipalHeader principal, Long tenant, String redirect) {
        String code = randomToken(); LocalDateTime issued = LocalDateTime.now(ZoneOffset.UTC);
        OAuthAuthorizationRecord auth = authorization(client, principal.getPrincipalId(), tenant, connection.getId(), McpConstant.OAuth.GRANT_AUTHORIZATION_CODE,
                String.join(" ", requestedScopes(params.get("scope"), client)));
        auth.setPrincipalType(StringUtils.defaultIfBlank(principal.getPrincipalType(), PrincipalTypeEnum.USER.getValue()));
        auth.setStateHash(sha256(params.get("state"))); auth.setAuthorizationCodeHash(sha256(code)); auth.setAuthorizationCodeIssued(issued);
        auth.setAuthorizationCodeExpires(issued.plus(properties.getAuthorizationCodeTtl()));
        auth.setTokenMetadata(JsonUtil.toJsonString(orderedMap(McpConstant.Field.REDIRECT_URI, redirect,
                McpConstant.Field.CODE_CHALLENGE, params.get(McpConstant.Field.CODE_CHALLENGE), McpConstant.Field.CODE_CHALLENGE_METHOD, params.get(McpConstant.Field.CODE_CHALLENGE_METHOD))));
        return transactionalOperator.transactional(store.insertAuthorization(auth)).flatMap(rows -> rows == 1 ? Mono.defer(() -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirect).queryParam("code", code);
            if (StringUtils.isNotBlank(params.get("state"))) builder.queryParam("state", params.get("state"));
            return Mono.just(builder.build(true).toUri());
        }) : oauthError(500, "server_error", "authorization failed"));
    }

    private Mono<Void> ensureConsent(OAuthRegisteredClientRecord client, RequestHeader.PrincipalHeader principal,
                                     Long tenantId, Map<String, String> request) {
        if (!one(client.getRequireConsent())) {
            return Mono.empty();
        }
        Set<String> requested = requestedScopes(request.get("scope"), client);
        return store.getConsent(client.getId(), principal.getPrincipalId(), tenantId)
                .flatMap(consent -> split(consent.getScopes()).containsAll(requested)
                        ? Mono.<Void>empty()
                        : persistConsentOrReject(client, principal, tenantId, requested, request))
                .switchIfEmpty(persistConsentOrReject(client, principal, tenantId, requested, request));
    }

    private Mono<Void> persistConsentOrReject(OAuthRegisteredClientRecord client,
                                              RequestHeader.PrincipalHeader principal, Long tenantId,
                                              Set<String> requested, Map<String, String> request) {
        String decision = StringUtils.lowerCase(StringUtils.trimToEmpty(request.get(McpConstant.Field.CONSENT)));
        if ("deny".equals(decision) || "denied".equals(decision)) {
            return oauthError(400, "access_denied", "user denied the authorization request");
        }
        if (!"approve".equals(decision) && !"approved".equals(decision)) {
            return oauthError(400, "consent_required", "user consent is required");
        }
        OAuthConsentRecord consent = new OAuthConsentRecord();
        consent.setId(UuidV7.nextLong());
        consent.setRegisteredClientId(client.getId());
        consent.setClientId(client.getClientId());
        consent.setPrincipalId(principal.getPrincipalId());
        consent.setTenantId(tenantId);
        consent.setScopes(String.join(" ", requested));
        consent.setConsentExt("{}");
        return transactionalOperator.transactional(store.upsertConsent(consent))
                .flatMap(rows -> rows > 0 ? Mono.empty() : oauthError(500, "server_error", "consent persistence failed"));
    }

    private OAuthAuthorizationRecord authorization(OAuthRegisteredClientRecord client, Long principal, Long tenant,
                                                    Long connection, String grant, String scopes) {
        OAuthAuthorizationRecord auth = new OAuthAuthorizationRecord(); auth.setRegisteredClientId(client.getId()); auth.setClientId(client.getClientId());
        auth.setPrincipalId(principal); auth.setTenantId(tenant); auth.setMcpConnectionId(connection); auth.setAuthorizationGrantType(grant);
        auth.setAuthorizedScopes(String.join(" ", supportedScopes(scopes))); return auth;
    }

    private Mono<OAuthRegisteredClientRecord> requireClient(String clientId) {
        if (StringUtils.isBlank(clientId)) return oauthError(400, "invalid_client", "client_id is required");
        return store.getClient(clientId).filter(c -> enabled(c.getEnableFlag())).switchIfEmpty(oauthError(401, "invalid_client", "client authentication failed"));
    }

    private Mono<Void> authenticateClient(OAuthRegisteredClientRecord client, Map<String, String> form, String header) {
        BasicCredentials credentials = basic(header);
        if (credentials != null && !Objects.equals(credentials.clientId(), client.getClientId())) {
            return oauthError(401, "invalid_client", "client authentication failed");
        }
        if (credentials == null && StringUtils.isNotBlank(form.get(McpConstant.Field.CLIENT_ID))
                && !Objects.equals(form.get(McpConstant.Field.CLIENT_ID), client.getClientId())) {
            return oauthError(401, "invalid_client", "client authentication failed");
        }
        if (OAuthClientTypeEnum.PUBLIC.getValue().equals(client.getClientType())) return Mono.empty();
        if (client.getClientSecretExpiresAt() != null && !client.getClientSecretExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            return oauthError(401, "invalid_client", "client authentication failed");
        }
        String presented = resolveClientSecret(form, header);
        if (StringUtils.isBlank(presented)) return oauthError(401, "invalid_client", "client authentication failed");
        return Mono.fromCallable(() -> PasswordUtil.verify(presented, client.getClientSecretHash()))
                .subscribeOn(ReactiveAuthScheduler.CRYPTO)
                .flatMap(valid -> valid ? Mono.empty() : oauthError(401, "invalid_client", "client authentication failed"));
    }

    private Mono<Void> requireServiceAccount(Long principal, Long tenant) {
        if (principal == null || principal <= 0) return oauthError(400, "invalid_client_metadata", "service_account_principal_id is required");
        return principalService.getById(tenant, principal).filter(p -> p.getPrincipalType() == PrincipalTypeEnum.SERVICE_ACCOUNT && p.getEnableFlag() == EnableFlagEnum.ENABLE)
                .switchIfEmpty(oauthError(400, "invalid_client_metadata", "service account is invalid")).then(membershipService.isTenantMember(tenant, principal))
                .flatMap(member -> member ? Mono.empty() : oauthError(400, "invalid_client_metadata", "service account is not a tenant member"));
    }

    private Mono<RequestHeader.PrincipalHeader> requirePrincipal(RequestHeader.PrincipalHeader principal) {
        return principal == null || principal.getPrincipalId() == null || principal.getTenantId() == null
                ? oauthError(401, "login_required", "authenticated principal is required") : Mono.just(principal);
    }

    private Mono<Void> validateConnection(McpConnectionRecord connection, String client, Long principal, Long tenant, String grant) {
        return usable(connection, client, principal, tenant) && Objects.equals(grant, connection.getGrantType()) ? Mono.empty()
                : oauthError(400, "invalid_request", "MCP connection is invalid");
    }

    private Mono<Void> validateConnectionOwner(McpConnectionRecord connection, RequestHeader.PrincipalHeader principal) {
        return Objects.equals(connection.getTenantId(), principal.getTenantId()) && Objects.equals(connection.getPrincipalId(), principal.getPrincipalId())
                ? Mono.empty() : oauthError(403, "access_denied", "connection is outside the principal scope");
    }

    private Mono<Void> validateConnectionPrincipal(Long principal, Long tenant, PrincipalTypeEnum type) {
        return membershipService.isTenantMember(tenant, principal).flatMap(member -> member ? principalService.getById(tenant, principal)
                .filter(p -> p.getEnableFlag() == EnableFlagEnum.ENABLE && (type == null || p.getPrincipalType() == type)).then()
                : oauthError(400, "invalid_request", "principal is not a tenant member"));
    }

    private Mono<Context> context(String token) {
        return Mono.defer(() -> {
            try {
                Claims claims = verify(token); Long tenant = number(claims.get(McpConstant.Field.TENANT_ID)); Long principal = number(claims.getSubject());
                Long connection = number(claims.get(McpConstant.Field.MCP_CONNECTION_ID)); String client = text(claims.get(McpConstant.Field.CLIENT_ID));
                Set<String> scopes = split(text(claims.get(McpConstant.Field.SCOPE)));
                return store.getAuthorizationByAccessTokenJti(claims.getId()).filter(this::activeAuthorization)
                        .switchIfEmpty(oauthError(401, "invalid_token", "token is inactive"))
                        .flatMap(auth -> store.getConnection(connection).filter(c -> usable(c, client, principal, tenant))
                                .switchIfEmpty(oauthError(401, "invalid_token", "connection is inactive")))
                        .then(membershipService.isTenantMember(tenant, principal)).flatMap(member -> member ? principalService.getById(tenant, principal)
                                .filter(p -> p.getEnableFlag() == EnableFlagEnum.ENABLE).map(p -> new Context(tenant, principal, connection, client,
                                        text(claims.get(McpConstant.Field.PRINCIPAL_TYPE)), p.getPrincipalName(), p.getDisplayName(), scopes))
                                : oauthError(401, "invalid_token", "principal is not a tenant member"));
            } catch (RuntimeException error) {
                return oauthError(401, "invalid_token", "token is invalid");
            }
        });
    }

    private Mono<McpToolAuthorizeResponseDTO> authorize(Context context, McpToolRecord tool, McpCallToolRequestDTO request) {
        if (!McpRiskLevelEnum.HIGH.getValue().equals(tool.getRiskLevel())) return Mono.just(decision("AUTHORIZED", null, "tool call authorized"));
        if (StringUtils.isBlank(request.getConfirmId())) return issueConfirmation(context, tool, request);
        return store.consumeConfirmation(request.getConfirmId(), context.tenantId(), context.principalId(), context.connectionId(), tool.getToolId(), request.getArgumentDigest(), LocalDateTime.now(ZoneOffset.UTC))
                .flatMap(rows -> rows == 1 ? Mono.just(decision("AUTHORIZED", request.getConfirmId(), "confirmation accepted"))
                        : oauthError(400, "invalid_request", "confirmation is invalid or already used"));
    }

    private Mono<McpToolAuthorizeResponseDTO> issueConfirmation(Context context, McpToolRecord tool, McpCallToolRequestDTO request) {
        if (StringUtils.isBlank(request.getIdempotencyKey())) return oauthError(400, "invalid_request", "idempotencyKey is required for high-risk calls");
        return store.getByIdempotency(context.connectionId(), request.getIdempotencyKey()).flatMap(existing ->
                Objects.equals(existing.getArgumentDigest(), request.getArgumentDigest()) && Objects.equals(existing.getToolId(), tool.getToolId())
                        && McpConfirmationStatusEnum.PENDING.getValue().equals(existing.getStatus())
                        ? Mono.just(decision("CONFIRM_REQUIRED", existing.getConfirmId(), "high risk tool requires confirmation"))
                        : oauthError(409, "idempotency_conflict", "idempotency key has already been used"))
                .switchIfEmpty(Mono.defer(() -> {
                    McpToolConfirmationRecord ticket = new McpToolConfirmationRecord(); ticket.setConfirmId(UUID.randomUUID().toString()); ticket.setTenantId(context.tenantId());
                    ticket.setPrincipalId(context.principalId()); ticket.setConnectionId(context.connectionId()); ticket.setToolId(tool.getToolId()); ticket.setArgumentDigest(request.getArgumentDigest());
                    ticket.setIdempotencyKey(request.getIdempotencyKey()); ticket.setRiskLevel(tool.getRiskLevel()); ticket.setStatus(McpConfirmationStatusEnum.PENDING.getValue());
                    ticket.setExpireTime(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
                    return transactionalOperator.transactional(store.insertConfirmation(ticket)).thenReturn(decision("CONFIRM_REQUIRED", ticket.getConfirmId(), "high risk tool requires confirmation"))
                            .onErrorResume(DataIntegrityViolationException.class, error -> store.getByIdempotency(context.connectionId(), request.getIdempotencyKey())
                                    .flatMap(existing -> Objects.equals(existing.getArgumentDigest(), request.getArgumentDigest())
                                            && Objects.equals(existing.getToolId(), tool.getToolId())
                                            && McpConfirmationStatusEnum.PENDING.getValue().equals(existing.getStatus())
                                            ? Mono.just(decision("CONFIRM_REQUIRED", existing.getConfirmId(), "high risk tool requires confirmation"))
                                            : oauthError(409, "idempotency_conflict", "idempotency key has already been used"))
                                    .switchIfEmpty(Mono.error(error)));
                }));
    }

    private void requireScope(Set<String> scopes, String... accepted) {
        for (String value : accepted) if (scopes.contains(value)) return;
        throw new OAuthProtocolException(403, "insufficient_scope", "required MCP scope is missing");
    }

    private McpToolDefinitionDTO toolDefinition(McpToolRecord tool) {
        return McpToolDefinitionDTO.builder().name(tool.getToolName()).title(tool.getToolTitle()).description(tool.getRemark())
                .inputSchema(input(tool.getToolExt())).annotations(McpToolDefinitionDTO.Annotations.builder().readOnlyHint(one(tool.getReadOnlyHint()))
                        .destructiveHint(one(tool.getDestructiveHint())).idempotentHint(one(tool.getIdempotentHint())).openWorldHint(one(tool.getOpenWorldHint())).build())
                .meta(McpToolDefinitionDTO.Metadata.builder().toolId(tool.getToolId()).permissionCode(tool.getPermissionCode()).riskLevel(tool.getRiskLevel()).build()).build();
    }

    private McpToolResolveResponseDTO resolve(McpToolRecord tool) {
        return McpToolResolveResponseDTO.builder().toolId(tool.getToolId()).toolName(tool.getToolName()).permissionCode(tool.getPermissionCode())
                .riskLevel(tool.getRiskLevel()).serviceName(tool.getServiceName()).apiPath(tool.getApiPath()).httpMethod(tool.getHttpMethod()).inputSchema(input(tool.getToolExt())).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> input(String raw) {
        if (StringUtils.isBlank(raw)) return Map.of();
        try { Map<String, Object> value = JsonUtil.parseObject(raw, Map.class); Object schema = value.get("inputSchema"); return schema instanceof Map<?, ?> ? (Map<String, Object>) schema : value; }
        catch (RuntimeException ignored) { return Map.of(); }
    }

    static void applyQuality(McpToolRecord target, ToolQuality quality) {
        target.setReadOnlyHint((byte) ("GET".equalsIgnoreCase(StringUtils.trimToEmpty(target.getHttpMethod())) ? 1 : 0));
        if (quality == null) {
            target.setRiskLevel(McpRiskLevelEnum.HIGH.getValue()); target.setDestructiveHint((byte) 1);
            target.setIdempotentHint((byte) 0); target.setOpenWorldHint((byte) 1); target.setEnableFlag((byte) 0); return;
        }
        String risk = StringUtils.upperCase(StringUtils.trimToEmpty(quality.getRiskLevel()));
        target.setRiskLevel(Set.of("LOW", "MEDIUM", "HIGH").contains(risk) ? risk : McpRiskLevelEnum.HIGH.getValue());
        target.setDestructiveHint((byte) (quality.getDestructive() == null || quality.getDestructive() ? 1 : 0));
        target.setIdempotentHint((byte) (Boolean.TRUE.equals(quality.getIdempotent()) ? 1 : 0));
        target.setOpenWorldHint((byte) (quality.getOpenWorld() == null || quality.getOpenWorld() ? 1 : 0));
        if (StringUtils.isNotBlank(quality.getSummary())) target.setToolTitle(quality.getSummary());
        if (StringUtils.isNotBlank(quality.getDescription())) target.setRemark(quality.getDescription());
        if (StringUtils.isNotBlank(quality.getInputSchema())) target.setToolExt(JsonUtil.toJsonString(Map.of("inputSchema", parseMap(quality.getInputSchema()))));
        target.setEnableFlag(Boolean.TRUE.equals(quality.getHidden()) ? (byte) 1 : (byte) 0);
    }

    static boolean toolChanged(McpToolRecord left, McpToolRecord right) {
        return !Objects.equals(left.getToolName(), right.getToolName()) || !Objects.equals(left.getToolTitle(), right.getToolTitle())
                || !Objects.equals(left.getToolCategory(), right.getToolCategory()) || !Objects.equals(left.getServiceName(), right.getServiceName())
                || !Objects.equals(left.getApiCode(), right.getApiCode()) || !Objects.equals(left.getPermissionCode(), right.getPermissionCode())
                || !Objects.equals(left.getHttpMethod(), right.getHttpMethod()) || !Objects.equals(left.getApiPath(), right.getApiPath())
                || !Objects.equals(left.getSchemaHash(), right.getSchemaHash()) || !Objects.equals(left.getRiskLevel(), right.getRiskLevel())
                || !Objects.equals(left.getReadOnlyHint(), right.getReadOnlyHint()) || !Objects.equals(left.getDestructiveHint(), right.getDestructiveHint())
                || !Objects.equals(left.getIdempotentHint(), right.getIdempotentHint()) || !Objects.equals(left.getOpenWorldHint(), right.getOpenWorldHint())
                || !Objects.equals(left.getEnableFlag(), right.getEnableFlag()) || !Objects.equals(left.getRemark(), right.getRemark())
                || !Objects.equals(left.getToolExt(), right.getToolExt());
    }
    private boolean activeAuthorization(OAuthAuthorizationRecord auth) { return auth != null && auth.getRevokedTime() == null && auth.getAccessTokenExpires() != null && auth.getAccessTokenExpires().isAfter(LocalDateTime.now(ZoneOffset.UTC)); }
    private boolean activeCode(OAuthAuthorizationRecord auth) { return auth != null && StringUtils.isNotBlank(auth.getAuthorizationCodeHash()) && auth.getAuthorizationCodeExpires() != null && auth.getAuthorizationCodeExpires().isAfter(LocalDateTime.now(ZoneOffset.UTC)) && auth.getRevokedTime() == null; }
    private boolean activeRefresh(OAuthAuthorizationRecord auth) { return auth.getRevokedTime() == null && auth.getRefreshTokenExpires() != null && auth.getRefreshTokenExpires().isAfter(LocalDateTime.now(ZoneOffset.UTC)); }
    private boolean usable(McpConnectionRecord c, String client, Long principal, Long tenant) { return c != null && Objects.equals(c.getClientId(), client) && Objects.equals(c.getPrincipalId(), principal) && Objects.equals(c.getTenantId(), tenant) && enabled(c.getEnableFlag()) && c.getRevokeTime() == null && (c.getExpireTime() == null || c.getExpireTime().isAfter(LocalDateTime.now(ZoneOffset.UTC))); }
    private boolean enabled(Byte value) { return value == null || value == 0; }
    private boolean one(Byte value) { return value != null && value == 1; }
    private boolean contains(String values, String expected) { return split(values).contains(expected); }
    private Set<String> requestedScopes(String raw, OAuthRegisteredClientRecord client) {
        Set<String> allowed = supportedScopes(client.getScopes());
        Set<String> requested = StringUtils.isBlank(raw) ? new LinkedHashSet<>(allowed) : split(raw);
        if (!McpConstant.Scope.SUPPORTED.containsAll(requested) || !allowed.containsAll(requested)) {
            throw new OAuthProtocolException(400, "invalid_scope", "requested scope is not registered for the client");
        }
        return requested;
    }
    private Set<String> registrationScopes(String raw) {
        Set<String> values = StringUtils.isBlank(raw) ? new LinkedHashSet<>() : split(raw);
        if (!McpConstant.Scope.SUPPORTED.containsAll(values)) {
            throw new OAuthProtocolException(400, "invalid_client_metadata", "unsupported scope");
        }
        return values;
    }
    private Set<String> supportedScopes(String raw) { Set<String> values = split(raw); values.retainAll(McpConstant.Scope.SUPPORTED); return values; }
    private Set<String> split(String raw) { if (StringUtils.isBlank(raw)) return new LinkedHashSet<>(); return new LinkedHashSet<>(List.of(raw.trim().split(McpConstant.Scope.DELIMITER_REGEX))); }
    private Set<String> split(Collection<String> values) { if (values == null) return new LinkedHashSet<>(); return values.stream().filter(StringUtils::isNotBlank).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)); }
    private String resolveClientId(Map<String, String> form, String header) { BasicCredentials credentials = basic(header); return credentials == null ? form.get(McpConstant.Field.CLIENT_ID) : credentials.clientId(); }
    private String resolveClientSecret(Map<String, String> form, String header) { BasicCredentials credentials = basic(header); return credentials == null ? form.get(McpConstant.Field.CLIENT_SECRET) : credentials.clientSecret(); }
    private BasicCredentials basic(String header) { if (StringUtils.isBlank(header) || !header.regionMatches(true, 0, "Basic ", 0, 6)) return null; try { String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8); int separator = decoded.indexOf(':'); return separator < 1 ? null : new BasicCredentials(decoded.substring(0, separator), decoded.substring(separator + 1)); } catch (IllegalArgumentException ignored) { return null; } }
    private Claims verify(String token) { if (StringUtils.isBlank(token)) throw new OAuthProtocolException(401, "invalid_token", "token is required"); return new OAuthJwtVerifier(properties.getIssuer(), properties.getAudience(), keyProvider).verify(token); }
    private String randomToken() { return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""); }
    private String sha256(String value) { try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8))); } catch (Exception error) { throw new IllegalStateException(error); } }
    private String sha256Base64(String value) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII))); } catch (Exception error) { throw new IllegalStateException(error); } }
    private Long positiveLong(String raw, Long fallback) { try { Long value = StringUtils.isBlank(raw) ? fallback : Long.valueOf(raw); return value == null || value <= 0 ? fallback : value; } catch (NumberFormatException ignored) { return fallback; } }
    private Long number(Object value) { try { return value == null ? null : value instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; } }
    private Long nextId() { return UuidV7.nextLong(); }
    private String text(Object value) { return value == null ? null : String.valueOf(value); }
    @SuppressWarnings("unchecked") private static Map<String, Object> parseMap(String json) { if (StringUtils.isBlank(json)) return new LinkedHashMap<>(); try { return JsonUtil.parseObject(json, Map.class); } catch (RuntimeException ignored) { return new LinkedHashMap<>(); } }
    private Map<String, Object> tokenResponse(String access, String refresh, Set<String> scopes, LocalDateTime expires, LocalDateTime issued) { Map<String, Object> value = new LinkedHashMap<>(); value.put(McpConstant.Field.ACCESS_TOKEN, access); value.put(McpConstant.Field.TOKEN_TYPE, McpConstant.OAuth.TOKEN_TYPE_BEARER); value.put(McpConstant.Field.EXPIRES_IN, Math.max(0, expires.toEpochSecond(ZoneOffset.UTC) - issued.toEpochSecond(ZoneOffset.UTC))); value.put("scope", String.join(" ", scopes)); if (refresh != null) value.put(McpConstant.Field.REFRESH_TOKEN, refresh); return value; }
    private Map<String, Object> orderedMap(Object... values) { Map<String, Object> result = new LinkedHashMap<>(); for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]); return result; }
    private <T> Mono<T> oauthError(int status, String error, String description) { return Mono.error(new OAuthProtocolException(status, error, description)); }
    private record Context(Long tenantId, Long principalId, Long connectionId, String clientId, String principalType, String principalName, String displayName, Set<String> scopes) { boolean allowHighRisk() { return scopes.contains(McpConstant.Scope.TOOLS_CALL_HIGH); } McpPrincipalContextDTO principalContext() { return McpPrincipalContextDTO.builder().tenantId(tenantId).principalId(principalId).connectionId(connectionId).clientId(clientId).principalType(principalType).principalName(principalName).displayName(displayName).build(); } }
    private record BasicCredentials(String clientId, String clientSecret) { }
    private McpToolAuthorizeResponseDTO decision(String decision, String id, String message) { return McpToolAuthorizeResponseDTO.builder().decision(decision).confirmId(id).message(message).build(); }
}
