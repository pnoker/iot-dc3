/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.biz.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.config.OAuthProperties;
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.dal.ServiceAccountManager;
import io.github.pnoker.common.auth.entity.bo.McpConnectionAddBO;
import io.github.pnoker.common.auth.entity.bo.OAuthClientRegistrationBO;
import io.github.pnoker.common.auth.entity.builder.McpAuditBuilder;
import io.github.pnoker.common.auth.entity.builder.McpConnectionBuilder;
import io.github.pnoker.common.auth.entity.builder.McpToolBuilder;
import io.github.pnoker.common.auth.entity.builder.OAuthClientBuilder;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationResponseVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.auth.mapper.OAuthMcpMapper;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.auth.tool.McpOpenApiAggregator;
import io.github.pnoker.common.auth.tool.ToolQuality;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.McpAuditStatusEnum;
import io.github.pnoker.common.enums.McpConfirmationStatusEnum;
import io.github.pnoker.common.enums.McpRiskLevelEnum;
import io.github.pnoker.common.enums.OAuthClientTypeEnum;
import io.github.pnoker.common.enums.OAuthGrantTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Standards-oriented OAuth and MCP runtime implementation for the reactive auth
 * center. Spring Authorization Server is servlet-only in the currently used stack,
 * so this service keeps the endpoints WebFlux-native while persisting the explicit
 * OAuth records defined by the design.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthMcpRuntimeServiceImpl implements OAuthMcpRuntimeService {

    private static final String KID = "dc3-oauth-rsa";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuthMcpMapper oauthMcpMapper;
    private final TenantMembershipService tenantMembershipService;
    private final PrincipalManager principalManager;
    private final ServiceAccountManager serviceAccountManager;
    private final McpOpenApiAggregator openApiAggregator;
    private final OAuthProperties oauthProperties;
    private final OAuthClientBuilder oauthClientBuilder;
    private final McpConnectionBuilder mcpConnectionBuilder;
    private final McpToolBuilder mcpToolBuilder;
    private final McpAuditBuilder mcpAuditBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dc3.mcp.confirm-ttl:PT5M}")
    private Duration confirmTtl;

    private volatile KeyMaterial keyMaterial;

    /**
     * Fill one catalog record's quality fields from the OpenAPI-sourced {@link ToolQuality}, applying
     * conservative defaults when a field (or the whole quality) is undeclared. {@code readOnly} is
     * always derived from the HTTP method; {@code hidden} disables the row via enable_flag.
     */
    static void applyQuality(McpToolRecord record, ToolQuality quality) {
        String method = StringUtils.upperCase(StringUtils.trimToEmpty(record.getHttpMethod()));
        record.setReadOnlyHint(toByte("GET".equals(method)));

        if (quality == null) {
            record.setRiskLevel("HIGH");
            record.setDestructiveHint(toByte(true));
            record.setIdempotentHint(toByte(false));
            record.setOpenWorldHint(toByte(true));
            record.setEnableFlag((byte) 0);
            return;
        }
        record.setRiskLevel(normalizeRisk(quality.getRiskLevel()));
        record.setDestructiveHint(toByte(defaultBool(quality.getDestructive(), true)));
        record.setIdempotentHint(toByte(defaultBool(quality.getIdempotent(), false)));
        record.setOpenWorldHint(toByte(defaultBool(quality.getOpenWorld(), true)));
        if (StringUtils.isNotBlank(quality.getSummary())) {
            record.setToolTitle(quality.getSummary());
        }
        if (StringUtils.isNotBlank(quality.getDescription())) {
            record.setRemark(quality.getDescription());
        }
        if (StringUtils.isNotBlank(quality.getInputSchema())) {
            record.setToolExt("{\"inputSchema\":" + quality.getInputSchema() + "}");
        }
        record.setEnableFlag(Boolean.TRUE.equals(quality.getHidden()) ? (byte) 1 : (byte) 0);
    }

    /**
     * True when any column a quality-only JSON edit can change differs between the existing row and
     * the freshly-built candidate — the quality fields (title/remark/hints) that such an edit can flip
     * without altering the schema hash, plus schemaHash/permissionCode/apiPath/riskLevel/enableFlag/toolExt.
     * Identity columns (toolName/toolCategory/serviceName/apiCode/httpMethod) are not compared directly:
     * existing and candidate are matched by tool_id (= api_code), and any change to their feeders flips
     * schemaHash ({@code md5(api_code || ':' || api_name)}) or the tool_id match key itself. Keeps the
     * catalog in sync with x-dc3-ai/description edits.
     */
    static boolean toolChanged(McpToolRecord existing, McpToolRecord candidate) {
        return !Objects.equals(existing.getSchemaHash(), candidate.getSchemaHash())
                || !Objects.equals(existing.getPermissionCode(), candidate.getPermissionCode())
                || !Objects.equals(existing.getApiPath(), candidate.getApiPath())
                || !Objects.equals(existing.getRiskLevel(), candidate.getRiskLevel())
                || !Objects.equals(existing.getEnableFlag(), candidate.getEnableFlag())
                || !Objects.equals(existing.getToolExt(), candidate.getToolExt())
                || !Objects.equals(existing.getToolTitle(), candidate.getToolTitle())
                || !Objects.equals(existing.getRemark(), candidate.getRemark())
                || !Objects.equals(existing.getReadOnlyHint(), candidate.getReadOnlyHint())
                || !Objects.equals(existing.getDestructiveHint(), candidate.getDestructiveHint())
                || !Objects.equals(existing.getIdempotentHint(), candidate.getIdempotentHint())
                || !Objects.equals(existing.getOpenWorldHint(), candidate.getOpenWorldHint());
    }

    private static String normalizeRisk(String declared) {
        String value = StringUtils.upperCase(StringUtils.trimToEmpty(declared));
        return switch (value) {
            case "HIGH", "MEDIUM", "LOW" -> value;
            default -> "HIGH"; // conservative default for blank/illegal
        };
    }

    private static boolean defaultBool(Boolean declared, boolean fallback) {
        return declared != null ? declared : fallback;
    }

    private static byte toByte(boolean flag) {
        return flag ? (byte) 1 : (byte) 0;
    }

    @Override
    public Map<String, Object> authorizationServerMetadata() {
        // PublicEndpoint (OAuthController.authorizationServerMetadata): discovery is unauthenticated.
        return TenantContextHolder.runIgnore(() -> {
            String issuer = oauthProperties.getIssuer();
            return orderedMap(
                    "issuer", issuer,
                    "authorization_endpoint", issuer + McpConstant.OAUTH2_AUTHORIZE,
                    "token_endpoint", issuer + McpConstant.OAUTH2_TOKEN,
                    "jwks_uri", issuer + McpConstant.OAUTH2_JWKS,
                    "revocation_endpoint", issuer + McpConstant.OAUTH2_REVOKE,
                    "registration_endpoint", issuer + McpConstant.OAUTH2_REGISTER,
                    "response_types_supported", List.of(McpConstant.OAuth.RESPONSE_TYPE_CODE),
                    "grant_types_supported", List.of(OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue(),
                            OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue(),
                            OAuthGrantTypeEnum.REFRESH_TOKEN.getValue()),
                    "code_challenge_methods_supported", List.of(McpConstant.OAuth.CODE_CHALLENGE_METHOD_S256),
                    "scopes_supported", McpConstant.Scope.SUPPORTED,
                    "token_endpoint_auth_methods_supported", List.of(McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_BASIC,
                            McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_POST, McpConstant.OAuth.AUTH_METHOD_NONE)
            );
        });
    }

    @Override
    public Map<String, Object> jwks() {
        // PublicEndpoint (OAuthController.jwks): JWKS is unauthenticated.
        return TenantContextHolder.runIgnore(() -> {
            RSAPublicKey publicKey = (RSAPublicKey) keyMaterial().publicKey();
            return Map.of("keys", List.of(orderedMap(
                    "kty", "RSA",
                    "use", "sig",
                    "kid", KID,
                    "alg", "RS256",
                    "n", DecodeUtil.base64UrlWithoutLeadingZero(publicKey.getModulus().toByteArray()),
                    "e", DecodeUtil.base64UrlWithoutLeadingZero(publicKey.getPublicExponent().toByteArray())
            )));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuthClientRegistrationResponseVO registerClient(OAuthClientRegistrationBO request,
                                                            RequestHeader.PrincipalHeader principalHeader) {
        // PublicEndpoint (OAuthController.register): dynamic client registration runs before tenant
        // context is established; the caller's tenant is taken from the principal header and bound
        // explicitly on the record, so the interceptor's implicit scoping must be bypassed here.
        return TenantContextHolder.runIgnore(() -> {
            OAuthClientRegistrationBO req = Objects.requireNonNullElseGet(request, OAuthClientRegistrationBO::new);
            String clientName = stringValue(req.getClientName());
            if (StringUtils.isBlank(clientName)) {
                throw oauthError(BAD_REQUEST.value(), "invalid_client_metadata", "client_name is required");
            }
            // The BO already carries a validated OAuthClientTypeEnum (null when unspecified), so the wire
            // value can no longer be an unknown string; default to PUBLIC when absent.
            String clientType = Objects.requireNonNullElse(req.getClientType(), OAuthClientTypeEnum.PUBLIC).getValue();

            List<String> grantValues = Objects.isNull(req.getGrantTypes()) ? null
                    : req.getGrantTypes().stream().filter(Objects::nonNull)
                    .map(OAuthGrantTypeEnum::getValue).toList();
            Set<String> grants = normalizeSet(grantValues);
            if (grants.isEmpty()) {
                grants = OAuthClientTypeEnum.PUBLIC.getValue().equals(clientType)
                        ? Set.of(OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue())
                        : Set.of(OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue());
            }
            Set<String> scopes = normalizeSet(req.getScope());
            if (scopes.isEmpty()) {
                scopes = Set.of(McpConstant.Scope.TOOLS_LIST, McpConstant.Scope.TOOLS_CALL);
            }
            Set<String> redirects = normalizeSet(req.getRedirectUris());
            if (grants.contains(OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue()) && redirects.isEmpty()) {
                throw oauthError(BAD_REQUEST.value(), "invalid_redirect_uri", "redirect_uris is required");
            }

            // The registered client is bound to the authenticated caller's tenant. The request body's
            // tenant_id is intentionally ignored, so a caller cannot register an OAuth client for a tenant
            // they do not belong to (which would otherwise let them bind another tenant's service account
            // and mint cross-tenant tokens). validateServiceAccountClient below is thereby scoped to the
            // caller's own tenant as well.
            requireAuthenticatedPrincipal(principalHeader);
            Long ownerPrincipalId = principalHeader.getPrincipalId();
            Long tenantId = principalHeader.getTenantId();
            Long serviceAccountPrincipalId = req.getServiceAccountPrincipalId();
            if (grants.contains(OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue())) {
                validateServiceAccountClient(serviceAccountPrincipalId, tenantId);
            }

            String clientId = McpConstant.OAuth.CLIENT_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
            String clientSecret = null;
            String secretHash = "";
            String authMethods = McpConstant.OAuth.AUTH_METHOD_NONE;
            if (OAuthClientTypeEnum.CONFIDENTIAL.getValue().equals(clientType)) {
                clientSecret = randomToken();
                secretHash = PasswordUtil.encode(clientSecret);
                authMethods = McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_BASIC + ' '
                        + McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_POST;
            }

            OAuthRegisteredClientRecord client = new OAuthRegisteredClientRecord();
            client.setId(IdWorker.getId());
            client.setClientId(clientId);
            client.setClientName(clientName);
            client.setClientType(clientType);
            client.setOwnerPrincipalId(ownerPrincipalId);
            client.setServiceAccountPrincipalId(Objects.requireNonNullElse(serviceAccountPrincipalId, 0L));
            client.setTenantId(Objects.requireNonNullElse(tenantId, 0L));
            client.setClientSecretHash(secretHash);
            client.setClientSecretExpiresAt(null);
            client.setClientAuthMethods(authMethods);
            client.setAuthorizationGrantTypes(String.join(" ", grants));
            client.setRedirectUris(String.join(" ", redirects));
            client.setScopes(String.join(" ", scopes));
            client.setRequirePkce((byte) (grants.contains(OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue()) ? 1 : 0));
            client.setRequireConsent((byte) 1);
            client.setEnableFlag((byte) 0);
            oauthMcpMapper.insertClient(client);

            return OAuthClientRegistrationResponseVO.builder()
                    .clientId(clientId)
                    .clientName(clientName)
                    .clientType(clientType)
                    .grantTypes(grants)
                    .redirectUris(redirects)
                    .scope(String.join(" ", scopes))
                    .tokenEndpointAuthMethod(OAuthClientTypeEnum.CONFIDENTIAL.getValue().equals(clientType)
                            ? McpConstant.OAuth.AUTH_METHOD_CLIENT_SECRET_BASIC : McpConstant.OAuth.AUTH_METHOD_NONE)
                    .clientSecret(clientSecret)
                    .build();
        });
    }

    @Override
    public List<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        return oauthClientBuilder.buildVOListByRecordList(
                oauthMcpMapper.listClientsByOwner(principalHeader.getPrincipalId(), principalHeader.getTenantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public URI authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader) {
        // PublicEndpoint (OAuthController.authorize): authorization endpoint runs before tenant
        // context; tenant membership is explicitly validated via tenantMembershipService below.
        return TenantContextHolder.runIgnore(() -> {
            if (principalHeader == null || principalHeader.getPrincipalId() == null) {
                throw oauthError(UNAUTHORIZED.value(), "login_required", "authenticated principal is required");
            }
            if (!McpConstant.OAuth.RESPONSE_TYPE_CODE.equals(params.get("response_type"))) {
                throw oauthError(BAD_REQUEST.value(), "unsupported_response_type", "only code is supported");
            }
            OAuthRegisteredClientRecord client = requireClient(params.get(McpConstant.Field.CLIENT_ID));
            requireGrant(client, OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue());
            String redirectUri = params.get(McpConstant.Field.REDIRECT_URI);
            if (!splitValues(client.getRedirectUris()).contains(redirectUri)) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request", "redirect_uri mismatch");
            }
            if (one(client.getRequirePkce())) {
                if (!McpConstant.OAuth.CODE_CHALLENGE_METHOD_S256.equals(
                        params.get(McpConstant.Field.CODE_CHALLENGE_METHOD))
                        || StringUtils.isBlank(params.get(McpConstant.Field.CODE_CHALLENGE))) {
                    throw oauthError(BAD_REQUEST.value(), "invalid_request", "PKCE S256 is required");
                }
            }
            Set<String> scopes = requestedScopes(params.get(McpConstant.Field.SCOPE), client);
            Long tenantId = longValue(params.get(McpConstant.Field.TENANT_ID));
            if (tenantId == null || tenantId == 0) {
                tenantId = principalHeader.getTenantId();
            }
            if (tenantId == null || !tenantMembershipService.isTenantMember(tenantId, principalHeader.getPrincipalId())) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request", "principal is not a member of the tenant");
            }

            Long connectionId = longValue(params.get(McpConstant.Field.MCP_CONNECTION_ID));
            McpConnectionRecord connection = connectionId == null || connectionId == 0
                    ? oauthMcpMapper.selectActiveConnection(client.getClientId(), principalHeader.getPrincipalId(),
                    tenantId, OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue())
                    : oauthMcpMapper.selectConnectionById(connectionId);
            validateConnection(connection, client.getClientId(), principalHeader.getPrincipalId(), tenantId,
                    OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue());

            String code = randomToken();
            OAuthAuthorizationRecord authorization = new OAuthAuthorizationRecord();
            authorization.setId(IdWorker.getId());
            authorization.setRegisteredClientId(client.getId());
            authorization.setClientId(client.getClientId());
            authorization.setPrincipalId(principalHeader.getPrincipalId());
            authorization.setPrincipalType(StringUtils.defaultIfBlank(principalHeader.getPrincipalType(),
                    PrincipalTypeEnum.USER.getValue()));
            authorization.setTenantId(tenantId);
            authorization.setMcpConnectionId(connection.getId());
            authorization.setAuthorizationGrantType(OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue());
            authorization.setAuthorizedScopes(String.join(" ", scopes));
            authorization.setStateHash(sha256(params.get("state")));
            authorization.setAuthorizationCodeHash(sha256(code));
            authorization.setAuthorizationCodeIssued(LocalDateTime.now());
            authorization.setAuthorizationCodeExpires(LocalDateTime.now().plus(oauthProperties.getAuthorizationCodeTtl()));
            authorization.setTokenMetadata(JsonUtil.toJsonString(orderedMap(
                    McpConstant.Field.REDIRECT_URI, redirectUri,
                    McpConstant.Field.CODE_CHALLENGE, params.get(McpConstant.Field.CODE_CHALLENGE),
                    McpConstant.Field.CODE_CHALLENGE_METHOD, params.get(McpConstant.Field.CODE_CHALLENGE_METHOD)
            )));
            oauthMcpMapper.insertAuthorization(authorization);

            StringBuilder target = new StringBuilder(redirectUri)
                    .append(redirectUri.contains("?") ? '&' : '?')
                    .append("code=").append(urlEncode(code));
            if (StringUtils.isNotBlank(params.get("state"))) {
                target.append("&state=").append(urlEncode(params.get("state")));
            }
            return URI.create(target.toString());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> token(Map<String, String> form, String authorizationHeader) {
        // PublicEndpoint (OAuthController.token): token issuance is cross-tenant authentication;
        // the issued token's tenant_id claim is the authorization basis, validated explicitly via
        // tenantMembershipService inside issueAndPersistTokens.
        return TenantContextHolder.runIgnore(() -> {
            String grantType = form.get(McpConstant.Field.GRANT_TYPE);
            if (OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue().equals(grantType)) {
                return authorizationCodeToken(form, authorizationHeader);
            }
            if (OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue().equals(grantType)) {
                return clientCredentialsToken(form, authorizationHeader);
            }
            if (OAuthGrantTypeEnum.REFRESH_TOKEN.getValue().equals(grantType)) {
                return refreshToken(form, authorizationHeader);
            }
            throw oauthError(BAD_REQUEST.value(), "unsupported_grant_type", "unsupported grant_type");
        });
    }

    @Override
    public McpIntrospectResponseDTO introspect(String token) {
        // PublicEndpoint (McpGatewayController.mcp): token introspection is invoked by the gateway
        // on every MCP request before tenant context exists; the token's tenant_id claim drives
        // authorization and is re-checked via tenantMembershipService below.
        return TenantContextHolder.runIgnore(() -> {
            try {
                Claims claims = parseAccessToken(token);
                String jti = claims.getId();
                OAuthAuthorizationRecord authorization = oauthMcpMapper.selectAuthorizationByAccessTokenJti(jti);
                if (!isActiveAuthorization(authorization, LocalDateTime.now())) {
                    return McpIntrospectResponseDTO.inactive();
                }
                Long tenantId = numberClaim(claims, McpConstant.Field.TENANT_ID);
                Long principalId = Long.valueOf(claims.getSubject());
                Long connectionId = numberClaim(claims, McpConstant.Field.MCP_CONNECTION_ID);
                String clientId = stringValue(claims.get(McpConstant.Field.CLIENT_ID));
                McpConnectionRecord connection = oauthMcpMapper.selectConnectionById(connectionId);
                if (!isUsableConnection(connection, clientId, principalId, tenantId)) {
                    return McpIntrospectResponseDTO.inactive();
                }
                PrincipalDO principal = principalManager.getById(principalId);
                if (principal == null || !enabled(principal.getEnableFlag())
                        || !tenantMembershipService.isTenantMember(tenantId, principalId)) {
                    return McpIntrospectResponseDTO.inactive();
                }
                return McpIntrospectResponseDTO.builder()
                        .active(true)
                        .iss(claims.getIssuer())
                        .aud(claims.getAudience())
                        .sub(claims.getSubject())
                        .jti(jti)
                        .exp(claims.getExpiration().toInstant().getEpochSecond())
                        .iat(claims.getIssuedAt().toInstant().getEpochSecond())
                        .tenantId(tenantId)
                        .principalId(principalId)
                        .principalType(stringValue(claims.get(McpConstant.Field.PRINCIPAL_TYPE)))
                        .principalName(principal.getPrincipalName())
                        .displayName(principal.getDisplayName())
                        .clientId(clientId)
                        .mcpConnectionId(connectionId)
                        .grantType(stringValue(claims.get(McpConstant.Field.GRANT_TYPE)))
                        .scope(stringValue(claims.get(McpConstant.Field.SCOPE)))
                        .build();
            } catch (RuntimeException ignored) {
                return McpIntrospectResponseDTO.inactive();
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> revoke(Map<String, String> form, String authorizationHeader) {
        // PublicEndpoint (OAuthController.revoke): revocation may be called by a client before any
        // tenant context; it resolves the authorization by the presented token only.
        return TenantContextHolder.runIgnore(() -> {
            String token = form.get("token");
            if (StringUtils.isBlank(token)) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request", "token is required");
            }
            LocalDateTime now = LocalDateTime.now();
            try {
                Claims claims = parseAccessToken(token);
                oauthMcpMapper.revokeAuthorizationByAccessTokenJti(claims.getId(), "revoke", now);
            } catch (RuntimeException ignored) {
                oauthMcpMapper.revokeAuthorizationByRefreshTokenHash(sha256(token), "revoke", now);
            }
            return Map.of("revoked", true);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshToolCatalog() {
        int changed = 0;
        // OpenAPI JSON is the single source of tool quality (description, x-dc3-ai flags, inputSchema).
        // dc3_api/dc3_resource still drive the tool set + permission code; we join by api_code.
        Map<String, ToolQuality> quality = openApiAggregator.toolQualityByApiCode();
        for (McpToolRecord candidate : oauthMcpMapper.listRegistryToolCandidates()) {
            applyQuality(candidate, quality.get(candidate.getApiCode()));
            McpToolRecord existing = oauthMcpMapper.selectToolByToolId(candidate.getToolId());
            if (existing == null) {
                candidate.setId(IdWorker.getId());
                changed += oauthMcpMapper.insertTool(candidate);
            } else if (toolChanged(existing, candidate)) {
                candidate.setId(existing.getId());
                changed += oauthMcpMapper.updateTool(candidate);
            }
        }
        return changed;
    }

    @Override
    public List<McpToolVO> listToolCatalog(String keyword, String riskLevel, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 200 : limit, 500));
        return mcpToolBuilder.buildVOListByRecordList(
                oauthMcpMapper.listToolCatalog(StringUtils.trimToEmpty(keyword), StringUtils.trimToEmpty(riskLevel),
                        boundedLimit));
    }

    @Override
    public List<McpAuditVO> listAudit(Long tenantId, Long principalId, String toolId, String status,
                                      String riskLevel, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 200 : limit, 500));
        return mcpAuditBuilder.buildVOListByRecordList(
                oauthMcpMapper.listAudit(tenantId, principalId, StringUtils.trimToEmpty(toolId),
                        StringUtils.trimToEmpty(status), StringUtils.trimToEmpty(riskLevel), boundedLimit));
    }

    @Override
    public List<McpConnectionVO> listConnections(RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        return mcpConnectionBuilder.buildVOListByRecordList(
                oauthMcpMapper.listConnectionsByPrincipal(principalHeader.getTenantId(),
                        principalHeader.getPrincipalId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpConnectionVO createConnection(McpConnectionAddBO entityBO,
                                            RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        // Build the projection from the BO; the *Record stays String-typed (OAuthMcpMapper resultType),
        // so domain enums are flattened to their wire value here.
        McpConnectionRecord connection = new McpConnectionRecord();
        connection.setConnectionName(entityBO.getConnectionName());
        connection.setClientId(entityBO.getClientId());
        connection.setPrincipalId(entityBO.getPrincipalId());
        connection.setPrincipalType(Objects.isNull(entityBO.getPrincipalType()) ? null
                : entityBO.getPrincipalType().getValue());
        connection.setTenantId(entityBO.getTenantId());
        connection.setGrantType(Objects.isNull(entityBO.getGrantType()) ? null
                : entityBO.getGrantType().getValue());
        connection.setExpireTime(entityBO.getExpireTime());
        OAuthRegisteredClientRecord client = requireClient(connection.getClientId());
        String grantType = StringUtils.defaultIfBlank(connection.getGrantType(),
                OAuthGrantTypeEnum.AUTHORIZATION_CODE.getValue());
        requireGrant(client, grantType);

        Long tenantId = Objects.requireNonNullElse(connection.getTenantId(), principalHeader.getTenantId());
        Long principalId = Objects.requireNonNullElse(connection.getPrincipalId(), principalHeader.getPrincipalId());
        String principalType = StringUtils.defaultIfBlank(connection.getPrincipalType(),
                StringUtils.defaultIfBlank(principalHeader.getPrincipalType(), PrincipalTypeEnum.USER.getValue()));

        if (OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue().equals(grantType)) {
            if (!PrincipalTypeEnum.SERVICE_ACCOUNT.getValue().equals(principalType)) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request",
                        "client_credentials connection requires a service account principal");
            }
            if (!Objects.equals(client.getServiceAccountPrincipalId(), principalId)
                    || !Objects.equals(client.getTenantId(), tenantId)) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request",
                        "OAuth client is not bound to this service account");
            }
            validateServiceAccountClient(principalId, tenantId);
        } else {
            if (!Objects.equals(principalId, principalHeader.getPrincipalId())) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request",
                        "authorization_code connection must use the current principal");
            }
            if (!tenantMembershipService.isTenantMember(tenantId, principalId)) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request", "principal is not a member of the tenant");
            }
        }

        connection.setId(IdWorker.getId());
        connection.setConnectionName(StringUtils.defaultIfBlank(connection.getConnectionName(),
                client.getClientName()));
        connection.setClientId(client.getClientId());
        connection.setPrincipalId(principalId);
        connection.setPrincipalType(principalType);
        connection.setTenantId(tenantId);
        connection.setGrantType(grantType);
        connection.setEnableFlag((byte) 0);
        connection.setRemark(StringUtils.defaultString(connection.getRemark()));
        connection.setCreatorId(principalHeader.getPrincipalId());
        connection.setCreatorName(principalName(principalHeader));
        oauthMcpMapper.insertConnection(connection);
        return mcpConnectionBuilder.buildVOByRecord(connection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeConnection(Long connectionId, RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        int changed = oauthMcpMapper.revokeConnection(connectionId, principalHeader.getTenantId(),
                principalHeader.getPrincipalId(), LocalDateTime.now());
        if (changed <= 0) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "MCP connection does not exist");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceConnectionTools(Long connectionId, List<String> toolIds,
                                       RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        McpConnectionRecord connection = oauthMcpMapper.selectConnectionById(connectionId);
        if (connection == null || !Objects.equals(connection.getTenantId(), principalHeader.getTenantId())
                || !Objects.equals(connection.getCreatorId(), principalHeader.getPrincipalId())) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "MCP connection does not exist");
        }
        oauthMcpMapper.deleteConnectionTools(connectionId);
        Set<String> uniqueToolIds = new HashSet<>(Objects.requireNonNullElse(toolIds, List.of()));
        for (String toolId : uniqueToolIds) {
            if (StringUtils.isBlank(toolId)) {
                continue;
            }
            McpToolRecord tool = oauthMcpMapper.selectToolByToolId(toolId);
            if (tool == null || !enabled(tool.getEnableFlag())) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request", "MCP tool does not exist: " + toolId);
            }
            oauthMcpMapper.insertConnectionTool(IdWorker.getId(), connectionId, toolId,
                    principalHeader.getPrincipalId(), principalName(principalHeader));
        }
    }

    @Override
    public List<String> listConnectionToolIds(Long connectionId, RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        return oauthMcpMapper.listConnectionToolIds(connectionId, principalHeader.getTenantId(),
                principalHeader.getPrincipalId());
    }

    @Override
    public List<McpToolDefinitionDTO> listVisibleTools(Long tenantId, Long principalId, Long connectionId,
                                                       Set<String> scopes) {
        if (!scopes.contains(McpConstant.Scope.TOOLS_LIST) && !scopes.contains(McpConstant.Scope.TOOLS_CALL)) {
            throw oauthError(UNAUTHORIZED.value(), "insufficient_scope",
                    McpConstant.Scope.TOOLS_LIST + " scope is required");
        }
        boolean allowHighRisk = scopes.contains(McpConstant.Scope.TOOLS_CALL_HIGH);
        return oauthMcpMapper.listVisibleTools(tenantId, principalId, connectionId, allowHighRisk)
                .stream()
                .map(this::toolToMcp)
                .toList();
    }

    @Override
    public McpToolResolveResponseDTO resolveVisibleTool(Long tenantId, Long principalId, Long connectionId,
                                                        String toolName, Set<String> scopes) {
        if (!scopes.contains(McpConstant.Scope.TOOLS_CALL)) {
            throw oauthError(UNAUTHORIZED.value(), "insufficient_scope",
                    McpConstant.Scope.TOOLS_CALL + " scope is required");
        }
        boolean allowHighRisk = scopes.contains(McpConstant.Scope.TOOLS_CALL_HIGH);
        McpToolRecord tool = oauthMcpMapper.selectVisibleToolByName(tenantId, principalId, connectionId, toolName,
                allowHighRisk);
        if (tool == null) {
            throw oauthError(UNAUTHORIZED.value(), "access_denied", "tool is not visible for this connection");
        }
        oauthMcpMapper.updateConnectionLastUsed(connectionId, LocalDateTime.now());
        return resolvedTool(tool);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpToolAuthorizeResponseDTO authorizeToolCall(McpToolAuthorizeRequestDTO request) {
        request = Objects.requireNonNullElseGet(request, McpToolAuthorizeRequestDTO::new);
        Set<String> scopes = splitValues(request.getScope());
        // Re-run the full visibility/whitelist/scope check; this is the authoritative gate and
        // also yields the tool's risk level and stable tool_id.
        McpToolResolveResponseDTO tool = resolveVisibleTool(request.getTenantId(), request.getPrincipalId(),
                request.getMcpConnectionId(), request.getToolName(), scopes);

        // Only HIGH-risk tools require platform confirmation; the rest pass straight through.
        if (!McpRiskLevelEnum.HIGH.getValue().equals(tool.getRiskLevel())) {
            return authorizeDecision(McpConstant.Confirmation.DECISION_AUTHORIZED, "", "authorized",
                    tool.getRiskLevel());
        }

        String confirmId = StringUtils.trimToEmpty(request.getConfirmId());
        String idempotencyKey = StringUtils.trimToEmpty(request.getIdempotencyKey());
        String argumentDigest = StringUtils.trimToEmpty(request.getArgumentDigest());

        if (StringUtils.isBlank(confirmId)) {
            // Reject an idempotency key already consumed by an earlier high-risk call.
            if (StringUtils.isNotBlank(idempotencyKey)
                    && oauthMcpMapper.selectConsumedByIdempotencyKey(request.getMcpConnectionId(),
                    idempotencyKey) != null) {
                return authorizeDecision(McpConstant.Confirmation.DECISION_REJECTED, "",
                        "idempotency key has already been used", tool.getRiskLevel());
            }
            // Issue a pending confirmation ticket bound to the caller, connection, tool and arguments.
            String issuedConfirmId = UUID.randomUUID().toString();
            McpToolConfirmationRecord ticket = new McpToolConfirmationRecord();
            ticket.setId(IdWorker.getId());
            ticket.setConfirmId(issuedConfirmId);
            ticket.setTenantId(request.getTenantId());
            ticket.setPrincipalId(request.getPrincipalId());
            ticket.setConnectionId(request.getMcpConnectionId());
            ticket.setToolId(tool.getToolId());
            ticket.setArgumentDigest(argumentDigest);
            ticket.setIdempotencyKey(idempotencyKey);
            ticket.setRiskLevel(tool.getRiskLevel());
            ticket.setStatus(McpConfirmationStatusEnum.PENDING.getValue());
            ticket.setExpireTime(LocalDateTime.now().plus(confirmTtl));
            oauthMcpMapper.insertConfirmation(ticket);
            return authorizeDecision(McpConstant.Confirmation.DECISION_CONFIRM_REQUIRED, issuedConfirmId,
                    "High risk tool '" + tool.getToolName() + "' requires confirmation; resend the call with this "
                            + "confirmId", tool.getRiskLevel());
        }

        McpToolConfirmationRecord ticket = oauthMcpMapper.selectConfirmationByConfirmId(confirmId);
        String rejection = confirmationRejection(ticket, request, tool, argumentDigest);
        if (rejection != null) {
            return authorizeDecision(McpConstant.Confirmation.DECISION_REJECTED, "", rejection, tool.getRiskLevel());
        }
        // Consuming is guarded by status=PENDING in SQL, so a replayed confirmId loses the race.
        if (oauthMcpMapper.consumeConfirmation(ticket.getId(), LocalDateTime.now()) <= 0) {
            return authorizeDecision(McpConstant.Confirmation.DECISION_REJECTED, "",
                    "confirmation has already been used", tool.getRiskLevel());
        }
        return authorizeDecision(McpConstant.Confirmation.DECISION_AUTHORIZED, confirmId, "authorized",
                tool.getRiskLevel());
    }

    private String confirmationRejection(McpToolConfirmationRecord ticket, McpToolAuthorizeRequestDTO request,
                                         McpToolResolveResponseDTO tool, String argumentDigest) {
        if (ticket == null) {
            return "confirmation does not exist";
        }
        if (!McpConfirmationStatusEnum.PENDING.getValue().equals(ticket.getStatus())) {
            return "confirmation has already been used";
        }
        if (ticket.getExpireTime() == null || ticket.getExpireTime().isBefore(LocalDateTime.now())) {
            return "confirmation has expired";
        }
        if (!Objects.equals(ticket.getPrincipalId(), request.getPrincipalId())
                || !Objects.equals(ticket.getConnectionId(), request.getMcpConnectionId())
                || !Objects.equals(ticket.getToolId(), tool.getToolId())) {
            return "confirmation does not match the caller";
        }
        if (!Objects.equals(StringUtils.trimToEmpty(ticket.getArgumentDigest()), argumentDigest)) {
            return "confirmation arguments do not match";
        }
        return null;
    }

    private McpToolAuthorizeResponseDTO authorizeDecision(String decision, String confirmId, String message,
                                                          String riskLevel) {
        return McpToolAuthorizeResponseDTO.builder()
                .decision(decision)
                .confirmId(confirmId)
                .message(message)
                .riskLevel(riskLevel)
                .build();
    }

    @Override
    public void audit(McpAuditCommandDTO source) {
        source = Objects.requireNonNullElseGet(source, McpAuditCommandDTO::new);
        McpAuditCommand command = auditCommand(source);
        command.setId(IdWorker.getId());
        command.setTraceId(StringUtils.defaultIfBlank(command.getTraceId(), UUID.randomUUID().toString()));
        command.setConfirmId(StringUtils.defaultString(command.getConfirmId()));
        command.setIdempotencyKey(StringUtils.defaultString(command.getIdempotencyKey()));
        command.setArgumentDigest(StringUtils.defaultString(command.getArgumentDigest()));
        command.setStatus(StringUtils.defaultIfBlank(command.getStatus(), McpAuditStatusEnum.UNKNOWN.getValue()));
        command.setErrorCode(StringUtils.defaultString(command.getErrorCode()));
        command.setDurationMs(Objects.requireNonNullElse(command.getDurationMs(), 0L));
        command.setClientName(StringUtils.defaultString(command.getClientName()));
        command.setClientVersion(StringUtils.defaultString(command.getClientVersion()));
        command.setRemoteIp(StringUtils.defaultString(command.getRemoteIp()));
        oauthMcpMapper.insertAudit(command);
    }

    /**
     * Exchange an authorization code for tokens (RFC 6749). Validates the code is active,
     * authenticates the client, checks the redirect URI matches, verifies PKCE when
     * required, then issues access and refresh tokens.
     *
     * @param form               token request form (code, redirect_uri, code_verifier)
     * @param authorizationHeader client credentials for confidential clients
     * @return the token response map
     */
    private Map<String, Object> authorizationCodeToken(Map<String, String> form, String authorizationHeader) {
        String code = form.get("code");
        OAuthAuthorizationRecord authorization = oauthMcpMapper.selectAuthorizationByCodeHash(sha256(code));
        if (authorization == null || !isActiveCode(authorization)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "authorization code is invalid or expired");
        }
        OAuthRegisteredClientRecord client = requireClient(authorization.getClientId());
        authenticateClient(client, form, authorizationHeader, false);
        Map<String, Object> metadata = parseJsonMap(authorization.getTokenMetadata());
        if (!Objects.equals(metadata.get(McpConstant.Field.REDIRECT_URI), form.get(McpConstant.Field.REDIRECT_URI))) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "redirect_uri mismatch");
        }
        if (one(client.getRequirePkce())) {
            String verifier = form.get("code_verifier");
            if (StringUtils.isBlank(verifier)
                    || !Objects.equals(metadata.get(McpConstant.Field.CODE_CHALLENGE), pkceChallenge(verifier))) {
                throw oauthError(BAD_REQUEST.value(), "invalid_grant", "PKCE verification failed");
            }
        }
        return issueAndPersistTokens(authorization, client, true, "");
    }

    /**
     * Issue tokens for the client-credentials grant. Authenticates the client, requires
     * a bound service account and an active connection, creates an authorization record,
     * then issues access (and refresh) tokens.
     *
     * @param form               token request form (scope)
     * @param authorizationHeader client credentials
     * @return the token response map
     */
    private Map<String, Object> clientCredentialsToken(Map<String, String> form, String authorizationHeader) {
        OAuthRegisteredClientRecord client = requireClient(resolveClientId(form, authorizationHeader));
        authenticateClient(client, form, authorizationHeader, true);
        requireGrant(client, OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue());
        validateServiceAccountClient(client.getServiceAccountPrincipalId(), client.getTenantId());
        McpConnectionRecord connection = oauthMcpMapper.selectActiveConnection(client.getClientId(),
                client.getServiceAccountPrincipalId(), client.getTenantId(),
                OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue());
        validateConnection(connection, client.getClientId(), client.getServiceAccountPrincipalId(),
                client.getTenantId(), OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue());
        Set<String> scopes = requestedScopes(form.get(McpConstant.Field.SCOPE), client);

        OAuthAuthorizationRecord authorization = new OAuthAuthorizationRecord();
        authorization.setId(IdWorker.getId());
        authorization.setRegisteredClientId(client.getId());
        authorization.setClientId(client.getClientId());
        authorization.setPrincipalId(client.getServiceAccountPrincipalId());
        authorization.setPrincipalType(PrincipalTypeEnum.SERVICE_ACCOUNT.getValue());
        authorization.setTenantId(client.getTenantId());
        authorization.setMcpConnectionId(connection.getId());
        authorization.setAuthorizationGrantType(OAuthGrantTypeEnum.CLIENT_CREDENTIALS.getValue());
        authorization.setAuthorizedScopes(String.join(" ", scopes));
        authorization.setStateHash("");
        authorization.setAuthorizationCodeHash("");
        authorization.setTokenMetadata("{}");
        oauthMcpMapper.insertAuthorization(authorization);
        return issueAndPersistTokens(authorization, client, false, "");
    }

    /**
     * Exchange a refresh token for new tokens (RFC 6749). Detects replay of a rotated
     * refresh token and revokes the whole authorization chain on suspicion of theft;
     * otherwise rotates the refresh token and reissues access and refresh tokens.
     *
     * @param form               token request form (refresh_token)
     * @param authorizationHeader client credentials
     * @return the token response map
     */
    private Map<String, Object> refreshToken(Map<String, String> form, String authorizationHeader) {
        String refreshToken = form.get(McpConstant.Field.REFRESH_TOKEN);
        String presentedHash = sha256(refreshToken);
        OAuthAuthorizationRecord authorization = oauthMcpMapper.selectAuthorizationByRefreshTokenHash(presentedHash);
        if (authorization == null) {
            // A rotated (previous) refresh token replayed after rotation signals theft per
            // RFC 9700; revoke the whole authorization so the leaked chain is dead.
            OAuthAuthorizationRecord replayed =
                    oauthMcpMapper.selectAuthorizationByPreviousRefreshTokenHash(presentedHash);
            if (replayed != null) {
                oauthMcpMapper.revokeAuthorizationByAccessTokenJti(replayed.getAccessTokenJti(),
                        "refresh_token_replayed", LocalDateTime.now());
                throw oauthError(BAD_REQUEST.value(), "invalid_grant", "refresh token has been revoked");
            }
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "refresh token is invalid or expired");
        }
        if (authorization.getRefreshTokenExpires() == null
                || authorization.getRefreshTokenExpires().isBefore(LocalDateTime.now())
                || authorization.getRevokedTime() != null) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "refresh token is invalid or expired");
        }
        OAuthRegisteredClientRecord client = requireClient(authorization.getClientId());
        authenticateClient(client, form, authorizationHeader, false);
        return issueAndPersistTokens(authorization, client, true, presentedHash);
    }

    /**
     * Issue and persist access (and optionally refresh) tokens for an authorization,
     * verifying the principal is active and still a tenant member, signing the JWT, and
     * activating the authorization record with the new token hashes.
     *
     * @param authorization      the authorization to issue for
     * @param client             the registered client
     * @param issueRefreshToken  whether to issue a refresh token
     * @param previousRefreshHash the prior refresh-token hash, for rotation tracking
     * @return the token response map
     */
    private Map<String, Object> issueAndPersistTokens(OAuthAuthorizationRecord authorization,
                                                      OAuthRegisteredClientRecord client,
                                                      boolean issueRefreshToken,
                                                      String previousRefreshHash) {
        PrincipalDO principal = principalManager.getById(authorization.getPrincipalId());
        if (principal == null
                || !enabled(principal.getEnableFlag())
                || !tenantMembershipService.isTenantMember(authorization.getTenantId(),
                authorization.getPrincipalId())) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "principal is not active for tenant");
        }
        Set<String> scopes = splitValues(authorization.getAuthorizedScopes());
        LocalDateTime issued = LocalDateTime.now();
        LocalDateTime accessExpires = issued.plus(oauthProperties.getAccessTokenTtl());
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = orderedMap(
                McpConstant.Field.PRINCIPAL_TYPE, authorization.getPrincipalType(),
                McpConstant.Field.TENANT_ID, authorization.getTenantId(),
                McpConstant.Field.CLIENT_ID, client.getClientId(),
                McpConstant.Field.MCP_CONNECTION_ID, authorization.getMcpConnectionId(),
                McpConstant.Field.GRANT_TYPE, authorization.getAuthorizationGrantType(),
                McpConstant.Field.SCOPE, String.join(" ", scopes)
        );
        String accessToken = Jwts.builder()
                .header().keyId(KID).and()
                .issuer(oauthProperties.getIssuer())
                .audience().add(oauthProperties.getAudience()).and()
                .id(jti)
                .subject(String.valueOf(authorization.getPrincipalId()))
                .issuedAt(asDate(issued))
                .notBefore(asDate(issued.minusSeconds(5)))
                .expiration(asDate(accessExpires))
                .claims(claims)
                .signWith(keyMaterial().privateKey(), Jwts.SIG.RS256)
                .compact();

        String refreshToken = issueRefreshToken ? randomToken() : "";
        LocalDateTime refreshIssued = issueRefreshToken ? issued : null;
        LocalDateTime refreshExpires = issueRefreshToken ? issued.plus(oauthProperties.getRefreshTokenTtl()) : null;
        oauthMcpMapper.activateAuthorizationTokens(authorization.getId(), "", jti, issued, accessExpires,
                sha256(refreshToken), StringUtils.defaultString(previousRefreshHash), refreshIssued, refreshExpires,
                JsonUtil.toJsonString(claims));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put(McpConstant.Field.ACCESS_TOKEN, accessToken);
        response.put(McpConstant.Field.TOKEN_TYPE, McpConstant.OAuth.TOKEN_TYPE_BEARER);
        response.put(McpConstant.Field.EXPIRES_IN, oauthProperties.getAccessTokenTtl().toSeconds());
        response.put(McpConstant.Field.SCOPE, String.join(" ", scopes));
        if (issueRefreshToken) {
            response.put(McpConstant.Field.REFRESH_TOKEN, refreshToken);
        }
        return response;
    }

    /**
     * Parse and verify an access token's JWT signature, issuer, and audience.
     *
     * @param token the access token
     * @return the verified JWT claims
     */
    private Claims parseAccessToken(String token) {
        return Jwts.parser()
                .requireIssuer(oauthProperties.getIssuer())
                .requireAudience(oauthProperties.getAudience())
                .verifyWith(keyMaterial().publicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate that a service account is bound, enabled, and not expired, required for
     * the client-credentials grant.
     *
     * @param serviceAccountPrincipalId the service account principal id
     * @param tenantId                  tenant scope
     */
    private void validateServiceAccountClient(Long serviceAccountPrincipalId, Long tenantId) {
        if (serviceAccountPrincipalId == null || serviceAccountPrincipalId == 0 || tenantId == null || tenantId == 0) {
            throw oauthError(BAD_REQUEST.value(), "invalid_client_metadata",
                    "client_credentials requires service_account_principal_id and tenant_id");
        }
        ServiceAccountDO serviceAccount = serviceAccountManager.getOne(Wrappers.<ServiceAccountDO>lambdaQuery()
                .eq(ServiceAccountDO::getPrincipalId, serviceAccountPrincipalId)
                .eq(ServiceAccountDO::getTenantId, tenantId)
                .eq(ServiceAccountDO::getEnableFlag, EnableFlagEnum.ENABLE.getIndex())
                .last("LIMIT 1"));
        if (serviceAccount == null || (serviceAccount.getExpireTime() != null
                && serviceAccount.getExpireTime().isBefore(LocalDateTime.now()))) {
            throw oauthError(BAD_REQUEST.value(), "invalid_client_metadata", "service account is not active");
        }
    }

    /**
     * Require an authenticated principal carrying principal and tenant ids, throwing a
     * login_required OAuth error otherwise.
     *
     * @param principalHeader the principal header from the request
     */
    private void requireAuthenticatedPrincipal(RequestHeader.PrincipalHeader principalHeader) {
        if (principalHeader == null
                || principalHeader.getPrincipalId() == null
                || principalHeader.getTenantId() == null) {
            throw oauthError(UNAUTHORIZED.value(), "login_required", "authenticated principal is required");
        }
    }

    private String principalName(RequestHeader.PrincipalHeader principalHeader) {
        String name = principalHeader.getNickName();
        if (StringUtils.isBlank(name)) {
            name = principalHeader.getUserName();
        }
        return StringUtils.defaultIfBlank(name, String.valueOf(principalHeader.getPrincipalId()));
    }

    /**
     * Validate an MCP connection matches the client, principal, tenant, and grant type
     * and is still usable.
     *
     * @param connection the connection to validate
     * @param clientId   the expected client id
     * @param principalId the expected principal id
     * @param tenantId   the expected tenant id
     * @param grantType  the expected grant type
     */
    private void validateConnection(McpConnectionRecord connection, String clientId, Long principalId, Long tenantId,
                                    String grantType) {
        if (!isUsableConnection(connection, clientId, principalId, tenantId)
                || !Objects.equals(connection.getGrantType(), grantType)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "MCP connection is not active");
        }
    }

    /**
     * Return whether a connection is usable: matches the client/principal/tenant, is
     * enabled, not revoked, and not expired.
     *
     * @param connection  the connection to test
     * @param clientId    the expected client id
     * @param principalId the expected principal id
     * @param tenantId    the expected tenant id
     * @return true if the connection is usable
     */
    private boolean isUsableConnection(McpConnectionRecord connection, String clientId, Long principalId,
                                       Long tenantId) {
        return connection != null
                && Objects.equals(connection.getClientId(), clientId)
                && Objects.equals(connection.getPrincipalId(), principalId)
                && Objects.equals(connection.getTenantId(), tenantId)
                && enabled(connection.getEnableFlag())
                && connection.getRevokeTime() == null
                && (connection.getExpireTime() == null || connection.getExpireTime().isAfter(LocalDateTime.now()));
    }

    /**
     * Look up a registered client by its client id, requiring it to exist and be enabled.
     *
     * @param clientId the public client id
     * @return the registered client record
     */
    private OAuthRegisteredClientRecord requireClient(String clientId) {
        OAuthRegisteredClientRecord client = oauthMcpMapper.selectClientByClientId(clientId);
        if (client == null || !enabled(client.getEnableFlag())) {
            throw oauthError(UNAUTHORIZED.value(), "invalid_client", "client is not registered or disabled");
        }
        return client;
    }

    /**
     * Authenticate a client: public clients must allow the none method (and may be
     * rejected when a confidential client is required); confidential clients must
     * present a verifiable, non-expired secret.
     *
     * @param client              the registered client
     * @param form                the token request form (client_secret in body)
     * @param authorizationHeader the Authorization header (client_secret_basic)
     * @param confidentialRequired whether a confidential client is required
     */
    private void authenticateClient(OAuthRegisteredClientRecord client, Map<String, String> form,
                                    String authorizationHeader, boolean confidentialRequired) {
        if (OAuthClientTypeEnum.PUBLIC.getValue().equals(client.getClientType())) {
            if (confidentialRequired
                    || !splitValues(client.getClientAuthMethods()).contains(McpConstant.OAuth.AUTH_METHOD_NONE)) {
                throw oauthError(UNAUTHORIZED.value(), "invalid_client", "confidential client is required");
            }
            return;
        }
        String secret = resolveClientSecret(form, authorizationHeader);
        if (StringUtils.isBlank(secret) || !PasswordUtil.verify(secret, client.getClientSecretHash())) {
            throw oauthError(UNAUTHORIZED.value(), "invalid_client", "client authentication failed");
        }
        if (client.getClientSecretExpiresAt() != null
                && client.getClientSecretExpiresAt().isBefore(LocalDateTime.now())) {
            throw oauthError(UNAUTHORIZED.value(), "invalid_client", "client secret expired");
        }
    }

    /**
     * Require a client to allow the given grant type.
     *
     * @param client    the registered client
     * @param grantType the grant type to check
     */
    private void requireGrant(OAuthRegisteredClientRecord client, String grantType) {
        if (!splitValues(client.getAuthorizationGrantTypes()).contains(grantType)) {
            throw oauthError(BAD_REQUEST.value(), "unauthorized_client", "grant type is not allowed");
        }
    }

    /**
     * Resolve the requested scopes against the client's allowed scopes, defaulting to all
     * allowed scopes when none are requested, and rejecting any scope the client does not
     * allow.
     *
     * @param rawScopes the raw space-delimited scope string
     * @param client    the registered client
     * @return the resolved scope set
     */
    private Set<String> requestedScopes(String rawScopes, OAuthRegisteredClientRecord client) {
        Set<String> allowed = splitValues(client.getScopes());
        Set<String> requested = StringUtils.isBlank(rawScopes) ? allowed : splitValues(rawScopes);
        if (!allowed.containsAll(requested)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_scope", "requested scope is not allowed");
        }
        return requested;
    }

    private McpToolDefinitionDTO toolToMcp(McpToolRecord tool) {
        return McpToolDefinitionDTO.builder()
                .name(tool.getToolName())
                .title(tool.getToolTitle())
                .description(StringUtils.defaultIfBlank(tool.getRemark(), tool.getToolTitle()))
                .inputSchema(inputSchemaOf(tool))
                .annotations(McpToolDefinitionDTO.Annotations.builder()
                        .readOnlyHint(one(tool.getReadOnlyHint()))
                        .destructiveHint(one(tool.getDestructiveHint()))
                        .idempotentHint(one(tool.getIdempotentHint()))
                        .openWorldHint(one(tool.getOpenWorldHint()))
                        .build())
                .meta(McpToolDefinitionDTO.Metadata.builder()
                        .toolId(tool.getToolId())
                        .permissionCode(tool.getPermissionCode())
                        .riskLevel(tool.getRiskLevel())
                        .build())
                .build();
    }

    /**
     * Resolve the input schema for a tool. When the catalog carries one in {@code tool_ext}
     * (populated by {@link McpOpenApiAggregator} from the static OpenAPI specs), surface it;
     * otherwise fall back to the static default so tools/list never breaks.
     */
    private Map<String, Object> inputSchemaOf(McpToolRecord tool) {
        String ext = tool.getToolExt();
        if (StringUtils.isBlank(ext)) {
            return McpConstant.ToolDefinition.DEFAULT_INPUT_SCHEMA;
        }
        try {
            JsonNode node = objectMapper.readTree(ext).path("inputSchema");
            if (node.isMissingNode() || node.isEmpty()) {
                return McpConstant.ToolDefinition.DEFAULT_INPUT_SCHEMA;
            }
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return McpConstant.ToolDefinition.DEFAULT_INPUT_SCHEMA;
        }
    }

    private McpToolResolveResponseDTO resolvedTool(McpToolRecord tool) {
        return McpToolResolveResponseDTO.builder()
                .toolId(tool.getToolId())
                .toolName(tool.getToolName())
                .permissionCode(tool.getPermissionCode())
                .riskLevel(tool.getRiskLevel())
                .serviceName(tool.getServiceName())
                .apiPath(tool.getApiPath())
                .httpMethod(tool.getHttpMethod())
                .build();
    }

    private McpAuditCommand auditCommand(McpAuditCommandDTO source) {
        McpAuditCommand target = new McpAuditCommand();
        target.setTraceId(source.getTraceId());
        target.setTenantId(source.getTenantId());
        target.setPrincipalId(source.getPrincipalId());
        target.setPrincipalType(source.getPrincipalType());
        target.setClientId(source.getClientId());
        target.setConnectionId(source.getConnectionId());
        target.setToolId(source.getToolId());
        target.setToolName(source.getToolName());
        target.setPermissionCode(source.getPermissionCode());
        target.setRiskLevel(source.getRiskLevel());
        target.setConfirmId(source.getConfirmId());
        target.setIdempotencyKey(source.getIdempotencyKey());
        target.setArgumentDigest(source.getArgumentDigest());
        target.setStatus(source.getStatus());
        target.setErrorCode(source.getErrorCode());
        target.setDurationMs(source.getDurationMs());
        target.setClientName(source.getClientName());
        target.setClientVersion(source.getClientVersion());
        target.setRemoteIp(source.getRemoteIp());
        return target;
    }

    /**
     * Return whether an authorization code is still active: not revoked, present, and
     * unexpired.
     *
     * @param authorization the authorization record
     * @return true if the code is active
     */
    private boolean isActiveCode(OAuthAuthorizationRecord authorization) {
        return authorization.getRevokedTime() == null
                && StringUtils.isNotBlank(authorization.getAuthorizationCodeHash())
                && authorization.getAuthorizationCodeExpires() != null
                && authorization.getAuthorizationCodeExpires().isAfter(LocalDateTime.now());
    }

    /**
     * Return whether an authorization is active at the given instant: present, not
     * revoked, and the access token has not expired.
     *
     * @param authorization the authorization record
     * @param now           the instant to check against
     * @return true if the authorization is active
     */
    private boolean isActiveAuthorization(OAuthAuthorizationRecord authorization, LocalDateTime now) {
        return authorization != null
                && authorization.getRevokedTime() == null
                && authorization.getAccessTokenExpires() != null
                && authorization.getAccessTokenExpires().isAfter(now);
    }

    private String resolveClientId(Map<String, String> form, String authorizationHeader) {
        BasicClientCredentials basic = basicCredentials(authorizationHeader);
        return basic != null ? basic.clientId() : form.get(McpConstant.Field.CLIENT_ID);
    }

    private String resolveClientSecret(Map<String, String> form, String authorizationHeader) {
        BasicClientCredentials basic = basicCredentials(authorizationHeader);
        return basic != null ? basic.clientSecret() : form.get("client_secret");
    }

    private BasicClientCredentials basicCredentials(String authorizationHeader) {
        if (StringUtils.isBlank(authorizationHeader) || !authorizationHeader.startsWith("Basic ")) {
            return null;
        }
        String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6)),
                StandardCharsets.UTF_8);
        int idx = decoded.indexOf(':');
        if (idx <= 0) {
            return null;
        }
        return new BasicClientCredentials(decoded.substring(0, idx), decoded.substring(idx + 1));
    }

    private Set<String> normalizeSet(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::stringValue).filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        return splitValues(stringValue(value));
    }

    private Set<String> splitValues(String value) {
        if (StringUtils.isBlank(value)) {
            return Set.of();
        }
        String[] parts = value.trim().split(McpConstant.Scope.DELIMITER_REGEX);
        Set<String> out = new HashSet<>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }
        return JsonUtil.parseObject(json, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {
        });
    }

    private RuntimeException oauthError(int status, String error, String description) {
        return new OAuthProtocolException(status, error, description);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return DecodeUtil.base64Url(bytes);
    }

    private String pkceChallenge(String verifier) {
        return DecodeUtil.sha256Base64Url(verifier);
    }

    private String sha256(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return DecodeUtil.sha256Hex(value);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Date asDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Long numberClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return Objects.toString(value, "");
    }

    private boolean enabled(Byte value) {
        return Objects.equals(value, (byte) 0);
    }

    private boolean one(Byte value) {
        return Objects.equals(value, (byte) 1);
    }

    private Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    /**
     * Return the RSA key material, loading and caching it lazily under double-checked
     * locking.
     *
     * @return the cached key material
     */
    private KeyMaterial keyMaterial() {
        KeyMaterial current = keyMaterial;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (keyMaterial != null) {
                return keyMaterial;
            }
            keyMaterial = loadKeyMaterial();
            return keyMaterial;
        }
    }

    /**
     * Load the RSA key pair from configured PEM keys, generating an ephemeral 2048-bit
     * pair when neither is configured. Both keys must be provided together.
     *
     * @return the loaded key material
     */
    private KeyMaterial loadKeyMaterial() {
        try {
            String privateKeyBase64 = oauthProperties.getJwt().getPrivateKey();
            String publicKeyBase64 = oauthProperties.getJwt().getPublicKey();
            if (StringUtils.isNotBlank(privateKeyBase64) || StringUtils.isNotBlank(publicKeyBase64)) {
                if (StringUtils.isAnyBlank(privateKeyBase64, publicKeyBase64)) {
                    throw new InvalidKeyException("Both OAuth private and public keys must be configured together");
                }
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = keyFactory.generatePrivate(
                        new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
                PublicKey publicKey = keyFactory.generatePublic(
                        new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
                return new KeyMaterial(privateKey, publicKey);
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            log.warn("dc3.oauth.jwt.private-key/public-key are not configured. Generated an ephemeral OAuth JWT key "
                    + "for this process; configure stable RSA keys before production use.");
            return new KeyMaterial(pair.getPrivate(), pair.getPublic());
        } catch (Exception e) {
            throw new IllegalStateException("OAuth RSA key initialization failed", e);
        }
    }

    private record BasicClientCredentials(String clientId, String clientSecret) {
    }

    private record KeyMaterial(PrivateKey privateKey, PublicKey publicKey) {
    }

    @Getter
    public static final class OAuthProtocolException extends ResponseStatusException {

        private final String error;
        private final String description;

        public OAuthProtocolException(int status, String error, String description) {
            super(org.springframework.http.HttpStatus.valueOf(status), description);
            this.error = error;
            this.description = description;
        }
    }

}
