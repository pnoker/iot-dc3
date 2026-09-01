package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpCallToolRequestDTO;
import io.github.pnoker.common.entity.dto.McpCallToolResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import reactor.core.publisher.Mono;

/** Reactive MCP runtime facade used by the gateway. */
public interface McpRuntimeFacade {
    Mono<McpToolListResponseDTO> listTools(String token);
    Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request);
    Mono<Void> audit(McpAuditCommandDTO command);
}
