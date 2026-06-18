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

package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;

/**
 * Protocol-neutral MCP runtime facade used by gateway-to-auth internal calls.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface McpRuntimeFacade {

    /**
     * Introspect an MCP OAuth bearer token.
     *
     * @param token bearer token
     * @return introspection result
     */
    McpIntrospectResponseDTO introspect(String token);

    /**
     * List tools visible to the current MCP connection.
     *
     * @param tenantId        tenant ID
     * @param principalId     principal ID
     * @param mcpConnectionId MCP connection ID
     * @param scope           granted OAuth scopes
     * @return visible MCP tools
     */
    McpToolListResponseDTO listTools(Long tenantId, Long principalId, Long mcpConnectionId, String scope);

    /**
     * Resolve one visible MCP tool to backend invocation metadata.
     *
     * @param tenantId        tenant ID
     * @param principalId     principal ID
     * @param mcpConnectionId MCP connection ID
     * @param scope           granted OAuth scopes
     * @param toolName        MCP tool name
     * @return backend invocation metadata
     */
    McpToolResolveResponseDTO resolveTool(Long tenantId, Long principalId, Long mcpConnectionId, String scope,
                                          String toolName);

    /**
     * Authorize one tool call, enforcing high-risk confirmation and idempotency.
     *
     * @param request authorization request
     * @return authorization decision
     */
    McpToolAuthorizeResponseDTO authorizeToolCall(McpToolAuthorizeRequestDTO request);

    /**
     * Store one MCP call audit record.
     *
     * @param command audit command
     */
    void audit(McpAuditCommandDTO command);

}
