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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.facade.api.McpRuntimeFacade;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-process MCP runtime facade for single-center deployments.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Component
@RequiredArgsConstructor
public class McpRuntimeLocalFacade implements McpRuntimeFacade {

    private final OAuthMcpRuntimeService oauthMcpRuntimeService;

    @Override
    public McpIntrospectResponseDTO introspect(String token) {
        return oauthMcpRuntimeService.introspect(token);
    }

    @Override
    public McpToolListResponseDTO listTools(Long tenantId, Long principalId, Long mcpConnectionId, String scope) {
        return McpToolListResponseDTO.builder()
                .tools(oauthMcpRuntimeService.listVisibleTools(tenantId, principalId, mcpConnectionId, scopes(scope)))
                .build();
    }

    @Override
    public McpToolResolveResponseDTO resolveTool(Long tenantId, Long principalId, Long mcpConnectionId, String scope,
                                                 String toolName) {
        return oauthMcpRuntimeService.resolveVisibleTool(tenantId, principalId, mcpConnectionId, toolName,
                scopes(scope));
    }

    @Override
    public void audit(McpAuditCommandDTO command) {
        oauthMcpRuntimeService.audit(command);
    }

    private Set<String> scopes(String value) {
        if (StringUtils.isBlank(value)) {
            return Set.of();
        }
        return Arrays.stream(value.trim().split(McpConstant.Scope.DELIMITER_REGEX))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

}
