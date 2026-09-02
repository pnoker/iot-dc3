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
package io.github.pnoker.common.gateway.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class McpGatewayControllerTest {

    @Mock
    private McpGatewayController.McpGatewayClient mcpGatewayClient;

    private McpGatewayController controller;

    @BeforeEach
    void setUp() {
        McpGatewayProperties properties = new McpGatewayProperties();
        properties.setResource("https://gateway.example/mcp");
        properties.setAuthorizationServer("https://gateway.example");
        properties.setBackendBaseUrls(new LinkedHashMap<>());
        controller = new McpGatewayController(mcpGatewayClient, properties);
    }

    @Test
    void protectedResourceMetadataAdvertisesBearerResourceServer() {
        Map<String, Object> metadata = controller.protectedResourceMetadata().block();

        assertThat(metadata).isNotNull();
        assertThat(metadata.get("bearer_methods_supported")).asList().containsExactly("header");
        assertThat(metadata.get("scopes_supported")).asList().contains("mcp:tools:list", "mcp:tools:call");
    }

    @Test
    void mcpReturnsBearerChallengeWhenAuthorizationHeaderIsMissing() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/mcp").build());

        var response = controller
                .mcp(Map.of("jsonrpc", "2.0", "method", "initialize", "id", 1), exchange)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).contains("resource_metadata");
        verifyNoInteractions(mcpGatewayClient);
    }

    @Test
    void initializeReturnsMcpServerCapabilitiesForActiveToken() {
        MockServerWebExchange exchange = authorizedExchange();
        when(mcpGatewayClient.validateToken("token")).thenReturn(Mono.empty());
        var response = controller
                .mcp(Map.of("jsonrpc", "2.0", "method", "initialize", "id", 7), exchange)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("jsonrpc", "2.0").containsEntry("id", 7);
        assertThat(castMap(response.getBody().get("result"))).containsEntry("protocolVersion", "2025-06-18");
        verify(mcpGatewayClient).validateToken("token");
    }

    @Test
    void toolsListDelegatesToAuthFilteredToolList() {
        MockServerWebExchange exchange = authorizedExchange();
        McpToolListResponseDTO tools = McpToolListResponseDTO.builder()
                .tools(List.of(
                        McpToolDefinitionDTO.builder().name("auth_user_get").build()))
                .build();
        when(mcpGatewayClient.listTools("token")).thenReturn(Mono.just(tools));

        var response = controller
                .mcp(Map.of("jsonrpc", "2.0", "method", "tools/list", "id", 9), exchange)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getBody().get("result")).isSameAs(tools);
    }

    @Test
    void toolsCallUsesAtomicFacadeRequest() {
        MockServerWebExchange exchange = authorizedExchange();
        when(mcpGatewayClient.callTool(eq("token"), eq("restart_device"), anyMap(), anyMap(), any()))
                .thenReturn(Mono.just(Map.of("content", List.of(Map.of("type", "text", "text", "{}")))));
        Map<String, Object> request = Map.of(
                "jsonrpc",
                "2.0",
                "method",
                "tools/call",
                "id",
                10,
                "params",
                Map.of(
                        "name",
                        "restart_device",
                        "arguments",
                        Map.of("deviceId", 3L),
                        "_meta",
                        Map.of("confirm_id", "confirm-1", "idempotency_key", "idem-1")));
        var response = controller.mcp(request, exchange).block();
        verify(mcpGatewayClient)
                .callTool(eq("token"), eq("restart_device"), anyMap(), anyMap(), any(ServerWebExchange.class));
        assertThat(response).isNotNull();
    }

    private MockServerWebExchange authorizedExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/mcp")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
