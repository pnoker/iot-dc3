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
package io.github.pnoker.common.auth.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.common.auth.biz.ReactiveOAuthMcpRuntimeService;
import io.github.pnoker.common.entity.dto.*;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class McpRuntimeServerTest {
    private final ReactiveOAuthMcpRuntimeService service = mock(ReactiveOAuthMcpRuntimeService.class);
    private io.grpc.Server server;
    private ManagedChannel channel;
    private McpRuntimeApiGrpc.McpRuntimeApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "mcp-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new McpRuntimeServer(service))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = McpRuntimeApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void listToolsUsesBearerToken() {
        when(service.listTools("token"))
                .thenReturn(Mono.just(McpToolListResponseDTO.builder()
                        .tools(List.of(McpToolDefinitionDTO.builder()
                                .name("ping")
                                .inputSchema(Map.of("type", "object"))
                                .build()))
                        .build()));
        GrpcMcpToolListDTO response = stub.listTools(
                GrpcMcpListToolsRequest.newBuilder().setToken("token").build());
        assertThat(response.getToolsCount()).isEqualTo(1);
        verify(service).listTools("token");
    }

    @Test
    void callToolReturnsDecisionAndPrincipal() {
        McpCallToolResponseDTO value = McpCallToolResponseDTO.builder()
                .decision("AUTHORIZED")
                .riskLevel("LOW")
                .tool(McpToolResolveResponseDTO.builder()
                        .toolName("ping")
                        .serviceName("svc")
                        .apiPath("/ping")
                        .httpMethod("GET")
                        .build())
                .principal(McpPrincipalContextDTO.builder()
                        .tenantId(1L)
                        .principalId(2L)
                        .principalType("USER")
                        .build())
                .build();
        when(service.callTool(any())).thenReturn(Mono.just(value));
        GrpcMcpCallToolDTO response = stub.callTool(GrpcMcpCallToolRequest.newBuilder()
                .setToken("token")
                .setToolName("ping")
                .build());
        assertThat(response.getDecision()).isEqualTo(GrpcMcpDecision.AUTHORIZED);
        assertThat(response.getPrincipal().getTenantId()).isEqualTo(1L);
    }
}
