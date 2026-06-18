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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.dto.McpConnectionToolsReplaceRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolCatalogListRequestDTO;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationRequestDTO;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * RBAC-protected MCP management endpoints used by the settings UI.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Tag(name = "mcp", description = "MCP OAuth connections: register clients, exchange tokens, and manage authorization flows for AI agent integration via the Model Context Protocol")
@Slf4j
@RestController
@RequestMapping(AuthConstant.MCP_URL_PREFIX)
@RequiredArgsConstructor
public class McpManagementController implements BaseController {

    private final OAuthMcpRuntimeService oauthMcpRuntimeService;

    /**
     * Fetch the OAuth authorization server metadata for the MCP runtime.
     *
     * @return the authorization server metadata (issuer, token and registration endpoints)
     */
    @PreAuthorize("@perm.can('mcp', 'get')")
    @Operation(summary = "Get MCP OAuth Metadata", description = "Fetch the OAuth authorization server metadata for the MCP runtime, "
            + "including issuer, token and registration endpoints. Use to discover how MCP clients should authenticate.")
    @GetMapping("/metadata")
    public Mono<R<Map<String, Object>>> metadata() {
        return async(() -> R.ok(oauthMcpRuntimeService.authorizationServerMetadata()));
    }

    /**
     * Register an OAuth client owned by the current principal for MCP access.
     *
     * @param request OAuth client registration payload (grant types, redirects, scopes)
     * @return the registration response carrying the new client id and one-time secret
     */
    @PreAuthorize("@perm.can('mcp', 'add')")
    @Operation(summary = "Register OAuth Client", description = "Register an OAuth client owned by the current principal for MCP access. "
            + "Returns the client id and secret; the secret is shown only once at registration time.")
    @PostMapping("/client/register")
    public Mono<R<OAuthClientRegistrationResponseDTO>> registerClient(
            @RequestBody OAuthClientRegistrationRequestDTO request) {
        return getPrincipalHeader().flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.registerClient(request,
                header))));
    }

    /**
     * List the OAuth clients owned by the current principal.
     *
     * @return the principal's client records, without secrets
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List OAuth Clients", description = "List the OAuth clients the current principal owns. "
            + "Returns client records without secrets; use to pick a client before creating or inspecting a connection.")
    @PostMapping("/client/list")
    public Mono<R<List<OAuthRegisteredClientRecord>>> listClients() {
        return getPrincipalHeader().flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.listClients(header))));
    }

    /**
     * List the MCP connections owned by the current principal.
     *
     * @return the principal's connections, each binding an OAuth client to a tool whitelist
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Connections", description = "List the MCP connections owned by the current principal. "
            + "Each connection binds an OAuth client to a tool whitelist; use to review which clients are wired up.")
    @PostMapping("/connection/list")
    public Mono<R<List<McpConnectionRecord>>> listConnections() {
        return getPrincipalHeader()
                .flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.listConnections(header))));
    }

    /**
     * Create an MCP connection linking an OAuth client to an allowed tool set.
     *
     * @param connection connection payload binding a registered OAuth client to a tool whitelist
     * @return the persisted connection record; the client must already be registered
     */
    @PreAuthorize("@perm.can('mcp', 'add')")
    @Operation(summary = "Create MCP Connection", description = "Create an MCP connection linking an OAuth client to an allowed tool set for the current principal. "
            + "Returns the persisted connection record; the client must already be registered.")
    @PostMapping("/connection/add")
    public Mono<R<McpConnectionRecord>> createConnection(@RequestBody McpConnectionRecord connection) {
        return getPrincipalHeader()
                .flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.createConnection(connection, header))));
    }

    /**
     * Revoke an MCP connection by id, severing its OAuth client from the tool whitelist.
     *
     * @param id id of the MCP connection to revoke; only the owning principal may revoke it
     * @return true on successful revocation
     */
    @PreAuthorize("@perm.can('mcp', 'delete')")
    @Operation(summary = "Revoke MCP Connection", description = "Revoke an MCP connection by id, severing its OAuth client from the tool whitelist. "
            + "Only the principal that owns the connection may revoke it; returns true on success.")
    @PostMapping("/connection/revoke")
    public Mono<R<Boolean>> revokeConnection(@Parameter(description = "Primary key of the MCP connection to revoke.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            oauthMcpRuntimeService.revokeConnection(id, header);
            return R.ok(true);
        }));
    }

    /**
     * Replace a connection's tool whitelist with the supplied tool ids.
     *
     * @param request payload carrying the connection id and the new tool id list
     * @return true on successful replacement; the previous whitelist is fully overwritten
     */
    @PreAuthorize("@perm.can('mcp', 'update')")
    @Operation(summary = "Replace MCP Connection Tools", description = "Replace a connection's tool whitelist with the supplied tool ids, scoped to the owning principal. "
            + "The previous whitelist is fully overwritten; returns true on success.")
    @PostMapping("/connection/tools/replace")
    public Mono<R<Boolean>> replaceConnectionTools(@RequestBody McpConnectionToolsReplaceRequestDTO request) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            McpConnectionToolsReplaceRequestDTO body =
                    request == null ? new McpConnectionToolsReplaceRequestDTO() : request;
            oauthMcpRuntimeService.replaceConnectionTools(body.getConnectionId(), toolIds(body.getToolIds()),
                    header);
            return R.ok(true);
        }));
    }

    /**
     * List the tool ids a connection is currently allowed to invoke.
     *
     * @param id id of the MCP connection whose tool whitelist is listed
     * @return the connection's effective tool whitelist
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Connection Tools", description = "List the tool ids a connection is currently allowed to invoke. "
            + "Use to inspect a connection's effective whitelist before editing or revoking it.")
    @GetMapping("/connection/tools/list")
    public Mono<R<List<String>>> listConnectionTools(@Parameter(description = "Primary key of the MCP connection to list tools for.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.listConnectionToolIds(id,
                header))));
    }

    /**
     * Rebuild the MCP tool catalog from the registered APIs (dc3_api entries).
     *
     * @return the number of tools refreshed in the catalog
     */
    @PreAuthorize("@perm.can('mcp', 'update')")
    @Operation(summary = "Refresh MCP Tool Catalog", description = "Rebuild the MCP tool catalog from the registered APIs (dc3_api entries). "
            + "Returns the number of tools refreshed; call after API registrations change so the catalog stays current.")
    @PostMapping("/tool/catalog/refresh")
    public Mono<R<Integer>> refreshToolCatalog() {
        return async(() -> R.ok(oauthMcpRuntimeService.refreshToolCatalog()));
    }

    /**
     * Page the MCP tool catalog with optional keyword, risk level and limit filters.
     *
     * @param request optional catalog filter payload; an empty request lists all tools
     * @return tool records exposing each tool's schema
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Tool Catalog", description = "Page the MCP tool catalog with optional keyword, risk level and limit filters. "
            + "Returns tool records exposing each tool's schema; use to browse tools before whitelisting them on a connection.")
    @PostMapping("/tool/list")
    public Mono<R<List<McpToolRecord>>> listToolCatalog(
            @RequestBody(required = false) McpToolCatalogListRequestDTO request) {
        McpToolCatalogListRequestDTO body = request == null ? new McpToolCatalogListRequestDTO() : request;
        return async(() -> R.ok(oauthMcpRuntimeService.listToolCatalog(
                StringUtils.defaultString(body.getKeyword()),
                StringUtils.defaultString(body.getRiskLevel()),
                intValue(body.getLimit())
        )));
    }

    /**
     * List MCP tool-call audit entries scoped to the caller's tenant.
     *
     * @param principalId optional filter by owning principal id
     * @param toolId      optional filter by MCP tool id
     * @param status      optional filter by invocation outcome (SUCCESS, DENIED, POLICY_DENIED, ERROR, UNKNOWN)
     * @param riskLevel   optional filter by tool risk level (LOW, MEDIUM, HIGH)
     * @param limit       optional cap on the number of records returned
     * @return append-only audit records matching the filters
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Audit Log", description = "List MCP tool-call audit entries scoped to the caller's tenant, "
            + "filterable by principal, tool, status and risk level. Returns append-only records kept for compliance review.")
    @PostMapping("/audit/list")
    public Mono<R<List<McpAuditCommand>>> listAuditLog(
            @Parameter(description = "Filter by owning principal ID.", example = "2048") @RequestParam(value = "principal_id", required = false) Long principalId,
            @Parameter(description = "Filter by MCP tool ID.", example = "tool_read_device") @RequestParam(value = "tool_id", required = false) String toolId,
            @Parameter(description = "Filter by audit invocation outcome: SUCCESS, DENIED, POLICY_DENIED, ERROR, or UNKNOWN.", example = "SUCCESS") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by tool risk level: LOW, MEDIUM, or HIGH.", example = "LOW") @RequestParam(value = "risk_level", required = false) String riskLevel,
            @Parameter(description = "Maximum number of records to return.", example = "20") @RequestParam(value = "limit", required = false) Integer limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(oauthMcpRuntimeService.listAudit(
                tenantId, principalId, StringUtils.defaultString(toolId), StringUtils.defaultString(status),
                StringUtils.defaultString(riskLevel), intValue(limit)
        ))));
    }

    private List<String> toolIds(List<String> value) {
        return value == null ? List.of() : value.stream().filter(StringUtils::isNotBlank).toList();
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

}
