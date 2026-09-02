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

import io.github.pnoker.common.auth.biz.ReactiveOAuthMcpRuntimeService;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpCallToolRequestDTO;
import io.github.pnoker.common.entity.dto.McpCallToolResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.facade.api.McpRuntimeFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** In-process reactive MCP runtime facade. */
@Component
@RequiredArgsConstructor
public class McpRuntimeLocalFacade implements McpRuntimeFacade {
    private final ReactiveOAuthMcpRuntimeService service;

    @Override
    public Mono<McpToolListResponseDTO> listTools(String token) {
        return service.listTools(token);
    }

    @Override
    public Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request) {
        return service.callTool(request);
    }

    @Override
    public Mono<Void> audit(McpAuditCommandDTO command) {
        return service.audit(command);
    }
}
