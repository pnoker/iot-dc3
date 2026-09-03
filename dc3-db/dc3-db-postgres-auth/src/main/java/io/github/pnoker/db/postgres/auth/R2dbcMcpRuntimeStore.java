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
package io.github.pnoker.db.postgres.auth;

import io.github.pnoker.common.auth.repository.ReactiveOAuthMcpStore;

import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthConsentRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for mcp runtime records. */
@Repository
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcMcpRuntimeStore implements ReactiveOAuthMcpStore {
    private static final String AUTH = "dc3_auth.dc3_oauth_authorization";
    private static final String CLIENT = "dc3_auth.dc3_oauth_registered_client";
    private static final String CONNECTION = "dc3_auth.dc3_mcp_connection";
    private static final String TOOL = "dc3_auth.dc3_mcp_tool_catalog";
    private static final String CONFIRM = "dc3_auth.dc3_mcp_tool_confirmation";
    private static final String AUDIT = "dc3_auth.dc3_mcp_audit_log";

    private final DatabaseClient client;
    private final R2dbcDialect dialect;

    @Override
    public Mono<OAuthRegisteredClientRecord> getClient(String clientId) {
        return client.sql(
                        "SELECT id,client_id,client_name,client_type,owner_principal_id,service_account_principal_id,tenant_id,client_secret_hash,client_secret_expires_at,client_auth_methods,authorization_grant_types,redirect_uris,scopes,require_pkce,require_consent,enable_flag FROM "
                                + CLIENT + " WHERE client_id=:client_id AND deleted=0 LIMIT 1")
                .bind("client_id", clientId)
                .map(this::registeredClient)
                .one();
    }

    @Override
    public Mono<Integer> insertClient(OAuthRegisteredClientRecord value) {
        value.setId(UuidV7.nextLong());
        DatabaseClient.GenericExecuteSpec statement = client.sql("INSERT INTO " + CLIENT
                        + " (id,client_id,client_name,client_type,owner_principal_id,service_account_principal_id,tenant_id,client_secret_hash,client_secret_expires_at,client_auth_methods,authorization_grant_types,redirect_uris,scopes,require_pkce,require_consent,enable_flag,client_settings,token_settings,remark,creator_id,creator_name,operator_id,operator_name,deleted) VALUES (:id,:client_id,:client_name,:client_type,:owner_principal_id,:service_account_principal_id,:tenant_id,:client_secret_hash,:client_secret_expires_at,:client_auth_methods,:authorization_grant_types,:redirect_uris,:scopes,:require_pkce,:require_consent,:enable_flag,"
                        + dialect.jsonWriteExpression(":client_settings") + ","
                        + dialect.jsonWriteExpression(":token_settings")
                        + ",:remark,:creator_id,:creator_name,:operator_id,:operator_name,0)")
                .bind("id", value.getId())
                .bind("client_id", value.getClientId())
                .bind("client_name", value.getClientName())
                .bind("client_type", value.getClientType())
                .bind("owner_principal_id", value.getOwnerPrincipalId())
                .bind(
                        "service_account_principal_id",
                        value.getServiceAccountPrincipalId() == null ? 0L : value.getServiceAccountPrincipalId())
                .bind("tenant_id", value.getTenantId())
                .bind("client_secret_hash", value.getClientSecretHash())
                .bind("client_auth_methods", value.getClientAuthMethods())
                .bind("authorization_grant_types", value.getAuthorizationGrantTypes())
                .bind("redirect_uris", value.getRedirectUris())
                .bind("scopes", value.getScopes())
                .bind("require_pkce", value.getRequirePkce())
                .bind("require_consent", value.getRequireConsent())
                .bind("enable_flag", value.getEnableFlag())
                .bind("client_settings", "{}")
                .bind("token_settings", "{}")
                .bind("remark", "")
                .bind("creator_id", value.getOwnerPrincipalId())
                .bind("creator_name", "")
                .bind("operator_id", value.getOwnerPrincipalId())
                .bind("operator_name", "");
        statement = value.getClientSecretExpiresAt() == null
                ? statement.bindNull("client_secret_expires_at", java.time.Instant.class)
                : statement.bind(
                        "client_secret_expires_at", dialect.bindInstant(toInstant(value.getClientSecretExpiresAt())));
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Flux<OAuthRegisteredClientRecord> listClientsByOwner(Long ownerPrincipalId, Long tenantId) {
        return client.sql(
                        "SELECT id,client_id,client_name,client_type,owner_principal_id,service_account_principal_id,tenant_id,client_secret_hash,client_secret_expires_at,client_auth_methods,authorization_grant_types,redirect_uris,scopes,require_pkce,require_consent,enable_flag FROM "
                                + CLIENT
                                + " WHERE owner_principal_id=:owner_id AND tenant_id=:tenant_id AND deleted=0 ORDER BY create_time DESC,id DESC")
                .bind("owner_id", ownerPrincipalId)
                .bind("tenant_id", tenantId)
                .map(this::registeredClient)
                .all();
    }

    @Override
    public Mono<Integer> insertAuthorization(OAuthAuthorizationRecord value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        DatabaseClient.GenericExecuteSpec statement = client.sql("INSERT INTO " + AUTH
                        + " (id,registered_client_id,client_id,principal_id,principal_type,tenant_id,mcp_connection_id,authorization_grant_type,authorized_scopes,state_hash,authorization_code_hash,authorization_code_issued,authorization_code_expires,token_metadata,deleted) VALUES (:id,:registered_client_id,:client_id,:principal_id,:principal_type,:tenant_id,:mcp_connection_id,:grant_type,:scopes,:state_hash,:code_hash,:code_issued,:code_expires,"
                        + dialect.jsonWriteExpression(":token_metadata") + ",0)")
                .bind("id", value.getId())
                .bind("registered_client_id", value.getRegisteredClientId())
                .bind("client_id", value.getClientId())
                .bind("principal_id", value.getPrincipalId())
                .bind("principal_type", value.getPrincipalType())
                .bind("tenant_id", value.getTenantId())
                .bind("mcp_connection_id", value.getMcpConnectionId())
                .bind("grant_type", value.getAuthorizationGrantType())
                .bind("scopes", value.getAuthorizedScopes())
                .bind("state_hash", value.getStateHash())
                .bind("code_hash", value.getAuthorizationCodeHash())
                .bind("code_issued", dialect.bindInstant(toInstant(value.getAuthorizationCodeIssued())))
                .bind("code_expires", dialect.bindInstant(toInstant(value.getAuthorizationCodeExpires())))
                .bind("token_metadata", value.getTokenMetadata() == null ? "{}" : value.getTokenMetadata());
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Mono<OAuthConsentRecord> getConsent(Long registeredClientId, Long principalId, Long tenantId) {
        return client.sql(
                        "SELECT id,registered_client_id,client_id,principal_id,tenant_id,scopes,consent_ext FROM dc3_auth.dc3_oauth_authorization_consent WHERE registered_client_id=:registered_client_id AND principal_id=:principal_id AND tenant_id=:tenant_id AND deleted=0 LIMIT 1")
                .bind("registered_client_id", registeredClientId)
                .bind("principal_id", principalId)
                .bind("tenant_id", tenantId)
                .map(this::consent)
                .one();
    }

    @Override
    public Mono<Integer> upsertConsent(OAuthConsentRecord value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        String base =
                "INSERT INTO dc3_auth.dc3_oauth_authorization_consent (id,registered_client_id,client_id,principal_id,tenant_id,scopes,consent_ext,deleted) VALUES (:id,:registered_client_id,:client_id,:principal_id,:tenant_id,:scopes,"
                        + dialect.jsonWriteExpression(":consent_ext") + ",0) ";
        String sql = "postgres".equalsIgnoreCase(dialect.name())
                ? base
                        + "ON CONFLICT (registered_client_id,principal_id,tenant_id) WHERE deleted=0 DO UPDATE SET client_id=EXCLUDED.client_id,scopes=EXCLUDED.scopes,consent_ext=EXCLUDED.consent_ext,deleted=0,operate_time=CURRENT_TIMESTAMP"
                : base
                        + "ON DUPLICATE KEY UPDATE client_id=VALUES(client_id),scopes=VALUES(scopes),consent_ext=VALUES(consent_ext),deleted=0,operate_time=CURRENT_TIMESTAMP";
        return client.sql(sql)
                .bind("id", value.getId())
                .bind("registered_client_id", value.getRegisteredClientId())
                .bind("client_id", value.getClientId())
                .bind("principal_id", value.getPrincipalId())
                .bind("tenant_id", value.getTenantId())
                .bind("scopes", value.getScopes() == null ? "" : value.getScopes())
                .bind("consent_ext", value.getConsentExt() == null ? "{}" : value.getConsentExt())
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> activateAuthorizationTokens(
            Long id,
            String codeHash,
            String accessTokenJti,
            LocalDateTime accessIssued,
            LocalDateTime accessExpires,
            String refreshHash,
            String previousRefreshHash,
            LocalDateTime refreshIssued,
            LocalDateTime refreshExpires,
            String tokenClaims) {
        DatabaseClient.GenericExecuteSpec statement = client.sql("UPDATE " + AUTH
                        + " SET authorization_code_hash=:code_hash,access_token_jti=:access_jti,access_token_issued=:access_issued,access_token_expires=:access_expires,refresh_token_hash=:refresh_hash,previous_refresh_token_hash=:previous_refresh_hash,refresh_token_issued=:refresh_issued,refresh_token_expires=:refresh_expires,token_claims="
                        + dialect.jsonWriteExpression(":token_claims")
                        + ",operate_time=CURRENT_TIMESTAMP WHERE id=:id AND deleted=0")
                .bind("code_hash", codeHash == null ? "" : codeHash)
                .bind("access_jti", accessTokenJti)
                .bind("access_issued", dialect.bindInstant(toInstant(accessIssued)))
                .bind("access_expires", dialect.bindInstant(toInstant(accessExpires)))
                .bind("refresh_hash", refreshHash == null ? "" : refreshHash)
                .bind("token_claims", tokenClaims == null ? "{}" : tokenClaims)
                .bind("id", id);
        statement = refreshIssued == null
                ? statement.bindNull("refresh_issued", java.time.Instant.class)
                : statement.bind("refresh_issued", dialect.bindInstant(toInstant(refreshIssued)));
        statement = refreshExpires == null
                ? statement.bindNull("refresh_expires", java.time.Instant.class)
                : statement.bind("refresh_expires", dialect.bindInstant(toInstant(refreshExpires)));
        statement = previousRefreshHash == null
                ? statement.bindNull("previous_refresh_hash", String.class)
                : statement.bind("previous_refresh_hash", previousRefreshHash);
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Mono<Integer> activateAuthorizationCode(
            String expectedCodeHash,
            Long id,
            String accessTokenJti,
            LocalDateTime accessIssued,
            LocalDateTime accessExpires,
            String refreshHash,
            String previousRefreshHash,
            LocalDateTime refreshIssued,
            LocalDateTime refreshExpires,
            String tokenClaims) {
        DatabaseClient.GenericExecuteSpec statement = client.sql(
                        "UPDATE " + AUTH
                                + " SET authorization_code_hash='',access_token_jti=:access_jti,access_token_issued=:access_issued,access_token_expires=:access_expires,refresh_token_hash=:refresh_hash,previous_refresh_token_hash=:previous_refresh_hash,refresh_token_issued=:refresh_issued,refresh_token_expires=:refresh_expires,token_claims="
                                + dialect.jsonWriteExpression(":token_claims")
                                + ",operate_time=CURRENT_TIMESTAMP WHERE id=:id AND authorization_code_hash=:expected_code_hash AND authorization_code_expires>CURRENT_TIMESTAMP AND deleted=0")
                .bind("access_jti", accessTokenJti)
                .bind("access_issued", dialect.bindInstant(toInstant(accessIssued)))
                .bind("access_expires", dialect.bindInstant(toInstant(accessExpires)))
                .bind("refresh_hash", refreshHash == null ? "" : refreshHash)
                .bind("token_claims", tokenClaims == null ? "{}" : tokenClaims)
                .bind("id", id)
                .bind("expected_code_hash", expectedCodeHash);
        statement = refreshIssued == null
                ? statement.bindNull("refresh_issued", java.time.Instant.class)
                : statement.bind("refresh_issued", dialect.bindInstant(toInstant(refreshIssued)));
        statement = refreshExpires == null
                ? statement.bindNull("refresh_expires", java.time.Instant.class)
                : statement.bind("refresh_expires", dialect.bindInstant(toInstant(refreshExpires)));
        statement = previousRefreshHash == null
                ? statement.bindNull("previous_refresh_token_hash", String.class)
                : statement.bind("previous_refresh_token_hash", previousRefreshHash);
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Mono<Integer> rotateAuthorizationRefreshToken(
            Long id,
            String expectedRefreshHash,
            String accessTokenJti,
            LocalDateTime accessIssued,
            LocalDateTime accessExpires,
            String refreshHash,
            LocalDateTime refreshIssued,
            LocalDateTime refreshExpires,
            String tokenClaims) {
        DatabaseClient.GenericExecuteSpec statement = client.sql(
                        "UPDATE " + AUTH
                                + " SET access_token_jti=:access_jti,access_token_issued=:access_issued,access_token_expires=:access_expires,previous_refresh_token_hash=:previous_refresh_hash,refresh_token_hash=:refresh_hash,refresh_token_issued=:refresh_issued,refresh_token_expires=:refresh_expires,token_claims="
                                + dialect.jsonWriteExpression(":token_claims")
                                + ",operate_time=CURRENT_TIMESTAMP WHERE id=:id AND refresh_token_hash=:expected_refresh_hash AND refresh_token_expires>CURRENT_TIMESTAMP AND revoked_time IS NULL AND deleted=0")
                .bind("access_jti", accessTokenJti)
                .bind("access_issued", dialect.bindInstant(toInstant(accessIssued)))
                .bind("access_expires", dialect.bindInstant(toInstant(accessExpires)))
                .bind("previous_refresh_hash", expectedRefreshHash)
                .bind("refresh_hash", refreshHash == null ? "" : refreshHash)
                .bind("token_claims", tokenClaims == null ? "{}" : tokenClaims)
                .bind("id", id)
                .bind("expected_refresh_hash", expectedRefreshHash);
        statement = refreshIssued == null
                ? statement.bindNull("refresh_issued", java.time.Instant.class)
                : statement.bind("refresh_issued", dialect.bindInstant(toInstant(refreshIssued)));
        statement = refreshExpires == null
                ? statement.bindNull("refresh_expires", java.time.Instant.class)
                : statement.bind("refresh_expires", dialect.bindInstant(toInstant(refreshExpires)));
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Mono<Integer> revokeAuthorizationByAccessTokenJti(String jti, String reason, LocalDateTime revokedTime) {
        return revokeAuthorization("access_token_jti", jti, reason, revokedTime);
    }

    @Override
    public Mono<Integer> revokeAuthorizationByRefreshTokenHash(
            String refreshHash, String reason, LocalDateTime revokedTime) {
        return revokeAuthorization("refresh_token_hash", refreshHash, reason, revokedTime);
    }

    private Mono<Integer> revokeAuthorization(String column, String token, String reason, LocalDateTime revokedTime) {
        if (!java.util.Set.of("access_token_jti", "refresh_token_hash").contains(column))
            return Mono.error(new IllegalArgumentException("unsupported revoke key"));
        return client.sql("UPDATE " + AUTH
                        + " SET revoked_time=:revoked_time,revoke_reason=:reason,operate_time=CURRENT_TIMESTAMP WHERE "
                        + column + "=:token AND deleted=0")
                .bind("revoked_time", dialect.bindInstant(toInstant(revokedTime)))
                .bind("reason", reason)
                .bind("token", token)
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<OAuthAuthorizationRecord> getAuthorizationByCodeHash(String codeHash) {
        return authorizationBy("authorization_code_hash", codeHash);
    }

    @Override
    public Mono<OAuthAuthorizationRecord> getAuthorizationByRefreshTokenHash(String refreshHash) {
        return authorizationBy("refresh_token_hash", refreshHash);
    }

    @Override
    public Mono<OAuthAuthorizationRecord> getAuthorizationByPreviousRefreshTokenHash(String previousRefreshHash) {
        return authorizationBy("previous_refresh_token_hash", previousRefreshHash);
    }

    private Mono<OAuthAuthorizationRecord> authorizationBy(String column, String value) {
        if (!java.util.Set.of("authorization_code_hash", "refresh_token_hash", "previous_refresh_token_hash")
                .contains(column)) {
            return Mono.error(new IllegalArgumentException("unsupported authorization lookup"));
        }
        return client.sql(
                        "SELECT id,registered_client_id,client_id,principal_id,principal_type,tenant_id,mcp_connection_id,authorization_grant_type,authorized_scopes,state_hash,authorization_code_hash,authorization_code_issued,authorization_code_expires,access_token_jti,access_token_issued,access_token_expires,refresh_token_hash,previous_refresh_token_hash,refresh_token_issued,refresh_token_expires,token_metadata,revoked_time,revoke_reason FROM "
                                + AUTH + " WHERE " + column + "=:lookup AND deleted=0 LIMIT 1")
                .bind("lookup", value)
                .map(this::authorization)
                .one();
    }

    @Override
    public Mono<OAuthAuthorizationRecord> getAuthorizationByAccessTokenJti(String jti) {
        return client.sql(
                        "SELECT id,registered_client_id,client_id,principal_id,principal_type,tenant_id,mcp_connection_id,authorization_grant_type,authorized_scopes,state_hash,authorization_code_hash,authorization_code_issued,authorization_code_expires,access_token_jti,access_token_issued,access_token_expires,refresh_token_hash,previous_refresh_token_hash,refresh_token_issued,refresh_token_expires,token_metadata,revoked_time,revoke_reason FROM "
                                + AUTH + " WHERE access_token_jti=:jti AND deleted=0 LIMIT 1")
                .bind("jti", jti)
                .map(this::authorization)
                .one();
    }

    @Override
    public Mono<McpConnectionRecord> getConnection(Long id) {
        return client.sql(
                        "SELECT id,connection_name,client_id,principal_id,principal_type,tenant_id,grant_type,enable_flag,expire_time,revoke_time,last_used_time,remark,creator_id,creator_name FROM "
                                + CONNECTION + " WHERE id=:id AND deleted=0 LIMIT 1")
                .bind("id", id)
                .map(this::connection)
                .one();
    }

    @Override
    public Mono<McpConnectionRecord> getActiveConnection(
            String clientId, Long principalId, Long tenantId, String grantType) {
        return client.sql(
                        "SELECT id,connection_name,client_id,principal_id,principal_type,tenant_id,grant_type,enable_flag,expire_time,revoke_time,last_used_time,remark,creator_id,creator_name FROM "
                                + CONNECTION
                                + " WHERE client_id=:client_id AND principal_id=:principal_id AND tenant_id=:tenant_id AND grant_type=:grant_type AND enable_flag=0 AND revoke_time IS NULL AND (expire_time IS NULL OR expire_time>CURRENT_TIMESTAMP) AND deleted=0 ORDER BY id LIMIT 1")
                .bind("client_id", clientId)
                .bind("principal_id", principalId)
                .bind("tenant_id", tenantId)
                .bind("grant_type", grantType)
                .map(this::connection)
                .one();
    }

    @Override
    public Flux<McpConnectionRecord> listConnections(Long tenantId, Long principalId) {
        return client.sql(
                        "SELECT id,connection_name,client_id,principal_id,principal_type,tenant_id,grant_type,enable_flag,expire_time,revoke_time,last_used_time,remark,creator_id,creator_name FROM "
                                + CONNECTION
                                + " WHERE tenant_id=:tenant_id AND principal_id=:principal_id AND deleted=0 ORDER BY create_time DESC,id DESC")
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .map(this::connection)
                .all();
    }

    @Override
    public Mono<Integer> insertConnection(McpConnectionRecord value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        DatabaseClient.GenericExecuteSpec statement = client.sql(
                        "INSERT INTO " + CONNECTION
                                + " (id,connection_name,client_id,principal_id,principal_type,tenant_id,grant_type,enable_flag,expire_time,revoke_time,last_used_time,remark,creator_id,creator_name,deleted) VALUES (:id,:connection_name,:client_id,:principal_id,:principal_type,:tenant_id,:grant_type,:enable_flag,:expire_time,NULL,NULL,:remark,:creator_id,:creator_name,0)")
                .bind("id", value.getId())
                .bind("connection_name", value.getConnectionName())
                .bind("client_id", value.getClientId())
                .bind("principal_id", value.getPrincipalId())
                .bind("principal_type", value.getPrincipalType())
                .bind("tenant_id", value.getTenantId())
                .bind("grant_type", value.getGrantType())
                .bind("enable_flag", value.getEnableFlag() == null ? 0 : value.getEnableFlag())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("creator_id", value.getCreatorId())
                .bind("creator_name", value.getCreatorName() == null ? "" : value.getCreatorName());
        statement = value.getExpireTime() == null
                ? statement.bindNull("expire_time", java.time.Instant.class)
                : statement.bind("expire_time", dialect.bindInstant(toInstant(value.getExpireTime())));
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Mono<Integer> revokeConnection(Long id, Long tenantId, Long principalId, LocalDateTime revokeTime) {
        return client.sql(
                        "UPDATE " + CONNECTION
                                + " SET revoke_time=:revoke_time,enable_flag=1,operate_time=CURRENT_TIMESTAMP WHERE id=:id AND tenant_id=:tenant_id AND principal_id=:principal_id AND deleted=0")
                .bind("revoke_time", dialect.bindInstant(toInstant(revokeTime)))
                .bind("id", id)
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Flux<McpToolRecord> listTools(Long tenantId, Long principalId, Long connectionId, boolean allowHighRisk) {
        return visibleTools(null, tenantId, principalId, connectionId, allowHighRisk);
    }

    @Override
    public Mono<McpToolRecord> resolveTool(
            Long tenantId, Long principalId, Long connectionId, String toolName, boolean allowHighRisk) {
        // Keep the name predicate in SQL: a large catalog must not be materialised merely
        // to resolve one tool, and the predicate remains tenant/permission scoped.
        return visibleTools(toolName, tenantId, principalId, connectionId, allowHighRisk)
                .next();
    }

    private Flux<McpToolRecord> visibleTools(
            String toolName, Long tenantId, Long principalId, Long connectionId, boolean allowHighRisk) {
        StringBuilder sql = new StringBuilder("SELECT tool.* FROM ")
                .append(CONNECTION)
                .append(
                        " c JOIN dc3_auth.dc3_mcp_connection_tool w ON w.connection_id=c.id AND w.enable_flag=0 AND w.deleted=0 JOIN ")
                .append(TOOL)
                .append(" tool ON tool.tool_id=w.tool_id AND tool.enable_flag=0 AND tool.deleted=0")
                .append(" WHERE c.id=:connection_id AND c.tenant_id=:tenant_id AND c.principal_id=:principal_id")
                .append(
                        " AND c.enable_flag=0 AND c.revoke_time IS NULL AND (c.expire_time IS NULL OR c.expire_time>CURRENT_TIMESTAMP)")
                .append(" AND c.deleted=0 AND (:allow_high_risk OR tool.risk_level<>'HIGH')");
        if (toolName != null) {
            sql.append(" AND tool.tool_name=:tool_name");
        }
        sql.append(" AND (tool.permission_code='' OR EXISTS (SELECT 1 FROM dc3_auth.dc3_role_principal_bind rp")
                .append(" JOIN dc3_auth.dc3_role_resource_bind rr ON rr.role_id=rp.role_id AND rr.deleted=0")
                .append(
                        " JOIN dc3_auth.dc3_resource res ON res.id=rr.resource_id AND res.enable_flag=0 AND res.deleted=0")
                .append(" WHERE rp.tenant_id=c.tenant_id AND rp.principal_id=c.principal_id AND rp.deleted=0")
                .append(" AND (res.resource_code='*' OR res.resource_code=tool.permission_code)))")
                .append(" ORDER BY tool.tool_category,tool.tool_name");
        DatabaseClient.GenericExecuteSpec query = client.sql(sql.toString())
                .bind("connection_id", connectionId)
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .bind("allow_high_risk", allowHighRisk);
        if (toolName != null) {
            query = query.bind("tool_name", toolName);
        }
        return query.map(this::tool).all();
    }

    @Override
    public Mono<Integer> deleteConnectionTools(Long connectionId) {
        return client.sql(
                        "UPDATE dc3_auth.dc3_mcp_connection_tool SET deleted=1,operate_time=CURRENT_TIMESTAMP WHERE connection_id=:connection_id AND deleted=0")
                .bind("connection_id", connectionId)
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Flux<String> listConnectionToolIds(Long connectionId, Long tenantId, Long principalId) {
        return client.sql(
                        "SELECT w.tool_id FROM " + CONNECTION
                                + " c JOIN dc3_auth.dc3_mcp_connection_tool w ON w.connection_id=c.id AND w.enable_flag=0 AND w.deleted=0 WHERE c.id=:connection_id AND c.tenant_id=:tenant_id AND c.principal_id=:principal_id AND c.deleted=0 ORDER BY w.tool_id")
                .bind("connection_id", connectionId)
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .map((row, metadata) -> row.get("tool_id", String.class))
                .all();
    }

    @Override
    public Mono<Integer> insertConnectionTool(
            Long id, Long connectionId, String toolId, Long operatorId, String operatorName) {
        return client.sql(
                        "INSERT INTO dc3_auth.dc3_mcp_connection_tool (id,connection_id,tool_id,enable_flag,remark,creator_id,creator_name,operator_id,operator_name,deleted) VALUES (:id,:connection_id,:tool_id,0,'',:operator_id,:operator_name,:operator_id,:operator_name,0)")
                .bind("id", id == null ? UuidV7.nextLong() : id)
                .bind("connection_id", connectionId)
                .bind("tool_id", toolId)
                .bind("operator_id", operatorId)
                .bind("operator_name", operatorName == null ? "" : operatorName)
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Flux<McpToolRecord> listRegistryToolCandidates() {
        String apiPath = dialect.jsonTextExpression("api.api_ext", "content.url");
        return client.sql(
                        "SELECT NULL AS id,api.api_code AS tool_id,lower(CONCAT(api.service_name, '_', api.api_name)) AS tool_name,COALESCE(NULLIF(api.api_name,''),api.api_code) AS tool_title,api.api_group AS tool_category,api.service_name,api.api_code,resource.resource_code AS permission_code,CASE api.api_type_flag WHEN 0 THEN 'POST' WHEN 1 THEN 'DELETE' WHEN 2 THEN 'PUT' WHEN 4 THEN 'PATCH' ELSE 'GET' END AS http_method,COALESCE(NULLIF("
                                + apiPath
                                + ",''),'/') AS api_path,'' AS schema_hash,'LOW' AS risk_level,0 AS read_only_hint,0 AS destructive_hint,0 AS idempotent_hint,0 AS open_world_hint,0 AS enable_flag,COALESCE(NULLIF(api.remark,''),api.api_group) AS remark,'{}' AS tool_ext FROM dc3_api api JOIN dc3_auth.dc3_resource resource ON resource.resource_code=CONCAT(api.service_name, ':', api.api_name) AND resource.service_name=api.service_name AND resource.resource_type_flag=6 AND resource.enable_flag=0 AND resource.deleted=0 WHERE api.enable_flag=0 AND api.deleted=0")
                .map(this::tool)
                .all();
    }

    @Override
    public Flux<McpToolRecord> listEnabledToolsByPermissionCodes(java.util.Collection<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) return Flux.empty();
        String placeholders = java.util.stream.IntStream.range(0, permissionCodes.size())
                .mapToObj(i -> ":permission_" + i)
                .collect(java.util.stream.Collectors.joining(","));
        DatabaseClient.GenericExecuteSpec query = client.sql(
                "SELECT id,tool_id,tool_name,tool_title,tool_category,service_name,api_code,permission_code,http_method,api_path,schema_hash,risk_level,read_only_hint,destructive_hint,idempotent_hint,open_world_hint,enable_flag,remark,tool_ext FROM "
                        + TOOL + " WHERE permission_code IN (" + placeholders
                        + ") AND enable_flag=0 AND deleted=0 ORDER BY tool_id");
        int index = 0;
        for (String code : permissionCodes) query = query.bind("permission_" + index++, code);
        return query.map(this::tool).all();
    }

    @Override
    public Mono<McpToolRecord> getTool(String toolId) {
        return client.sql(
                        "SELECT id,tool_id,tool_name,tool_title,tool_category,service_name,api_code,permission_code,http_method,api_path,schema_hash,risk_level,read_only_hint,destructive_hint,idempotent_hint,open_world_hint,enable_flag,remark,tool_ext FROM "
                                + TOOL + " WHERE tool_id=:tool_id AND deleted=0 LIMIT 1")
                .bind("tool_id", toolId)
                .map(this::tool)
                .one();
    }

    @Override
    public Mono<Integer> upsertTool(McpToolRecord value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        String columns =
                " (id,tool_id,tool_name,tool_title,tool_category,service_name,api_code,permission_code,http_method,api_path,schema_hash,risk_level,read_only_hint,destructive_hint,idempotent_hint,open_world_hint,enable_flag,remark,tool_ext,deleted) ";
        String values =
                " VALUES (:id,:tool_id,:tool_name,:tool_title,:tool_category,:service_name,:api_code,:permission_code,:http_method,:api_path,:schema_hash,:risk_level,:read_only_hint,:destructive_hint,:idempotent_hint,:open_world_hint,:enable_flag,:remark,"
                        + dialect.jsonWriteExpression(":tool_ext") + ",0) ";
        String sql;
        if ("postgres".equalsIgnoreCase(dialect.name())) {
            sql = "INSERT INTO " + TOOL + columns + values
                    + "ON CONFLICT (tool_id) WHERE deleted=0 AND tool_id<>'' DO UPDATE SET "
                    + "tool_name=EXCLUDED.tool_name,tool_title=EXCLUDED.tool_title,tool_category=EXCLUDED.tool_category,"
                    + "service_name=EXCLUDED.service_name,api_code=EXCLUDED.api_code,permission_code=EXCLUDED.permission_code,"
                    + "http_method=EXCLUDED.http_method,api_path=EXCLUDED.api_path,schema_hash=EXCLUDED.schema_hash,"
                    + "risk_level=EXCLUDED.risk_level,read_only_hint=EXCLUDED.read_only_hint,destructive_hint=EXCLUDED.destructive_hint,"
                    + "idempotent_hint=EXCLUDED.idempotent_hint,open_world_hint=EXCLUDED.open_world_hint,enable_flag=EXCLUDED.enable_flag,"
                    + "remark=EXCLUDED.remark,tool_ext=EXCLUDED.tool_ext,deleted=0,operate_time=CURRENT_TIMESTAMP";
        } else {
            sql = "INSERT INTO " + TOOL + columns + values
                    + "ON DUPLICATE KEY UPDATE tool_name=VALUES(tool_name),tool_title=VALUES(tool_title),tool_category=VALUES(tool_category),"
                    + "service_name=VALUES(service_name),api_code=VALUES(api_code),permission_code=VALUES(permission_code),"
                    + "http_method=VALUES(http_method),api_path=VALUES(api_path),schema_hash=VALUES(schema_hash),risk_level=VALUES(risk_level),"
                    + "read_only_hint=VALUES(read_only_hint),destructive_hint=VALUES(destructive_hint),idempotent_hint=VALUES(idempotent_hint),"
                    + "open_world_hint=VALUES(open_world_hint),enable_flag=VALUES(enable_flag),remark=VALUES(remark),tool_ext=VALUES(tool_ext),deleted=0,operate_time=CURRENT_TIMESTAMP";
        }
        return toolMutationStatement(sql, value).fetch().rowsUpdated().map(Long::intValue);
    }

    @Override
    public Mono<Integer> insertTool(McpToolRecord value) {
        value.setId(value.getId() == null ? UuidV7.nextLong() : value.getId());
        return client.sql("INSERT INTO " + TOOL
                        + " (id,tool_id,tool_name,tool_title,tool_category,service_name,api_code,permission_code,http_method,api_path,schema_hash,risk_level,read_only_hint,destructive_hint,idempotent_hint,open_world_hint,enable_flag,remark,tool_ext,deleted) VALUES (:id,:tool_id,:tool_name,:tool_title,:tool_category,:service_name,:api_code,:permission_code,:http_method,:api_path,:schema_hash,:risk_level,:read_only_hint,:destructive_hint,:idempotent_hint,:open_world_hint,:enable_flag,:remark,"
                        + dialect.jsonWriteExpression(":tool_ext") + ",0)")
                .bind("id", value.getId())
                .bind("tool_id", value.getToolId())
                .bind("tool_name", value.getToolName())
                .bind("tool_title", value.getToolTitle())
                .bind("tool_category", value.getToolCategory())
                .bind("service_name", value.getServiceName())
                .bind("api_code", value.getApiCode())
                .bind("permission_code", value.getPermissionCode())
                .bind("http_method", value.getHttpMethod())
                .bind("api_path", value.getApiPath())
                .bind("schema_hash", value.getSchemaHash())
                .bind("risk_level", value.getRiskLevel())
                .bind("read_only_hint", value.getReadOnlyHint() == null ? 0 : value.getReadOnlyHint())
                .bind("destructive_hint", value.getDestructiveHint() == null ? 0 : value.getDestructiveHint())
                .bind("idempotent_hint", value.getIdempotentHint() == null ? 0 : value.getIdempotentHint())
                .bind("open_world_hint", value.getOpenWorldHint() == null ? 0 : value.getOpenWorldHint())
                .bind("enable_flag", value.getEnableFlag() == null ? 0 : value.getEnableFlag())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("tool_ext", value.getToolExt() == null ? "{}" : value.getToolExt())
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> updateTool(McpToolRecord value) {
        return client.sql("UPDATE " + TOOL
                        + " SET tool_name=:tool_name,tool_title=:tool_title,tool_category=:tool_category,service_name=:service_name,api_code=:api_code,permission_code=:permission_code,http_method=:http_method,api_path=:api_path,schema_hash=:schema_hash,risk_level=:risk_level,read_only_hint=:read_only_hint,destructive_hint=:destructive_hint,idempotent_hint=:idempotent_hint,open_world_hint=:open_world_hint,enable_flag=:enable_flag,remark=:remark,tool_ext="
                        + dialect.jsonWriteExpression(":tool_ext")
                        + ",operate_time=CURRENT_TIMESTAMP WHERE id=:id AND deleted=0")
                .bind("id", value.getId())
                .bind("tool_name", value.getToolName())
                .bind("tool_title", value.getToolTitle())
                .bind("tool_category", value.getToolCategory())
                .bind("service_name", value.getServiceName())
                .bind("api_code", value.getApiCode())
                .bind("permission_code", value.getPermissionCode())
                .bind("http_method", value.getHttpMethod())
                .bind("api_path", value.getApiPath())
                .bind("schema_hash", value.getSchemaHash())
                .bind("risk_level", value.getRiskLevel())
                .bind("read_only_hint", value.getReadOnlyHint())
                .bind("destructive_hint", value.getDestructiveHint())
                .bind("idempotent_hint", value.getIdempotentHint())
                .bind("open_world_hint", value.getOpenWorldHint())
                .bind("enable_flag", value.getEnableFlag())
                .bind("remark", value.getRemark())
                .bind("tool_ext", value.getToolExt() == null ? "{}" : value.getToolExt())
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    private DatabaseClient.GenericExecuteSpec toolMutationStatement(String sql, McpToolRecord value) {
        return client.sql(sql)
                .bind("id", value.getId())
                .bind("tool_id", value.getToolId())
                .bind("tool_name", value.getToolName())
                .bind("tool_title", value.getToolTitle())
                .bind("tool_category", value.getToolCategory())
                .bind("service_name", value.getServiceName())
                .bind("api_code", value.getApiCode())
                .bind("permission_code", value.getPermissionCode())
                .bind("http_method", value.getHttpMethod())
                .bind("api_path", value.getApiPath())
                .bind("schema_hash", value.getSchemaHash())
                .bind("risk_level", value.getRiskLevel())
                .bind("read_only_hint", value.getReadOnlyHint() == null ? 0 : value.getReadOnlyHint())
                .bind("destructive_hint", value.getDestructiveHint() == null ? 0 : value.getDestructiveHint())
                .bind("idempotent_hint", value.getIdempotentHint() == null ? 0 : value.getIdempotentHint())
                .bind("open_world_hint", value.getOpenWorldHint() == null ? 0 : value.getOpenWorldHint())
                .bind("enable_flag", value.getEnableFlag() == null ? 0 : value.getEnableFlag())
                .bind("remark", value.getRemark() == null ? "" : value.getRemark())
                .bind("tool_ext", value.getToolExt() == null ? "{}" : value.getToolExt());
    }

    @Override
    public Mono<Boolean> touchConnection(Long id, LocalDateTime usedAt) {
        return client.sql("UPDATE " + CONNECTION
                        + " SET last_used_time=:used_at,operate_time=:used_at WHERE id=:id AND deleted=0")
                .bind("id", id)
                .bind("used_at", dialect.bindInstant(toInstant(usedAt)))
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    @Override
    public Mono<McpToolConfirmationRecord> getConsumedByIdempotency(Long connectionId, String key) {
        return client.sql(
                        "SELECT id,confirm_id,tenant_id,principal_id,connection_id,tool_id,argument_digest,idempotency_key,risk_level,status,expire_time,consumed_time,create_time FROM "
                                + CONFIRM
                                + " WHERE connection_id=:connection_id AND idempotency_key=:key AND status='CONSUMED' AND deleted=0 LIMIT 1")
                .bind("connection_id", connectionId)
                .bind("key", key)
                .map(this::confirmation)
                .one();
    }

    @Override
    public Mono<McpToolConfirmationRecord> getByIdempotency(Long connectionId, String key) {
        return client.sql(
                        "SELECT id,confirm_id,tenant_id,principal_id,connection_id,tool_id,argument_digest,idempotency_key,risk_level,status,expire_time,consumed_time,create_time FROM "
                                + CONFIRM
                                + " WHERE connection_id=:connection_id AND idempotency_key=:key AND deleted=0 LIMIT 1")
                .bind("connection_id", connectionId)
                .bind("key", key)
                .map(this::confirmation)
                .one();
    }

    @Override
    public Mono<McpToolConfirmationRecord> getConfirmation(String confirmId) {
        return client.sql(
                        "SELECT id,confirm_id,tenant_id,principal_id,connection_id,tool_id,argument_digest,idempotency_key,risk_level,status,expire_time,consumed_time,create_time FROM "
                                + CONFIRM + " WHERE confirm_id=:confirm_id AND deleted=0 LIMIT 1")
                .bind("confirm_id", confirmId)
                .map(this::confirmation)
                .one();
    }

    @Override
    public Mono<Integer> insertConfirmation(McpToolConfirmationRecord value) {
        value.setId(UuidV7.nextLong());
        return client.sql(
                        "INSERT INTO " + CONFIRM
                                + " (id,confirm_id,tenant_id,principal_id,connection_id,tool_id,argument_digest,idempotency_key,risk_level,status,expire_time,create_time,deleted) VALUES (:id,:confirm_id,:tenant_id,:principal_id,:connection_id,:tool_id,:argument_digest,:idempotency_key,:risk_level,:status,:expire_time,:create_time,0)")
                .bind("id", value.getId())
                .bind("confirm_id", value.getConfirmId())
                .bind("tenant_id", value.getTenantId())
                .bind("principal_id", value.getPrincipalId())
                .bind("connection_id", value.getConnectionId())
                .bind("tool_id", value.getToolId())
                .bind("argument_digest", value.getArgumentDigest())
                .bind("idempotency_key", value.getIdempotencyKey())
                .bind("risk_level", value.getRiskLevel())
                .bind("status", value.getStatus())
                .bind("expire_time", dialect.bindInstant(toInstant(value.getExpireTime())))
                .bind("create_time", dialect.bindInstant(Instant.now()))
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> consumeConfirmation(Long id, LocalDateTime consumedAt) {
        return client.sql(
                        "UPDATE " + CONFIRM
                                + " SET status='CONSUMED',consumed_time=:consumed_at WHERE id=:id AND status='PENDING' AND expire_time>CURRENT_TIMESTAMP AND deleted=0")
                .bind("id", id)
                .bind("consumed_at", dialect.bindInstant(toInstant(consumedAt)))
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> consumeConfirmation(
            String confirmId,
            Long tenantId,
            Long principalId,
            Long connectionId,
            String toolId,
            String argumentDigest,
            LocalDateTime consumedAt) {
        return client.sql(
                        "UPDATE " + CONFIRM
                                + " SET status='CONSUMED',consumed_time=:consumed_at WHERE confirm_id=:confirm_id AND tenant_id=:tenant_id AND principal_id=:principal_id AND connection_id=:connection_id AND tool_id=:tool_id AND argument_digest=:argument_digest AND status='PENDING' AND expire_time>CURRENT_TIMESTAMP AND deleted=0")
                .bind("confirm_id", confirmId)
                .bind("tenant_id", tenantId)
                .bind("principal_id", principalId)
                .bind("connection_id", connectionId)
                .bind("tool_id", toolId)
                .bind("argument_digest", argumentDigest)
                .bind("consumed_at", dialect.bindInstant(toInstant(consumedAt)))
                .fetch()
                .rowsUpdated()
                .map(Long::intValue);
    }

    @Override
    public Mono<Integer> insertAudit(McpAuditCommand value) {
        value.setId(UuidV7.nextLong());
        DatabaseClient.GenericExecuteSpec statement = client.sql("INSERT INTO " + AUDIT
                        + " (id,trace_id,tenant_id,principal_id,principal_type,client_id,connection_id,tool_id,tool_name,permission_code,risk_level,confirm_id,idempotency_key,argument_digest,status,error_code,duration_ms,client_name,client_version,remote_ip,audit_ext) VALUES (:id,:trace_id,:tenant_id,:principal_id,:principal_type,:client_id,:connection_id,:tool_id,:tool_name,:permission_code,:risk_level,:confirm_id,:idempotency_key,:argument_digest,:status,:error_code,:duration_ms,:client_name,:client_version,:remote_ip,"
                        + dialect.jsonWriteExpression(":audit_ext") + ")")
                .bind("id", value.getId())
                .bind("audit_ext", "{}");
        statement = bindNullable(statement, "trace_id", value.getTraceId(), String.class);
        statement = bindNullable(statement, "tenant_id", value.getTenantId(), Long.class);
        statement = bindNullable(statement, "principal_id", value.getPrincipalId(), Long.class);
        statement = bindNullable(statement, "principal_type", value.getPrincipalType(), String.class);
        statement = bindNullable(statement, "client_id", value.getClientId(), String.class);
        statement = bindNullable(statement, "connection_id", value.getConnectionId(), Long.class);
        statement = bindNullable(statement, "tool_id", value.getToolId(), String.class);
        statement = bindNullable(statement, "tool_name", value.getToolName(), String.class);
        statement = bindNullable(statement, "permission_code", value.getPermissionCode(), String.class);
        statement = bindNullable(statement, "risk_level", value.getRiskLevel(), String.class);
        statement = bindNullable(statement, "confirm_id", value.getConfirmId(), String.class);
        statement = bindNullable(statement, "idempotency_key", value.getIdempotencyKey(), String.class);
        statement = bindNullable(statement, "argument_digest", value.getArgumentDigest(), String.class);
        statement = bindNullable(statement, "status", value.getStatus(), String.class);
        statement = bindNullable(statement, "error_code", value.getErrorCode(), String.class);
        statement = bindNullable(statement, "duration_ms", value.getDurationMs(), Long.class);
        statement = bindNullable(statement, "client_name", value.getClientName(), String.class);
        statement = bindNullable(statement, "client_version", value.getClientVersion(), String.class);
        statement = bindNullable(statement, "remote_ip", value.getRemoteIp(), String.class);
        return statement.fetch().rowsUpdated().map(Long::intValue);
    }

    private DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec statement, String name, Object value, Class<?> type) {
        return value == null ? statement.bindNull(name, type) : statement.bind(name, value);
    }

    private OAuthAuthorizationRecord authorization(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        OAuthAuthorizationRecord value = new OAuthAuthorizationRecord();
        value.setId(row.get("id", Long.class));
        value.setRegisteredClientId(row.get("registered_client_id", Long.class));
        value.setClientId(row.get("client_id", String.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setMcpConnectionId(row.get("mcp_connection_id", Long.class));
        value.setAuthorizationGrantType(row.get("authorization_grant_type", String.class));
        value.setAuthorizedScopes(row.get("authorized_scopes", String.class));
        value.setStateHash(row.get("state_hash", String.class));
        value.setAuthorizationCodeHash(row.get("authorization_code_hash", String.class));
        value.setAuthorizationCodeIssued(time(row.get("authorization_code_issued")));
        value.setAuthorizationCodeExpires(time(row.get("authorization_code_expires")));
        value.setAccessTokenJti(row.get("access_token_jti", String.class));
        value.setAccessTokenIssued(time(row.get("access_token_issued")));
        value.setAccessTokenExpires(time(row.get("access_token_expires")));
        value.setRefreshTokenHash(row.get("refresh_token_hash", String.class));
        value.setPreviousRefreshTokenHash(row.get("previous_refresh_token_hash", String.class));
        value.setRefreshTokenIssued(time(row.get("refresh_token_issued")));
        value.setRefreshTokenExpires(time(row.get("refresh_token_expires")));
        value.setTokenMetadata(row.get("token_metadata", String.class));
        value.setRevokedTime(time(row.get("revoked_time")));
        value.setRevokeReason(row.get("revoke_reason", String.class));
        return value;
    }

    private OAuthConsentRecord consent(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        OAuthConsentRecord value = new OAuthConsentRecord();
        value.setId(row.get("id", Long.class));
        value.setRegisteredClientId(row.get("registered_client_id", Long.class));
        value.setClientId(row.get("client_id", String.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setScopes(row.get("scopes", String.class));
        value.setConsentExt(row.get("consent_ext", String.class));
        return value;
    }

    private OAuthRegisteredClientRecord registeredClient(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        OAuthRegisteredClientRecord value = new OAuthRegisteredClientRecord();
        value.setId(row.get("id", Long.class));
        value.setClientId(row.get("client_id", String.class));
        value.setClientName(row.get("client_name", String.class));
        value.setClientType(row.get("client_type", String.class));
        value.setOwnerPrincipalId(row.get("owner_principal_id", Long.class));
        value.setServiceAccountPrincipalId(row.get("service_account_principal_id", Long.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setClientSecretHash(row.get("client_secret_hash", String.class));
        value.setClientSecretExpiresAt(time(row.get("client_secret_expires_at")));
        value.setClientAuthMethods(row.get("client_auth_methods", String.class));
        value.setAuthorizationGrantTypes(row.get("authorization_grant_types", String.class));
        value.setRedirectUris(row.get("redirect_uris", String.class));
        value.setScopes(row.get("scopes", String.class));
        value.setRequirePkce(number(row.get("require_pkce")));
        value.setRequireConsent(number(row.get("require_consent")));
        value.setEnableFlag(number(row.get("enable_flag")));
        return value;
    }

    private McpConnectionRecord connection(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        McpConnectionRecord value = new McpConnectionRecord();
        value.setId(row.get("id", Long.class));
        value.setConnectionName(row.get("connection_name", String.class));
        value.setClientId(row.get("client_id", String.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setPrincipalType(row.get("principal_type", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setGrantType(row.get("grant_type", String.class));
        value.setEnableFlag(number(row.get("enable_flag")));
        value.setExpireTime(time(row.get("expire_time")));
        value.setRevokeTime(time(row.get("revoke_time")));
        value.setLastUsedTime(time(row.get("last_used_time")));
        value.setRemark(row.get("remark", String.class));
        value.setCreatorId(row.get("creator_id", Long.class));
        value.setCreatorName(row.get("creator_name", String.class));
        return value;
    }

    private McpToolRecord tool(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        McpToolRecord value = new McpToolRecord();
        value.setId(row.get("id", Long.class));
        value.setToolId(row.get("tool_id", String.class));
        value.setToolName(row.get("tool_name", String.class));
        value.setToolTitle(row.get("tool_title", String.class));
        value.setToolCategory(row.get("tool_category", String.class));
        value.setServiceName(row.get("service_name", String.class));
        value.setApiCode(row.get("api_code", String.class));
        value.setPermissionCode(row.get("permission_code", String.class));
        value.setHttpMethod(row.get("http_method", String.class));
        value.setApiPath(row.get("api_path", String.class));
        value.setSchemaHash(row.get("schema_hash", String.class));
        value.setRiskLevel(row.get("risk_level", String.class));
        value.setReadOnlyHint(number(row.get("read_only_hint")));
        value.setDestructiveHint(number(row.get("destructive_hint")));
        value.setIdempotentHint(number(row.get("idempotent_hint")));
        value.setOpenWorldHint(number(row.get("open_world_hint")));
        value.setEnableFlag(number(row.get("enable_flag")));
        value.setRemark(row.get("remark", String.class));
        value.setToolExt(row.get("tool_ext", String.class));
        return value;
    }

    private McpToolConfirmationRecord confirmation(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        McpToolConfirmationRecord value = new McpToolConfirmationRecord();
        value.setId(row.get("id", Long.class));
        value.setConfirmId(row.get("confirm_id", String.class));
        value.setTenantId(row.get("tenant_id", Long.class));
        value.setPrincipalId(row.get("principal_id", Long.class));
        value.setConnectionId(row.get("connection_id", Long.class));
        value.setToolId(row.get("tool_id", String.class));
        value.setArgumentDigest(row.get("argument_digest", String.class));
        value.setIdempotencyKey(row.get("idempotency_key", String.class));
        value.setRiskLevel(row.get("risk_level", String.class));
        value.setStatus(row.get("status", String.class));
        value.setExpireTime(time(row.get("expire_time")));
        value.setConsumedTime(time(row.get("consumed_time")));
        value.setCreateTime(time(row.get("create_time")));
        return value;
    }

    private Byte number(Object value) {
        return value instanceof Number number ? number.byteValue() : null;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof java.time.OffsetDateTime offset)
            return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
