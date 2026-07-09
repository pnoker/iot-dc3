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

package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bo.McpConnectionAddBO;
import io.github.pnoker.common.auth.entity.bo.OAuthClientRegistrationBO;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationResponseVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OAuth 2.1 and MCP runtime service.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface OAuthMcpRuntimeService {

    /**
     * Publish the OAuth 2.1 authorization server metadata (RFC 8414). This is an
     * unauthenticated discovery endpoint; tenant context is intentionally ignored.
     *
     * @return the authorization server metadata as a JSON-serializable map
     */
    Map<String, Object> authorizationServerMetadata();

    /**
     * Publish the JSON Web Key Set used to verify access-token signatures. This is an
     * unauthenticated endpoint; tenant context is intentionally ignored.
     *
     * @return the JWKS as a JSON-serializable map containing the RSA public key
     */
    Map<String, Object> jwks();

    /**
     * Dynamically register an OAuth client (RFC 7591). The registered client is bound
     * to the authenticated caller's tenant; the request body's tenant id is ignored so a
     * caller cannot register a client for a tenant they do not belong to.
     *
     * @param request         client registration request carrying name, type, grants,
     *                        scopes, and redirect URIs
     * @param principalHeader authenticated caller principal and tenant
     * @return the registration response including the generated client id and, for
     * confidential clients, the one-time client secret
     */
    OAuthClientRegistrationResponseVO registerClient(OAuthClientRegistrationBO request,
                                                     RequestHeader.PrincipalHeader principalHeader);

    /**
     * List OAuth clients owned by the authenticated caller, scoped to the caller's tenant.
     *
     * @param principalHeader authenticated caller principal and tenant
     * @return the caller's registered clients
     */
    List<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principalHeader);

    /**
     * Process the authorization-code grant authorization endpoint. Validates the client,
     * redirect URI, optional PKCE challenge, tenant membership, and the MCP connection,
     * then issues a single-use authorization code. Runs before tenant context is
     * established, so membership is validated explicitly.
     *
     * @param params           OAuth authorization request parameters (response_type,
     *                         client_id, redirect_uri, scope, code_challenge, state, etc.)
     * @param principalHeader  authenticated caller principal and tenant
     * @return the redirect URI carrying the authorization code (and echoed state)
     */
    URI authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader);

    /**
     * Process the token endpoint for authorization-code, client-credentials, and
     * refresh-token grants, issuing or refreshing access tokens. This is cross-tenant
     * authentication; the issued token's tenant claim is the authorization basis and is
     * validated explicitly.
     *
     * @param form               token request form (grant_type and grant-specific fields)
     * @param authorizationHeader client credentials for confidential clients
     * @return the token response as a JSON-serializable map
     */
    Map<String, Object> token(Map<String, String> form, String authorizationHeader);

    /**
     * Introspect an access token (RFC 7662), invoked by the gateway on every MCP request.
     * Verifies the token signature, checks the underlying authorization is still active,
     * the MCP connection is still usable, the principal is enabled, and the principal is
     * still a member of the token's tenant.
     *
     * @param token the access token to introspect
     * @return the introspection response, inactive when any check fails
     */
    McpIntrospectResponseDTO introspect(String token);

    /**
     * Revoke an access or refresh token (RFC 7009). Resolves the authorization by the
     * presented token only and marks it revoked, regardless of tenant context.
     *
     * @param form               revocation request form carrying the token to revoke
     * @param authorizationHeader client credentials
     * @return a JSON-serializable map with the revoked flag
     */
    Map<String, Object> revoke(Map<String, String> form, String authorizationHeader);

    /**
     * Refresh the MCP tool catalog from the OpenAPI source, joining tool quality metadata
     * (description, risk flags, input schema) by api code. Inserts new tools and updates
     * changed ones; the tool set itself is still driven by the resource table.
     *
     * @return the number of tools inserted or updated
     */
    int refreshToolCatalog();

    /**
     * List the MCP tool catalog with optional keyword and risk-level filters. The result
     * limit is bounded to a safe maximum.
     *
     * @param keyword   optional keyword filter applied to tool name and description
     * @param riskLevel optional risk-level filter
     * @param limit     maximum number of results (bounded to 1-500, defaults to 200)
     * @return the matching tool catalog entries
     */
    List<McpToolVO> listToolCatalog(String keyword, String riskLevel, int limit);

    /**
     * List MCP connections owned by the authenticated caller, scoped to the caller's tenant.
     *
     * @param principalHeader authenticated caller principal and tenant
     * @return the caller's MCP connections
     */
    List<McpConnectionVO> listConnections(RequestHeader.PrincipalHeader principalHeader);

    /**
     * Create a new MCP connection binding a principal to an OAuth client. The connection
     * is scoped to the authenticated caller's tenant.
     *
     * @param connection       connection definition carrying name, client id, principal, etc.
     * @param principalHeader  authenticated caller principal and tenant
     * @return the created connection
     */
    McpConnectionVO createConnection(McpConnectionAddBO connection,
                                     RequestHeader.PrincipalHeader principalHeader);

    /**
     * Revoke an MCP connection so it can no longer mint tokens. Tenant-scoped: only the
     * caller's own connection within their tenant can be revoked.
     *
     * @param connectionId     the connection to revoke
     * @param principalHeader  authenticated caller principal and tenant
     */
    void revokeConnection(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    /**
     * Replace the full set of tools enabled for an MCP connection. All existing bindings
     * are deleted before the new set is inserted; each tool id must exist and be enabled.
     * Tenant-scoped: the connection must belong to the caller's tenant and principal.
     *
     * @param connectionId     the connection to update
     * @param toolIds          the complete new set of enabled tool ids
     * @param principalHeader  authenticated caller principal and tenant
     */
    void replaceConnectionTools(Long connectionId, List<String> toolIds,
                                RequestHeader.PrincipalHeader principalHeader);

    /**
     * List the tool ids enabled for an MCP connection. Tenant-scoped to the caller.
     *
     * @param connectionId     the connection to query
     * @param principalHeader  authenticated caller principal and tenant
     * @return the enabled tool ids for the connection
     */
    List<String> listConnectionToolIds(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    /**
     * List the tools visible to a connection given its granted scopes. Requires the
     * tools/list or tools/call scope; the tools/call_high scope gates high-risk tools in.
     *
     * @param tenantId      the connection's tenant
     * @param principalId   the connection's principal
     * @param connectionId  the connection id
     * @param scopes        the access token's granted scopes
     * @return the visible tool definitions
     */
    List<McpToolDefinitionDTO> listVisibleTools(Long tenantId, Long principalId, Long connectionId,
                                                Set<String> scopes);

    /**
     * Resolve a single visible tool by name for a connection, updating the connection's
     * last-used timestamp. Requires the tools/call scope; high-risk visibility follows the
     * tools/call_high scope.
     *
     * @param tenantId      the connection's tenant
     * @param principalId   the connection's principal
     * @param connectionId  the connection id
     * @param toolName      the tool name to resolve
     * @param scopes        the access token's granted scopes
     * @return the resolved tool definition
     */
    McpToolResolveResponseDTO resolveVisibleTool(Long tenantId, Long principalId, Long connectionId, String toolName,
                                                 Set<String> scopes);

    /**
     * Authorize a tool call, the authoritative gate before invocation. Re-runs visibility
     * and scope checks; non-high-risk tools pass straight through, while high-risk tools
     * require a confirmation ticket flow (issue, then confirm with an idempotency key).
     *
     * @param request the authorization request carrying tenant, principal, connection,
     *                tool name, scopes, arguments digest, and optional confirmation
     * @return the authorization decision (authorized, confirmation required, or rejected)
     */
    McpToolAuthorizeResponseDTO authorizeToolCall(McpToolAuthorizeRequestDTO request);

    /**
     * Record an MCP tool-call audit entry.
     *
     * @param command the audit command carrying tenant, principal, connection, tool,
     *                outcome, and risk level
     */
    void audit(McpAuditCommandDTO command);

    /**
     * List MCP tool-call audit entries filtered by tenant, principal, tool, status, and
     * risk level. The result limit is bounded to a safe maximum.
     *
     * @param tenantId   tenant scope
     * @param principalId optional principal filter
     * @param toolId     optional tool filter
     * @param status     optional status filter
     * @param riskLevel  optional risk-level filter
     * @param limit      maximum number of results (bounded to 1-500, defaults to 200)
     * @return the matching audit entries
     */
    List<McpAuditVO> listAudit(Long tenantId, Long principalId, String toolId, String status,
                               String riskLevel, int limit);

}
