package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bo.McpConnectionAddBO;
import io.github.pnoker.common.auth.entity.bo.OAuthClientRegistrationBO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationResponseVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.auth.exception.OAuthProtocolException;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpCallToolRequestDTO;
import io.github.pnoker.common.entity.dto.McpCallToolResponseDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.service.ReactiveMcpCatalogService;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Reactive MCP runtime boundary consumed by the gRPC server. */
public interface ReactiveOAuthMcpRuntimeService {
    Mono<Map<String, Object>> authorizationServerMetadata();
    Mono<Map<String, Object>> jwks();
    Mono<OAuthClientRegistrationResponseVO> registerClient(OAuthClientRegistrationBO request,
                                                            RequestHeader.PrincipalHeader principalHeader);
    Flux<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principalHeader);
    Mono<URI> authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader);
    Mono<Map<String, Object>> token(Map<String, String> form, String authorizationHeader);
    Mono<McpIntrospectResponseDTO> introspect(String token);
    Mono<Map<String, Object>> revoke(Map<String, String> form, String authorizationHeader);
    Mono<Integer> refreshToolCatalog();
    Mono<List<McpConnectionVO>> listConnections(RequestHeader.PrincipalHeader principalHeader);
    Mono<McpConnectionVO> createConnection(McpConnectionAddBO connection,
                                            RequestHeader.PrincipalHeader principalHeader);
    Mono<Void> revokeConnection(Long connectionId, RequestHeader.PrincipalHeader principalHeader);
    Mono<Void> replaceConnectionTools(Long connectionId, List<String> toolIds,
                                      RequestHeader.PrincipalHeader principalHeader);
    Flux<String> listConnectionToolIds(Long connectionId, RequestHeader.PrincipalHeader principalHeader);
    Mono<McpToolListResponseDTO> listTools(String token);
    Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request);
    Mono<Void> audit(McpAuditCommandDTO command);
}
