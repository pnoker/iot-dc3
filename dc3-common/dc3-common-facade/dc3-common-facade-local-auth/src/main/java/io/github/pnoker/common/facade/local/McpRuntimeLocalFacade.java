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
    @Override public Mono<McpToolListResponseDTO> listTools(String token) { return service.listTools(token); }
    @Override public Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request) { return service.callTool(request); }
    @Override public Mono<Void> audit(McpAuditCommandDTO command) { return service.audit(command); }
}
