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

package io.github.pnoker.common.auth.mapper;

import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SQL boundary for OAuth and MCP runtime state.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface OAuthMcpMapper {

    // ------------------------------------------------------------------
    // Registered OAuth client
    // ------------------------------------------------------------------

    /**
     * Look up a registered OAuth client by its public client id. Tenant-agnostic;
     * client id is globally unique so the caller resolves the tenant from the record.
     *
     * @param clientId the public client id
     * @return the registered client record, or {@code null} if not found
     */
    OAuthRegisteredClientRecord selectClientByClientId(@Param("clientId") String clientId);

    /**
     * Insert a newly registered OAuth client.
     *
     * @param client the client record to insert
     * @return the number of affected rows
     */
    int insertClient(OAuthRegisteredClientRecord client);

    /**
     * List OAuth clients owned by a principal, scoped to one tenant.
     *
     * @param ownerPrincipalId the owning principal id
     * @param tenantId         the tenant scope
     * @return the matching client records
     */
    List<OAuthRegisteredClientRecord> listClientsByOwner(@Param("ownerPrincipalId") Long ownerPrincipalId,
                                                         @Param("tenantId") Long tenantId);

    // ------------------------------------------------------------------
    // Authorization and token lifecycle
    // ------------------------------------------------------------------

    /**
     * Look up an authorization by the SHA-256 hash of its authorization code.
     *
     * @param codeHash the authorization-code hash
     * @return the authorization record, or {@code null} if not found
     */
    OAuthAuthorizationRecord selectAuthorizationByCodeHash(@Param("codeHash") String codeHash);

    /**
     * Look up an authorization by its access token's JTI (JWT id), used for token
     * introspection and revocation.
     *
     * @param jti the access-token JTI
     * @return the authorization record, or {@code null} if not found
     */
    OAuthAuthorizationRecord selectAuthorizationByAccessTokenJti(@Param("jti") String jti);

    /**
     * Look up an authorization by the hash of its refresh token, used during the
     * refresh-token grant.
     *
     * @param refreshHash the refresh-token hash
     * @return the authorization record, or {@code null} if not found
     */
    OAuthAuthorizationRecord selectAuthorizationByRefreshTokenHash(@Param("refreshHash") String refreshHash);

    /**
     * Look up an authorization by the hash of a refresh token previously rotated out,
     * so a replayed (already-rotated) refresh token is detected and rejected.
     *
     * @param previousRefreshHash the previous (rotated) refresh-token hash
     * @return the authorization record, or {@code null} if not found
     */
    OAuthAuthorizationRecord selectAuthorizationByPreviousRefreshTokenHash(
            @Param("previousRefreshHash") String previousRefreshHash);

    /**
     * Insert a new authorization (code-issuance) record, before any token is issued.
     *
     * @param authorization the authorization record to insert
     * @return the number of affected rows
     */
    int insertAuthorization(OAuthAuthorizationRecord authorization);

    /**
     * Consume the authorization code and persist the issued access and refresh tokens.
     * Stores the new and previous refresh-token hashes so rotated refresh tokens can be
     * detected on replay.
     *
     * @param id                  the authorization record id
     * @param codeHash            the authorization-code hash (marks the code consumed)
     * @param accessTokenJti      the issued access-token JTI
     * @param accessIssued        access-token issuance time
     * @param accessExpires       access-token expiry time
     * @param refreshHash         the issued refresh-token hash
     * @param previousRefreshHash the prior refresh-token hash (for replay detection)
     * @param refreshIssued       refresh-token issuance time
     * @param refreshExpires      refresh-token expiry time
     * @param tokenClaims         the serialized token claims
     * @return the number of affected rows
     */
    int activateAuthorizationTokens(@Param("id") Long id,
                                    @Param("codeHash") String codeHash,
                                    @Param("accessTokenJti") String accessTokenJti,
                                    @Param("accessIssued") LocalDateTime accessIssued,
                                    @Param("accessExpires") LocalDateTime accessExpires,
                                    @Param("refreshHash") String refreshHash,
                                    @Param("previousRefreshHash") String previousRefreshHash,
                                    @Param("refreshIssued") LocalDateTime refreshIssued,
                                    @Param("refreshExpires") LocalDateTime refreshExpires,
                                    @Param("tokenClaims") String tokenClaims);

    /**
     * Revoke an authorization by its access-token JTI.
     *
     * @param jti         the access-token JTI to revoke
     * @param reason      the revocation reason
     * @param revokedTime the revocation time
     * @return the number of affected rows
     */
    int revokeAuthorizationByAccessTokenJti(@Param("jti") String jti,
                                            @Param("reason") String reason,
                                            @Param("revokedTime") LocalDateTime revokedTime);

    /**
     * Revoke an authorization by its refresh-token hash.
     *
     * @param refreshHash the refresh-token hash to revoke
     * @param reason      the revocation reason
     * @param revokedTime the revocation time
     * @return the number of affected rows
     */
    int revokeAuthorizationByRefreshTokenHash(@Param("refreshHash") String refreshHash,
                                              @Param("reason") String reason,
                                              @Param("revokedTime") LocalDateTime revokedTime);

    // ------------------------------------------------------------------
    // MCP connection
    // ------------------------------------------------------------------

    /**
     * Look up an MCP connection by id. Tenant-agnostic; the caller must re-check the
     * tenant from the returned record.
     *
     * @param id the connection id
     * @return the connection record, or {@code null} if not found
     */
    McpConnectionRecord selectConnectionById(@Param("id") Long id);

    /**
     * Look up the active (non-revoked) MCP connection for a client, principal, tenant,
     * and grant type. Used by the authorization endpoint to reuse an existing connection.
     *
     * @param clientId    the OAuth client id
     * @param principalId the connection principal
     * @param tenantId    the tenant scope
     * @param grantType   the grant type to match
     * @return the active connection record, or {@code null} if none exists
     */
    McpConnectionRecord selectActiveConnection(@Param("clientId") String clientId,
                                               @Param("principalId") Long principalId,
                                               @Param("tenantId") Long tenantId,
                                               @Param("grantType") String grantType);

    /**
     * List MCP connections owned by a principal, scoped to one tenant.
     *
     * @param tenantId    the tenant scope
     * @param principalId the owning principal
     * @return the matching connection records
     */
    List<McpConnectionRecord> listConnectionsByPrincipal(@Param("tenantId") Long tenantId,
                                                         @Param("principalId") Long principalId);

    /**
     * Insert a new MCP connection record.
     *
     * @param connection the connection record to insert
     * @return the number of affected rows
     */
    int insertConnection(McpConnectionRecord connection);

    /**
     * Revoke an MCP connection so it can no longer mint tokens. Tenant- and
     * principal-scoped: only a connection matching both the tenant and principal is
     * affected.
     *
     * @param id          the connection id
     * @param tenantId    the tenant scope (authorization guard)
     * @param principalId the owning principal (authorization guard)
     * @param revokeTime  the revocation time
     * @return the number of affected rows
     */
    int revokeConnection(@Param("id") Long id,
                         @Param("tenantId") Long tenantId,
                         @Param("principalId") Long principalId,
                         @Param("revokeTime") LocalDateTime revokeTime);

    // ------------------------------------------------------------------
    // MCP tool catalog
    // ------------------------------------------------------------------

    /**
     * List candidate tools sourced from the resource table for catalog refresh, before
     * they are joined with OpenAPI quality metadata.
     *
     * @return the candidate tool records
     */
    List<McpToolRecord> listRegistryToolCandidates();

    /**
     * Look up a catalog tool by its stable tool id.
     *
     * @param toolId the tool id
     * @return the tool record, or {@code null} if not found
     */
    McpToolRecord selectToolByToolId(@Param("toolId") String toolId);

    /**
     * List the published tool catalog with optional keyword and risk-level filters.
     *
     * @param keyword   keyword filter applied to tool name and description
     * @param riskLevel risk-level filter
     * @param limit     maximum number of results
     * @return the matching tool records
     */
    List<McpToolRecord> listToolCatalog(@Param("keyword") String keyword,
                                        @Param("riskLevel") String riskLevel,
                                        @Param("limit") int limit);

    /**
     * Insert a new catalog tool.
     *
     * @param tool the tool record to insert
     * @return the number of affected rows
     */
    int insertTool(McpToolRecord tool);

    /**
     * Update an existing catalog tool with refreshed quality metadata.
     *
     * @param tool the tool record to update
     * @return the number of affected rows
     */
    int updateTool(McpToolRecord tool);

    // ------------------------------------------------------------------
    // MCP audit
    // ------------------------------------------------------------------

    /**
     * List MCP tool-call audit entries filtered by tenant, principal, tool, status, and
     * risk level. Always scoped to one tenant.
     *
     * @param tenantId    the tenant scope
     * @param principalId optional principal filter
     * @param toolId      optional tool filter
     * @param status      optional status filter
     * @param riskLevel   optional risk-level filter
     * @param limit       maximum number of results
     * @return the matching audit entries
     */
    List<McpAuditCommand> listAudit(@Param("tenantId") Long tenantId,
                                    @Param("principalId") Long principalId,
                                    @Param("toolId") String toolId,
                                    @Param("status") String status,
                                    @Param("riskLevel") String riskLevel,
                                    @Param("limit") int limit);

    /**
     * Insert a tool-call audit entry.
     *
     * @param command the audit command to record
     * @return the number of affected rows
     */
    int insertAudit(McpAuditCommand command);

    // ------------------------------------------------------------------
    // Tool visibility and connection tools
    // ------------------------------------------------------------------

    /**
     * List the tools visible to a connection given the caller's tenant, principal, and
     * whether high-risk tools are allowed by scope.
     *
     * @param tenantId      the connection's tenant
     * @param principalId   the connection's principal
     * @param connectionId  the connection id
     * @param allowHighRisk whether high-risk tools are included
     * @return the visible tool records
     */
    List<McpToolRecord> listVisibleTools(@Param("tenantId") Long tenantId,
                                         @Param("principalId") Long principalId,
                                         @Param("connectionId") Long connectionId,
                                         @Param("allowHighRisk") boolean allowHighRisk);

    /**
     * Resolve a single visible tool by name for a connection.
     *
     * @param tenantId      the connection's tenant
     * @param principalId   the connection's principal
     * @param connectionId  the connection id
     * @param toolName      the tool name to resolve
     * @param allowHighRisk whether a high-risk tool is allowed
     * @return the tool record, or {@code null} if not visible
     */
    McpToolRecord selectVisibleToolByName(@Param("tenantId") Long tenantId,
                                          @Param("principalId") Long principalId,
                                          @Param("connectionId") Long connectionId,
                                          @Param("toolName") String toolName,
                                          @Param("allowHighRisk") boolean allowHighRisk);

    /**
     * Stamp a connection's last-used time.
     *
     * @param id           the connection id
     * @param lastUsedTime the last-used time
     * @return the number of affected rows
     */
    int updateConnectionLastUsed(@Param("id") Long id, @Param("lastUsedTime") LocalDateTime lastUsedTime);

    /**
     * Delete all tools bound to a connection, the first step of a full replacement.
     *
     * @param connectionId the connection id
     * @return the number of affected rows
     */
    int deleteConnectionTools(@Param("connectionId") Long connectionId);

    /**
     * List the tool ids enabled for a connection, scoped to one tenant and principal.
     *
     * @param connectionId the connection id
     * @param tenantId     the tenant scope
     * @param principalId  the owning principal
     * @return the enabled tool ids
     */
    List<String> listConnectionToolIds(@Param("connectionId") Long connectionId,
                                       @Param("tenantId") Long tenantId,
                                       @Param("principalId") Long principalId);

    /**
     * Bind one tool to a connection, recording who performed the binding.
     *
     * @param id           the generated binding id
     * @param connectionId the connection id
     * @param toolId       the tool id to bind
     * @param operatorId   the principal performing the binding
     * @param operatorName the operator display name
     * @return the number of affected rows
     */
    int insertConnectionTool(@Param("id") Long id,
                             @Param("connectionId") Long connectionId,
                             @Param("toolId") String toolId,
                             @Param("operatorId") Long operatorId,
                             @Param("operatorName") String operatorName);

    // ------------------------------------------------------------------
    // High-risk tool confirmation
    // ------------------------------------------------------------------

    /**
     * Insert a pending high-risk tool confirmation ticket.
     *
     * @param confirmation the confirmation record to insert
     * @return the number of affected rows
     */
    int insertConfirmation(McpToolConfirmationRecord confirmation);

    /**
     * Look up a confirmation ticket by its confirm id.
     *
     * @param confirmId the confirm id
     * @return the confirmation record, or {@code null} if not found
     */
    McpToolConfirmationRecord selectConfirmationByConfirmId(@Param("confirmId") String confirmId);

    /**
     * Look up a confirmation already consumed for an idempotency key, so a replayed
     * high-risk call is detected and rejected.
     *
     * @param connectionId   the connection id
     * @param idempotencyKey the idempotency key
     * @return the consumed confirmation record, or {@code null} if not consumed yet
     */
    McpToolConfirmationRecord selectConsumedByIdempotencyKey(@Param("connectionId") Long connectionId,
                                                             @Param("idempotencyKey") String idempotencyKey);

    /**
     * Consume a pending confirmation ticket. Guarded by {@code status = PENDING} in the
     * SQL so a replayed confirm id loses the race and affects no rows.
     *
     * @param id           the confirmation id
     * @param consumedTime the consumption time
     * @return the number of affected rows (0 if the ticket was already consumed)
     */
    int consumeConfirmation(@Param("id") Long id, @Param("consumedTime") LocalDateTime consumedTime);

}
