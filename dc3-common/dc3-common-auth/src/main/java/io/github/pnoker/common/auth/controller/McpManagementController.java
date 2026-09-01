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

import io.github.pnoker.common.auth.biz.ReactiveOAuthMcpRuntimeService;
import io.github.pnoker.common.auth.exception.OAuthProtocolException;
import io.github.pnoker.common.auth.entity.builder.McpConnectionBuilder;
import io.github.pnoker.common.auth.entity.builder.OAuthClientBuilder;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionAddVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionToolsReplaceVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.McpToolCatalogQueryVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationRequestVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationResponseVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.auth.service.ReactiveMcpCatalogService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;

import java.util.List;
import java.util.Map;

/**
 * RBAC-protected MCP management endpoints used by the settings UI.
 *
 * @author pnoker
 * @since 2026.6.12
 */
@Tag(name = "mcp", description = "MCP OAuth connections: register clients, exchange tokens, and manage authorization flows for AI agent integration via the Model Context Protocol")
@Slf4j
@RestController
@RequestMapping(AuthConstant.MCP_URL_PREFIX)
@RequiredArgsConstructor
public class McpManagementController implements BaseController {

    private final ReactiveOAuthMcpRuntimeService oauthMcpRuntimeService;
    private final ReactiveMcpCatalogService reactiveMcpCatalogService;
    private final McpConnectionBuilder mcpConnectionBuilder;
    private final OAuthClientBuilder oauthClientBuilder;

    /**
     * Fetch the OAuth authorization server metadata for the MCP runtime.
     *
     * @return the authorization server metadata (issuer, token and registration endpoints)
     */
    @PreAuthorize("@perm.can('mcp', 'get')")
    @Operation(summary = "Get MCP OAuth Metadata", description = "Fetch the OAuth authorization server metadata for the MCP runtime, "
            + "including issuer, token and registration endpoints. Use to discover how MCP clients should authenticate.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/metadata")
    public Mono<Map<String, Object>> metadata() {
        return oauthMcpRuntimeService.authorizationServerMetadata();
    }

    /**
     * Register an OAuth client owned by the current principal for MCP access.
     *
     * @param request OAuth client registration payload (grant types, redirects, scopes)
     * @return the registration response carrying the new client id and one-time secret
     */
    @PreAuthorize("@perm.can('mcp', 'add')")
    @Operation(summary = "Register OAuth Client", description = "Register an OAuth client owned by the current principal for MCP access. "
            + "Returns the client id and secret; the secret is shown only once at registration time.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/client/register")
    public Mono<OAuthClientRegistrationResponseVO> registerClient(
            @RequestBody OAuthClientRegistrationRequestVO request) {
        return getPrincipalHeader().flatMap(header -> {
            if (oauthClientBuilder.isUnknownClientType(request)) {
                return Mono.error(new OAuthProtocolException(HttpStatus.BAD_REQUEST.value(), "invalid_client_metadata",
                        "unsupported client_type"));
            }
            return oauthMcpRuntimeService.registerClient(oauthClientBuilder.buildBOByRequestVO(request), header);
        });
    }

    /**
     * List the OAuth clients owned by the current principal.
     *
     * @return the principal's client records, without secrets
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List OAuth Clients", description = "List the OAuth clients the current principal owns. "
            + "Returns client records without secrets; use to pick a client before creating or inspecting a connection.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/client/list")
    public Mono<List<OAuthClientVO>> listClients() {
        return getPrincipalHeader().flatMap(header -> oauthMcpRuntimeService.listClients(header).collectList());
    }

    /**
     * List the MCP connections owned by the current principal.
     *
     * @return the principal's connections, each binding an OAuth client to a tool whitelist
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Connections", description = "List the MCP connections owned by the current principal. "
            + "Each connection binds an OAuth client to a tool whitelist; use to review which clients are wired up.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/connection/list")
    public Mono<List<McpConnectionVO>> listConnections() {
        return getPrincipalHeader().flatMap(oauthMcpRuntimeService::listConnections);
    }

    /**
     * Create an MCP connection linking an OAuth client to an allowed tool set.
     *
     * @param connection connection payload binding a registered OAuth client to a tool whitelist
     * @return the persisted connection record; the client must already be registered
     */
    @PreAuthorize("@perm.can('mcp', 'add')")
    @Operation(summary = "Create MCP Connection", description = "Create an MCP connection linking an OAuth client to an allowed tool set for the current principal. "
            + "Returns the persisted connection record; the client must already be registered.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/connection/add")
    public Mono<McpConnectionVO> createConnection(
            @Validated(Add.class) @RequestBody McpConnectionAddVO connection) {
        return getPrincipalHeader().flatMap(header -> oauthMcpRuntimeService.createConnection(
                mcpConnectionBuilder.buildBOByAddVO(connection), header));
    }

    /**
     * Revoke an MCP connection by id, severing its OAuth client from the tool whitelist.
     *
     * @param id id of the MCP connection to revoke; only the owning principal may revoke it
     * @return true on successful revocation
     */
    @PreAuthorize("@perm.can('mcp', 'delete')")
    @Operation(summary = "Revoke MCP Connection", description = "Revoke an MCP connection by id, severing its OAuth client from the tool whitelist. "
            + "Only the principal that owns the connection may revoke it; returns true on success.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PostMapping("/connection/revoke")
    public Mono<Boolean> revokeConnection(@Parameter(description = "Primary key of the MCP connection to revoke.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> oauthMcpRuntimeService.revokeConnection(id, header).thenReturn(true));
    }

    /**
     * Replace a connection's tool whitelist with the supplied tool ids.
     *
     * @param request payload carrying the connection id and the new tool id list
     * @return true on successful replacement; the previous whitelist is fully overwritten
     */
    @PreAuthorize("@perm.can('mcp', 'update')")
    @Operation(summary = "Replace MCP Connection Tools", description = "Replace a connection's tool whitelist with the supplied tool ids, scoped to the owning principal. "
            + "The previous whitelist is fully overwritten; returns true on success.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/connection/tools/replace")
    public Mono<Boolean> replaceConnectionTools(
            @Validated(Update.class) @RequestBody McpConnectionToolsReplaceVO request) {
        return getPrincipalHeader().flatMap(header -> {
            McpConnectionToolsReplaceVO body =
                    request == null ? new McpConnectionToolsReplaceVO() : request;
            return oauthMcpRuntimeService.replaceConnectionTools(Long.parseLong(body.getConnectionId()), toolIds(body.getToolIds()),
                    header).thenReturn(true);
        });
    }

    /**
     * List the tool ids a connection is currently allowed to invoke.
     *
     * @param id id of the MCP connection whose tool whitelist is listed
     * @return the connection's effective tool whitelist
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Connection Tools", description = "List the tool ids a connection is currently allowed to invoke. "
            + "Use to inspect a connection's effective whitelist before editing or revoking it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/connection/tools/list")
    public Mono<List<String>> listConnectionTools(@Parameter(description = "Primary key of the MCP connection to list tools for.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMapMany(header -> oauthMcpRuntimeService.listConnectionToolIds(id, header)).collectList();
    }

    /**
     * Rebuild the MCP tool catalog from the registered APIs (dc3_api entries).
     *
     * @return the number of tools refreshed in the catalog
     */
    @PreAuthorize("@perm.can('mcp', 'update')")
    @Operation(summary = "Refresh MCP Tool Catalog", description = "Rebuild the MCP tool catalog from the registered APIs (dc3_api entries). "
            + "Returns the number of tools refreshed; call after API registrations change so the catalog stays current.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/tool/catalog/refresh")
    public Mono<Integer> refreshToolCatalog() {
        return oauthMcpRuntimeService.refreshToolCatalog();
    }

    /**
     * Page the MCP tool catalog with optional keyword, risk level and limit filters.
     *
     * @param request optional catalog filter and pagination payload; an empty request lists the first page of all tools
     * @return one page of tool records exposing each tool's schema
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Tool Catalog", description = "Page the MCP tool catalog with optional keyword and risk level filters. "
            + "Returns one page of tool records exposing each tool's schema; use to browse tools before whitelisting them on a connection.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/tool/list")
    public Mono<OffsetPage<McpToolVO>> listToolCatalog(
            @RequestBody(required = false) McpToolCatalogQueryVO request) {
        McpToolCatalogQueryVO body = request == null ? new McpToolCatalogQueryVO() : request;
        return Mono.defer(() -> reactiveMcpCatalogService.listTools(StringUtils.trimToEmpty(body.getKeyword()),
                StringUtils.trimToEmpty(body.getRiskLevel()), page(body.getOffset(), body.getLimit(), body.getSort())));
    }

    /**
     * Page MCP tool-call audit entries scoped to the caller's tenant.
     *
     * @param principalId optional filter by owning principal id
     * @param toolId      optional filter by MCP tool id
     * @param status      optional filter by invocation outcome (SUCCESS, DENIED, POLICY_DENIED, ERROR, UNKNOWN)
     * @param riskLevel   optional filter by tool risk level (LOW, MEDIUM, HIGH)
     * @param offset      zero-based result offset
     * @param limit       maximum page size (bounded to 1-200)
     * @return one page of append-only audit records matching the filters
     */
    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Audit Log", description = "Page MCP tool-call audit entries scoped to the caller's tenant, "
            + "filterable by principal, tool, status and risk level. Returns append-only records kept for compliance review.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/audit/list")
    public Mono<OffsetPage<McpAuditVO>> listAuditLog(
            @Parameter(description = "Filter by owning principal ID.", example = "2048") @RequestParam(value = "principal_id", required = false) Long principalId,
            @Parameter(description = "Filter by MCP tool ID.", example = "tool_read_device") @RequestParam(value = "tool_id", required = false) String toolId,
            @Parameter(description = "Filter by audit invocation outcome: SUCCESS, DENIED, POLICY_DENIED, ERROR, or UNKNOWN.", example = "SUCCESS") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Filter by tool risk level: LOW, MEDIUM, or HIGH.", example = "LOW") @RequestParam(value = "risk_level", required = false) String riskLevel,
            @Parameter(description = "Zero-based result offset.", example = "0") @RequestParam(value = "offset", required = false) Long offset,
            @Parameter(description = "Maximum number of results, bounded to 1-200.", example = "50") @RequestParam(value = "limit", required = false) Integer limit) {
        return getTenantId().flatMap(tenantId -> reactiveMcpCatalogService.listAudit(
                tenantId, principalId, StringUtils.trimToEmpty(toolId), StringUtils.trimToEmpty(status),
                StringUtils.trimToEmpty(riskLevel), new PageRequest(offset == null ? 0 : offset,
                        limit == null ? PageRequest.DEFAULT_LIMIT : limit, List.of())));
    }

    private List<String> toolIds(List<String> value) {
        return value == null ? List.of() : value.stream().filter(StringUtils::isNotBlank).toList();
    }

    private PageRequest page(Long offset, Integer limit, List<io.github.pnoker.db.r2dbc.core.page.SortSpec> sort) {
        return new PageRequest(offset == null ? 0 : offset,
                limit == null ? PageRequest.DEFAULT_LIMIT : limit, sort);
    }

}
