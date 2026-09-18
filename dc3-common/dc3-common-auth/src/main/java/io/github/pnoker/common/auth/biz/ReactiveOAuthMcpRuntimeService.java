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
    /** Emit the OAuth 2.1 authorization server metadata document. */
    Mono<Map<String, Object>> authorizationServerMetadata();

    /** Emit the JSON Web Key Set used to verify issued tokens. */
    Mono<Map<String, Object>> jwks();

    /** Register one OAuth client through dynamic client registration. */
    Mono<OAuthClientRegistrationResponseVO> registerClient(
            OAuthClientRegistrationBO request, RequestHeader.PrincipalHeader principalHeader);

    /** Stream clients matching the request. */
    Flux<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principalHeader);

    /** Start the authorization-code flow and emit the redirect URI. */
    Mono<URI> authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader);

    /** Exchange an authorization grant for tokens on the token endpoint. */
    Mono<Map<String, Object>> token(Map<String, String> form, String authorizationHeader);

    /** Introspect a bearer token and emit its MCP principal claims. */
    Mono<McpIntrospectResponseDTO> introspect(String token);

    /** Revoke a token on the revocation endpoint. */
    Mono<Map<String, Object>> revoke(Map<String, String> form, String authorizationHeader);

    /** Reload the MCP tool catalog from registered gateways, emitting the number of refreshed tools. */
    Mono<Integer> refreshToolCatalog();

    /** List connections matching the request. */
    Mono<List<McpConnectionVO>> listConnections(RequestHeader.PrincipalHeader principalHeader);

    /** Create one MCP connection for the principal. */
    Mono<McpConnectionVO> createConnection(
            McpConnectionAddBO connection, RequestHeader.PrincipalHeader principalHeader);

    /** Revoke one MCP connection and its bound tokens. */
    Mono<Void> revokeConnection(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    /** Replace the tools bound to an MCP connection. */
    Mono<Void> replaceConnectionTools(
            Long connectionId, List<String> toolIds, RequestHeader.PrincipalHeader principalHeader);

    /** Stream connection tool ids matching the request. */
    Flux<String> listConnectionToolIds(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    /** List tools matching the request. */
    Mono<McpToolListResponseDTO> listTools(String token);

    /** Authorize and execute one MCP tool call. */
    Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request);

    /** Persist one MCP tool-call audit record. */
    Mono<Void> audit(McpAuditCommandDTO command);
}
