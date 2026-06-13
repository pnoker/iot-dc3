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
@Tag(name = "mcp", description = "MCP OAuth management")
@Slf4j
@RestController
@RequestMapping(AuthConstant.MCP_URL_PREFIX)
@RequiredArgsConstructor
public class McpManagementController implements BaseController {

    private final OAuthMcpRuntimeService oauthMcpRuntimeService;

    @PreAuthorize("@perm.can('mcp', 'get')")
    @Operation(summary = "Get MCP OAuth Metadata", description = "Get OAuth authorization server metadata")
    @GetMapping("/metadata")
    public Mono<R<Map<String, Object>>> metadata() {
        return async(() -> R.ok(oauthMcpRuntimeService.authorizationServerMetadata()));
    }

    @PreAuthorize("@perm.can('mcp', 'add')")
    @Operation(summary = "Register OAuth Client", description = "Register an OAuth client for MCP usage")
    @PostMapping("/client/register")
    public Mono<R<OAuthClientRegistrationResponseDTO>> registerClient(
            @RequestBody OAuthClientRegistrationRequestDTO request) {
        return getPrincipalHeader().flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.registerClient(request,
                header))));
    }

    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List OAuth Clients", description = "List OAuth clients owned by the current principal")
    @PostMapping("/client/list")
    public Mono<R<List<OAuthRegisteredClientRecord>>> listClients() {
        return getPrincipalHeader().flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.listClients(header))));
    }

    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Connections", description = "List MCP connections owned by the current principal")
    @PostMapping("/connection/list")
    public Mono<R<List<McpConnectionRecord>>> listConnections() {
        return getPrincipalHeader()
                .flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.listConnections(header))));
    }

    @PreAuthorize("@perm.can('mcp', 'add')")
    @Operation(summary = "Create MCP Connection", description = "Create an MCP connection for an OAuth client")
    @PostMapping("/connection/add")
    public Mono<R<McpConnectionRecord>> createConnection(@RequestBody McpConnectionRecord connection) {
        return getPrincipalHeader()
                .flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.createConnection(connection, header))));
    }

    @PreAuthorize("@perm.can('mcp', 'delete')")
    @Operation(summary = "Revoke MCP Connection", description = "Revoke an MCP connection")
    @PostMapping("/connection/revoke")
    public Mono<R<Boolean>> revokeConnection(@NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            oauthMcpRuntimeService.revokeConnection(id, header);
            return R.ok(true);
        }));
    }

    @PreAuthorize("@perm.can('mcp', 'update')")
    @Operation(summary = "Replace MCP Connection Tools", description = "Replace a connection tool whitelist")
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

    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Connection Tools", description = "List a connection tool whitelist")
    @GetMapping("/connection/tools/list")
    public Mono<R<List<String>>> listConnectionTools(@NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> R.ok(oauthMcpRuntimeService.listConnectionToolIds(id,
                header))));
    }

    @PreAuthorize("@perm.can('mcp', 'update')")
    @Operation(summary = "Refresh MCP Tool Catalog", description = "Refresh the MCP tool catalog from registered APIs")
    @PostMapping("/tool/catalog/refresh")
    public Mono<R<Integer>> refreshToolCatalog() {
        return async(() -> R.ok(oauthMcpRuntimeService.refreshToolCatalog()));
    }

    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Tool Catalog", description = "List MCP tool catalog entries")
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

    @PreAuthorize("@perm.can('mcp', 'list')")
    @Operation(summary = "List MCP Audit Log", description = "List MCP tool call audit entries for the caller's tenant")
    @PostMapping("/audit/list")
    public Mono<R<List<McpAuditCommand>>> listAuditLog(
            @RequestParam(value = "principalId", required = false) Long principalId,
            @RequestParam(value = "toolId", required = false) String toolId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "limit", required = false) Integer limit) {
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
