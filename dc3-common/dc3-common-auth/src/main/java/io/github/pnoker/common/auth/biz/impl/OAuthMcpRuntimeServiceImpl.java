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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.dal.ServiceAccountManager;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.mapper.OAuthMcpMapper;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
    private static final String GRANT_REFRESH_TOKEN = "refresh_token";
    private static final String CLIENT_TYPE_PUBLIC = "PUBLIC";
    private static final String CLIENT_TYPE_CONFIDENTIAL = "CONFIDENTIAL";
    private static final String SCOPE_TOOLS_LIST = "mcp:tools:list";
    private static final String SCOPE_TOOLS_CALL = "mcp:tools:call";
    private static final String SCOPE_TOOLS_CALL_HIGH = "mcp:tools:call:high";
    private static final String KID = "dc3-oauth-rsa";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuthMcpMapper oauthMcpMapper;
    private final TenantMembershipService tenantMembershipService;
    private final PrincipalManager principalManager;
    private final ServiceAccountManager serviceAccountManager;

    @Value("${dc3.oauth.issuer:http://localhost:8300/auth}")
    private String issuer;

    @Value("${dc3.oauth.audience:dc3-mcp}")
    private String audience;

    @Value("${dc3.oauth.authorization-code-ttl:PT5M}")
    private Duration authorizationCodeTtl;

    @Value("${dc3.oauth.access-token-ttl:PT15M}")
    private Duration accessTokenTtl;

    @Value("${dc3.oauth.refresh-token-ttl:P30D}")
    private Duration refreshTokenTtl;

    @Value("${dc3.oauth.jwt.private-key:}")
    private String privateKeyBase64;

    @Value("${dc3.oauth.jwt.public-key:}")
    private String publicKeyBase64;

    private volatile KeyMaterial keyMaterial;

    @Override
    public Map<String, Object> authorizationServerMetadata() {
        return orderedMap(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/oauth2/authorize",
                "token_endpoint", issuer + "/oauth2/token",
                "jwks_uri", issuer + "/oauth2/jwks",
                "revocation_endpoint", issuer + "/oauth2/revoke",
                "registration_endpoint", issuer + "/oauth2/register",
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of(GRANT_AUTHORIZATION_CODE, GRANT_CLIENT_CREDENTIALS,
                        GRANT_REFRESH_TOKEN),
                "code_challenge_methods_supported", List.of("S256"),
                "scopes_supported", List.of(SCOPE_TOOLS_LIST, SCOPE_TOOLS_CALL, SCOPE_TOOLS_CALL_HIGH,
                        "mcp:resources:read"),
                "token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post",
                        "none")
        );
    }

    @Override
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = (RSAPublicKey) keyMaterial().publicKey();
        return Map.of("keys", List.of(orderedMap(
                "kty", "RSA",
                "use", "sig",
                "kid", KID,
                "alg", "RS256",
                "n", base64Url(publicKey.getModulus().toByteArray()),
                "e", base64Url(publicKey.getPublicExponent().toByteArray())
        )));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerClient(Map<String, Object> request,
                                               RequestHeader.PrincipalHeader principalHeader) {
        String clientName = stringValue(request.get("client_name"));
        if (StringUtils.isBlank(clientName)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_client_metadata", "client_name is required");
        }
        String clientType = StringUtils.defaultIfBlank(stringValue(request.get("client_type")), CLIENT_TYPE_PUBLIC)
                .toUpperCase();
        if (!Set.of(CLIENT_TYPE_PUBLIC, CLIENT_TYPE_CONFIDENTIAL).contains(clientType)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_client_metadata", "unsupported client_type");
        }

        Set<String> grants = normalizeSet(request.get("grant_types"));
        if (grants.isEmpty()) {
            grants = CLIENT_TYPE_PUBLIC.equals(clientType) ? Set.of(GRANT_AUTHORIZATION_CODE)
                    : Set.of(GRANT_CLIENT_CREDENTIALS);
        }
        Set<String> scopes = normalizeSet(request.get("scope"));
        if (scopes.isEmpty()) {
            scopes = Set.of(SCOPE_TOOLS_LIST, SCOPE_TOOLS_CALL);
        }
        Set<String> redirects = normalizeSet(request.get("redirect_uris"));
        if (grants.contains(GRANT_AUTHORIZATION_CODE) && redirects.isEmpty()) {
            throw oauthError(BAD_REQUEST.value(), "invalid_redirect_uri", "redirect_uris is required");
        }

        Long ownerPrincipalId = Objects.nonNull(principalHeader) ? principalHeader.getPrincipalId() : 0L;
        Long serviceAccountPrincipalId = longValue(request.get("service_account_principal_id"));
        Long tenantId = longValue(request.get("tenant_id"));
        if (grants.contains(GRANT_CLIENT_CREDENTIALS)) {
            validateServiceAccountClient(serviceAccountPrincipalId, tenantId);
        }

        String clientId = "dc3_" + UUID.randomUUID().toString().replace("-", "");
        String clientSecret = null;
        String secretHash = "";
        String authMethods = "none";
        if (CLIENT_TYPE_CONFIDENTIAL.equals(clientType)) {
            clientSecret = randomToken();
            secretHash = PasswordUtil.encode(clientSecret);
            authMethods = "client_secret_basic client_secret_post";
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
        client.setRequirePkce((byte) (grants.contains(GRANT_AUTHORIZATION_CODE) ? 1 : 0));
        client.setRequireConsent((byte) 1);
        client.setEnableFlag((byte) 0);
        oauthMcpMapper.insertClient(client);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client_id", clientId);
        response.put("client_name", clientName);
        response.put("client_type", clientType);
        response.put("grant_types", grants);
        response.put("redirect_uris", redirects);
        response.put("scope", String.join(" ", scopes));
        response.put("token_endpoint_auth_method", CLIENT_TYPE_CONFIDENTIAL.equals(clientType)
                ? "client_secret_basic" : "none");
        if (clientSecret != null) {
            response.put("client_secret", clientSecret);
        }
        return response;
    }

    @Override
    public List<OAuthRegisteredClientRecord> listClients(RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        return oauthMcpMapper.listClientsByOwner(principalHeader.getPrincipalId(), principalHeader.getTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public URI authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader) {
        if (principalHeader == null || principalHeader.getPrincipalId() == null) {
            throw oauthError(UNAUTHORIZED.value(), "login_required", "authenticated principal is required");
        }
        if (!"code".equals(params.get("response_type"))) {
            throw oauthError(BAD_REQUEST.value(), "unsupported_response_type", "only code is supported");
        }
        OAuthRegisteredClientRecord client = requireClient(params.get("client_id"));
        requireGrant(client, GRANT_AUTHORIZATION_CODE);
        String redirectUri = params.get("redirect_uri");
        if (!splitValues(client.getRedirectUris()).contains(redirectUri)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "redirect_uri mismatch");
        }
        if (one(client.getRequirePkce())) {
            if (!"S256".equals(params.get("code_challenge_method"))
                    || StringUtils.isBlank(params.get("code_challenge"))) {
                throw oauthError(BAD_REQUEST.value(), "invalid_request", "PKCE S256 is required");
            }
        }
        Set<String> scopes = requestedScopes(params.get("scope"), client);
        Long tenantId = longValue(params.get("tenant_id"));
        if (tenantId == null || tenantId == 0) {
            tenantId = principalHeader.getTenantId();
        }
        if (tenantId == null || !tenantMembershipService.isTenantMember(tenantId, principalHeader.getPrincipalId())) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "principal is not a member of the tenant");
        }

        Long connectionId = longValue(params.get("mcp_connection_id"));
        McpConnectionRecord connection = connectionId == null || connectionId == 0
                ? oauthMcpMapper.selectActiveConnection(client.getClientId(), principalHeader.getPrincipalId(),
                tenantId, GRANT_AUTHORIZATION_CODE)
                : oauthMcpMapper.selectConnectionById(connectionId);
        validateConnection(connection, client.getClientId(), principalHeader.getPrincipalId(), tenantId,
                GRANT_AUTHORIZATION_CODE);

        String code = randomToken();
        OAuthAuthorizationRecord authorization = new OAuthAuthorizationRecord();
        authorization.setId(IdWorker.getId());
        authorization.setRegisteredClientId(client.getId());
        authorization.setClientId(client.getClientId());
        authorization.setPrincipalId(principalHeader.getPrincipalId());
        authorization.setPrincipalType(StringUtils.defaultIfBlank(principalHeader.getPrincipalType(), "USER"));
        authorization.setTenantId(tenantId);
        authorization.setMcpConnectionId(connection.getId());
        authorization.setAuthorizationGrantType(GRANT_AUTHORIZATION_CODE);
        authorization.setAuthorizedScopes(String.join(" ", scopes));
        authorization.setStateHash(sha256(params.get("state")));
        authorization.setAuthorizationCodeHash(sha256(code));
        authorization.setAuthorizationCodeIssued(LocalDateTime.now());
        authorization.setAuthorizationCodeExpires(LocalDateTime.now().plus(authorizationCodeTtl));
        authorization.setTokenMetadata(JsonUtil.toJsonString(orderedMap(
                "redirect_uri", redirectUri,
                "code_challenge", params.get("code_challenge"),
                "code_challenge_method", params.get("code_challenge_method")
        )));
        oauthMcpMapper.insertAuthorization(authorization);

        StringBuilder target = new StringBuilder(redirectUri)
                .append(redirectUri.contains("?") ? '&' : '?')
                .append("code=").append(urlEncode(code));
        if (StringUtils.isNotBlank(params.get("state"))) {
            target.append("&state=").append(urlEncode(params.get("state")));
        }
        return URI.create(target.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> token(Map<String, String> form, String authorizationHeader) {
        String grantType = form.get("grant_type");
        if (GRANT_AUTHORIZATION_CODE.equals(grantType)) {
            return authorizationCodeToken(form, authorizationHeader);
        }
        if (GRANT_CLIENT_CREDENTIALS.equals(grantType)) {
            return clientCredentialsToken(form, authorizationHeader);
        }
        if (GRANT_REFRESH_TOKEN.equals(grantType)) {
            return refreshToken(form, authorizationHeader);
        }
        throw oauthError(BAD_REQUEST.value(), "unsupported_grant_type", "unsupported grant_type");
    }

    @Override
    public Map<String, Object> introspect(String token) {
        try {
            Claims claims = parseAccessToken(token);
            String jti = claims.getId();
            OAuthAuthorizationRecord authorization = oauthMcpMapper.selectAuthorizationByAccessTokenJti(jti);
            if (!isActiveAuthorization(authorization, LocalDateTime.now())) {
                return Map.of("active", false);
            }
            Long tenantId = numberClaim(claims, "tenant_id");
            Long principalId = Long.valueOf(claims.getSubject());
            Long connectionId = numberClaim(claims, "mcp_connection_id");
            String clientId = stringValue(claims.get("client_id"));
            McpConnectionRecord connection = oauthMcpMapper.selectConnectionById(connectionId);
            if (!isUsableConnection(connection, clientId, principalId, tenantId)) {
                return Map.of("active", false);
            }
            PrincipalDO principal = principalManager.getById(principalId);
            if (principal == null || !enabled(principal.getEnableFlag())
                    || !tenantMembershipService.isTenantMember(tenantId, principalId)) {
                return Map.of("active", false);
            }
            return orderedMap(
                    "active", true,
                    "iss", claims.getIssuer(),
                    "aud", claims.getAudience(),
                    "sub", claims.getSubject(),
                    "jti", jti,
                    "exp", claims.getExpiration().toInstant().getEpochSecond(),
                    "iat", claims.getIssuedAt().toInstant().getEpochSecond(),
                    "tenant_id", tenantId,
                    "principal_id", principalId,
                    "principal_type", stringValue(claims.get("principal_type")),
                    "principal_name", principal.getPrincipalName(),
                    "display_name", principal.getDisplayName(),
                    "client_id", clientId,
                    "mcp_connection_id", connectionId,
                    "grant_type", stringValue(claims.get("grant_type")),
                    "scope", stringValue(claims.get("scope"))
            );
        } catch (RuntimeException e) {
            return Map.of("active", false);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> revoke(Map<String, String> form, String authorizationHeader) {
        String token = form.get("token");
        if (StringUtils.isBlank(token)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "token is required");
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            Claims claims = parseAccessToken(token);
            oauthMcpMapper.revokeAuthorizationByAccessTokenJti(claims.getId(), "revoke", now);
        } catch (RuntimeException e) {
            oauthMcpMapper.revokeAuthorizationByRefreshTokenHash(sha256(token), "revoke", now);
        }
        return Map.of("revoked", true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshToolCatalog() {
        int changed = 0;
        for (McpToolRecord candidate : oauthMcpMapper.listRegistryToolCandidates()) {
            McpToolRecord existing = oauthMcpMapper.selectToolByToolId(candidate.getToolId());
            if (existing == null) {
                candidate.setId(IdWorker.getId());
                changed += oauthMcpMapper.insertTool(candidate);
            } else if (!Objects.equals(existing.getSchemaHash(), candidate.getSchemaHash())
                    || !Objects.equals(existing.getPermissionCode(), candidate.getPermissionCode())
                    || !Objects.equals(existing.getApiPath(), candidate.getApiPath())) {
                candidate.setId(existing.getId());
                changed += oauthMcpMapper.updateTool(candidate);
            }
        }
        return changed;
    }

    @Override
    public List<McpToolRecord> listToolCatalog(String keyword, String riskLevel, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 200 : limit, 500));
        return oauthMcpMapper.listToolCatalog(StringUtils.trimToEmpty(keyword), StringUtils.trimToEmpty(riskLevel),
                boundedLimit);
    }

    @Override
    public List<McpConnectionRecord> listConnections(RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        return oauthMcpMapper.listConnectionsByPrincipal(principalHeader.getTenantId(),
                principalHeader.getPrincipalId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpConnectionRecord createConnection(McpConnectionRecord connection,
                                                RequestHeader.PrincipalHeader principalHeader) {
        requireAuthenticatedPrincipal(principalHeader);
        OAuthRegisteredClientRecord client = requireClient(connection.getClientId());
        String grantType = StringUtils.defaultIfBlank(connection.getGrantType(), GRANT_AUTHORIZATION_CODE);
        requireGrant(client, grantType);

        Long tenantId = Objects.requireNonNullElse(connection.getTenantId(), principalHeader.getTenantId());
        Long principalId = Objects.requireNonNullElse(connection.getPrincipalId(), principalHeader.getPrincipalId());
        String principalType = StringUtils.defaultIfBlank(connection.getPrincipalType(),
                StringUtils.defaultIfBlank(principalHeader.getPrincipalType(), "USER"));

        if (GRANT_CLIENT_CREDENTIALS.equals(grantType)) {
            if (!"SERVICE_ACCOUNT".equals(principalType)) {
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
        connection.setConnectionName(StringUtils.defaultIfBlank(connection.getConnectionName(), client.getClientName()));
        connection.setClientId(client.getClientId());
        connection.setPrincipalId(principalId);
        connection.setPrincipalType(principalType);
        connection.setTenantId(tenantId);
        connection.setGrantType(grantType);
        connection.setEnableFlag((byte) 0);
        connection.setRemark(StringUtils.defaultString(connection.getRemark()));
        oauthMcpMapper.insertConnection(connection);
        return connection;
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
                || !Objects.equals(connection.getPrincipalId(), principalHeader.getPrincipalId())) {
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
    public List<Map<String, Object>> listVisibleTools(Long tenantId, Long principalId, Long connectionId,
                                                       Set<String> scopes) {
        if (!scopes.contains(SCOPE_TOOLS_LIST) && !scopes.contains(SCOPE_TOOLS_CALL)) {
            throw oauthError(UNAUTHORIZED.value(), "insufficient_scope", "mcp:tools:list scope is required");
        }
        boolean allowHighRisk = scopes.contains(SCOPE_TOOLS_CALL_HIGH);
        return oauthMcpMapper.listVisibleTools(tenantId, principalId, connectionId, allowHighRisk)
                .stream()
                .map(this::toolToMcp)
                .toList();
    }

    @Override
    public McpToolRecord resolveVisibleTool(Long tenantId, Long principalId, Long connectionId, String toolName,
                                            Set<String> scopes) {
        if (!scopes.contains(SCOPE_TOOLS_CALL)) {
            throw oauthError(UNAUTHORIZED.value(), "insufficient_scope", "mcp:tools:call scope is required");
        }
        boolean allowHighRisk = scopes.contains(SCOPE_TOOLS_CALL_HIGH);
        McpToolRecord tool = oauthMcpMapper.selectVisibleToolByName(tenantId, principalId, connectionId, toolName,
                allowHighRisk);
        if (tool == null) {
            throw oauthError(UNAUTHORIZED.value(), "access_denied", "tool is not visible for this connection");
        }
        oauthMcpMapper.updateConnectionLastUsed(connectionId, LocalDateTime.now());
        return tool;
    }

    @Override
    public void audit(McpAuditCommand command) {
        command.setId(IdWorker.getId());
        command.setTraceId(StringUtils.defaultIfBlank(command.getTraceId(), UUID.randomUUID().toString()));
        command.setConfirmId(StringUtils.defaultString(command.getConfirmId()));
        command.setIdempotencyKey(StringUtils.defaultString(command.getIdempotencyKey()));
        command.setArgumentDigest(StringUtils.defaultString(command.getArgumentDigest()));
        command.setStatus(StringUtils.defaultIfBlank(command.getStatus(), "UNKNOWN"));
        command.setErrorCode(StringUtils.defaultString(command.getErrorCode()));
        command.setDurationMs(Objects.requireNonNullElse(command.getDurationMs(), 0L));
        command.setClientName(StringUtils.defaultString(command.getClientName()));
        command.setClientVersion(StringUtils.defaultString(command.getClientVersion()));
        command.setRemoteIp(StringUtils.defaultString(command.getRemoteIp()));
        oauthMcpMapper.insertAudit(command);
    }

    private Map<String, Object> authorizationCodeToken(Map<String, String> form, String authorizationHeader) {
        String code = form.get("code");
        OAuthAuthorizationRecord authorization = oauthMcpMapper.selectAuthorizationByCodeHash(sha256(code));
        if (authorization == null || !isActiveCode(authorization)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "authorization code is invalid or expired");
        }
        OAuthRegisteredClientRecord client = requireClient(authorization.getClientId());
        authenticateClient(client, form, authorizationHeader, false);
        Map<String, Object> metadata = parseJsonMap(authorization.getTokenMetadata());
        if (!Objects.equals(metadata.get("redirect_uri"), form.get("redirect_uri"))) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "redirect_uri mismatch");
        }
        if (one(client.getRequirePkce())) {
            String verifier = form.get("code_verifier");
            if (StringUtils.isBlank(verifier)
                    || !Objects.equals(metadata.get("code_challenge"), pkceChallenge(verifier))) {
                throw oauthError(BAD_REQUEST.value(), "invalid_grant", "PKCE verification failed");
            }
        }
        return issueAndPersistTokens(authorization, client, true);
    }

    private Map<String, Object> clientCredentialsToken(Map<String, String> form, String authorizationHeader) {
        OAuthRegisteredClientRecord client = requireClient(resolveClientId(form, authorizationHeader));
        authenticateClient(client, form, authorizationHeader, true);
        requireGrant(client, GRANT_CLIENT_CREDENTIALS);
        validateServiceAccountClient(client.getServiceAccountPrincipalId(), client.getTenantId());
        McpConnectionRecord connection = oauthMcpMapper.selectActiveConnection(client.getClientId(),
                client.getServiceAccountPrincipalId(), client.getTenantId(), GRANT_CLIENT_CREDENTIALS);
        validateConnection(connection, client.getClientId(), client.getServiceAccountPrincipalId(),
                client.getTenantId(), GRANT_CLIENT_CREDENTIALS);
        Set<String> scopes = requestedScopes(form.get("scope"), client);

        OAuthAuthorizationRecord authorization = new OAuthAuthorizationRecord();
        authorization.setId(IdWorker.getId());
        authorization.setRegisteredClientId(client.getId());
        authorization.setClientId(client.getClientId());
        authorization.setPrincipalId(client.getServiceAccountPrincipalId());
        authorization.setPrincipalType("SERVICE_ACCOUNT");
        authorization.setTenantId(client.getTenantId());
        authorization.setMcpConnectionId(connection.getId());
        authorization.setAuthorizationGrantType(GRANT_CLIENT_CREDENTIALS);
        authorization.setAuthorizedScopes(String.join(" ", scopes));
        authorization.setStateHash("");
        authorization.setAuthorizationCodeHash("");
        authorization.setTokenMetadata("{}");
        oauthMcpMapper.insertAuthorization(authorization);
        return issueAndPersistTokens(authorization, client, false);
    }

    private Map<String, Object> refreshToken(Map<String, String> form, String authorizationHeader) {
        String refreshToken = form.get("refresh_token");
        OAuthAuthorizationRecord authorization = oauthMcpMapper.selectAuthorizationByRefreshTokenHash(
                sha256(refreshToken));
        if (authorization == null || authorization.getRefreshTokenExpires() == null
                || authorization.getRefreshTokenExpires().isBefore(LocalDateTime.now())
                || authorization.getRevokedTime() != null) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "refresh token is invalid or expired");
        }
        OAuthRegisteredClientRecord client = requireClient(authorization.getClientId());
        authenticateClient(client, form, authorizationHeader, false);
        return issueAndPersistTokens(authorization, client, true);
    }

    private Map<String, Object> issueAndPersistTokens(OAuthAuthorizationRecord authorization,
                                                       OAuthRegisteredClientRecord client,
                                                       boolean issueRefreshToken) {
        PrincipalDO principal = principalManager.getById(authorization.getPrincipalId());
        if (principal == null || !enabled(principal.getEnableFlag())
                || !tenantMembershipService.isTenantMember(authorization.getTenantId(), authorization.getPrincipalId())) {
            throw oauthError(BAD_REQUEST.value(), "invalid_grant", "principal is not active for tenant");
        }
        Set<String> scopes = splitValues(authorization.getAuthorizedScopes());
        LocalDateTime issued = LocalDateTime.now();
        LocalDateTime accessExpires = issued.plus(accessTokenTtl);
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = orderedMap(
                "principal_type", authorization.getPrincipalType(),
                "tenant_id", authorization.getTenantId(),
                "client_id", client.getClientId(),
                "mcp_connection_id", authorization.getMcpConnectionId(),
                "grant_type", authorization.getAuthorizationGrantType(),
                "scope", String.join(" ", scopes)
        );
        String accessToken = Jwts.builder()
                .header().keyId(KID).and()
                .issuer(issuer)
                .audience().add(audience).and()
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
        LocalDateTime refreshExpires = issueRefreshToken ? issued.plus(refreshTokenTtl) : null;
        oauthMcpMapper.activateAuthorizationTokens(authorization.getId(), "", jti, issued, accessExpires,
                sha256(refreshToken), refreshIssued, refreshExpires, JsonUtil.toJsonString(claims));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", accessTokenTtl.toSeconds());
        response.put("scope", String.join(" ", scopes));
        if (issueRefreshToken) {
            response.put("refresh_token", refreshToken);
        }
        return response;
    }

    private Claims parseAccessToken(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .requireAudience(audience)
                .verifyWith(keyMaterial().publicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

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

    private void requireAuthenticatedPrincipal(RequestHeader.PrincipalHeader principalHeader) {
        if (principalHeader == null || principalHeader.getPrincipalId() == null || principalHeader.getTenantId() == null) {
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

    private void validateConnection(McpConnectionRecord connection, String clientId, Long principalId, Long tenantId,
                                    String grantType) {
        if (!isUsableConnection(connection, clientId, principalId, tenantId)
                || !Objects.equals(connection.getGrantType(), grantType)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_request", "MCP connection is not active");
        }
    }

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

    private OAuthRegisteredClientRecord requireClient(String clientId) {
        OAuthRegisteredClientRecord client = oauthMcpMapper.selectClientByClientId(clientId);
        if (client == null || !enabled(client.getEnableFlag())) {
            throw oauthError(UNAUTHORIZED.value(), "invalid_client", "client is not registered or disabled");
        }
        return client;
    }

    private void authenticateClient(OAuthRegisteredClientRecord client, Map<String, String> form,
                                    String authorizationHeader, boolean confidentialRequired) {
        if (CLIENT_TYPE_PUBLIC.equals(client.getClientType())) {
            if (confidentialRequired || !splitValues(client.getClientAuthMethods()).contains("none")) {
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

    private void requireGrant(OAuthRegisteredClientRecord client, String grantType) {
        if (!splitValues(client.getAuthorizationGrantTypes()).contains(grantType)) {
            throw oauthError(BAD_REQUEST.value(), "unauthorized_client", "grant type is not allowed");
        }
    }

    private Set<String> requestedScopes(String rawScopes, OAuthRegisteredClientRecord client) {
        Set<String> allowed = splitValues(client.getScopes());
        Set<String> requested = StringUtils.isBlank(rawScopes) ? allowed : splitValues(rawScopes);
        if (!allowed.containsAll(requested)) {
            throw oauthError(BAD_REQUEST.value(), "invalid_scope", "requested scope is not allowed");
        }
        return requested;
    }

    private Map<String, Object> toolToMcp(McpToolRecord tool) {
        return orderedMap(
                "name", tool.getToolName(),
                "title", tool.getToolTitle(),
                "description", StringUtils.defaultIfBlank(tool.getRemark(), tool.getToolTitle()),
                "inputSchema", orderedMap("type", "object", "additionalProperties", true),
                "annotations", orderedMap(
                        "readOnlyHint", one(tool.getReadOnlyHint()),
                        "destructiveHint", one(tool.getDestructiveHint()),
                        "idempotentHint", one(tool.getIdempotentHint()),
                        "openWorldHint", one(tool.getOpenWorldHint())
                ),
                "_meta", orderedMap(
                        "tool_id", tool.getToolId(),
                        "permission_code", tool.getPermissionCode(),
                        "risk_level", tool.getRiskLevel()
                )
        );
    }

    private boolean isActiveCode(OAuthAuthorizationRecord authorization) {
        return authorization.getRevokedTime() == null
                && StringUtils.isNotBlank(authorization.getAuthorizationCodeHash())
                && authorization.getAuthorizationCodeExpires() != null
                && authorization.getAuthorizationCodeExpires().isAfter(LocalDateTime.now());
    }

    private boolean isActiveAuthorization(OAuthAuthorizationRecord authorization, LocalDateTime now) {
        return authorization != null
                && authorization.getRevokedTime() == null
                && authorization.getAccessTokenExpires() != null
                && authorization.getAccessTokenExpires().isAfter(now);
    }

    private String resolveClientId(Map<String, String> form, String authorizationHeader) {
        BasicClientCredentials basic = basicCredentials(authorizationHeader);
        return basic != null ? basic.clientId() : form.get("client_id");
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
        String[] parts = value.trim().split("[\\s,]+");
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
        return base64Url(bytes);
    }

    private String pkceChallenge(String verifier) {
        return base64Url(sha256Bytes(verifier));
    }

    private String sha256(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return java.util.HexFormat.of().formatHex(sha256Bytes(value));
    }

    private byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stripLeadingZero(bytes));
    }

    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            return java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
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

    private KeyMaterial loadKeyMaterial() {
        try {
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
