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
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationResponseVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpCallToolRequestDTO;
import io.github.pnoker.common.entity.dto.McpCallToolResponseDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import java.net.URI;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive MCP runtime boundary consumed by the gRPC server. */
public interface ReactiveOAuthMcpRuntimeService {
    Mono<Map<String, Object>> authorizationServerMetadata();

    Mono<Map<String, Object>> jwks();

    Mono<OAuthClientRegistrationResponseVO> registerClient(
            OAuthClientRegistrationBO request, RequestHeader.PrincipalHeader principalHeader);

    Flux<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principalHeader);

    Mono<URI> authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader);

    Mono<Map<String, Object>> token(Map<String, String> form, String authorizationHeader);

    Mono<McpIntrospectResponseDTO> introspect(String token);

    Mono<Map<String, Object>> revoke(Map<String, String> form, String authorizationHeader);

    Mono<Integer> refreshToolCatalog();

    Mono<List<McpConnectionVO>> listConnections(RequestHeader.PrincipalHeader principalHeader);

    Mono<McpConnectionVO> createConnection(
            McpConnectionAddBO connection, RequestHeader.PrincipalHeader principalHeader);

    Mono<Void> revokeConnection(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    Mono<Void> replaceConnectionTools(
            Long connectionId, List<String> toolIds, RequestHeader.PrincipalHeader principalHeader);

    Flux<String> listConnectionToolIds(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    Mono<McpToolListResponseDTO> listTools(String token);

    Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request);

    Mono<Void> audit(McpAuditCommandDTO command);
}
