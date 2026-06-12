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

import io.github.pnoker.api.center.auth.GrpcMcpAuditCommand;
import io.github.pnoker.api.center.auth.GrpcMcpIntrospectRequest;
import io.github.pnoker.api.center.auth.GrpcMcpToolListRequest;
import io.github.pnoker.api.center.auth.GrpcMcpToolResolveRequest;
import io.github.pnoker.api.center.auth.GrpcRMcpBoolean;
import io.github.pnoker.api.center.auth.GrpcRMcpIntrospectDTO;
import io.github.pnoker.api.center.auth.GrpcRMcpToolListDTO;
import io.github.pnoker.api.center.auth.GrpcRMcpToolResolveDTO;
import io.github.pnoker.api.center.auth.McpRuntimeApiGrpc;
import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpRuntimeServerTest {

    @Mock
    private OAuthMcpRuntimeService oauthMcpRuntimeService;

    private Server server;
    private ManagedChannel channel;
    private McpRuntimeApiGrpc.McpRuntimeApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        McpRuntimeServer mcpRuntimeServer = new McpRuntimeServer(oauthMcpRuntimeService);

        String name = "dc3-mcp-runtime-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(mcpRuntimeServer).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = McpRuntimeApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void introspectMapsActiveTokenContext() {
        when(oauthMcpRuntimeService.introspect("token")).thenReturn(McpIntrospectResponseDTO.builder()
                .active(true)
                .aud(Set.of("dc3-mcp"))
                .tenantId(1L)
                .principalId(100L)
                .principalType("USER")
                .principalName("admin")
                .clientId("dc3_client")
                .mcpConnectionId(300L)
                .scope("mcp:tools:list")
                .build());

        GrpcRMcpIntrospectDTO response = stub.introspect(GrpcMcpIntrospectRequest.newBuilder()
                .setToken("token")
                .build());

        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.OK.getCode());
        assertThat(response.getData().getActive()).isTrue();
        assertThat(response.getData().getTenantId()).isEqualTo(1L);
        assertThat(response.getData().getPrincipalId()).isEqualTo(100L);
        assertThat(response.getData().getAudList()).containsExactly("dc3-mcp");
    }

    @Test
    void listToolsMapsToolDefinition() {
        McpToolDefinitionDTO tool = McpToolDefinitionDTO.builder()
                .name("auth_user_get")
                .title("List users")
                .description("List users")
                .annotations(McpToolDefinitionDTO.Annotations.builder()
                        .readOnlyHint(true)
                        .idempotentHint(true)
                        .build())
                .meta(McpToolDefinitionDTO.Metadata.builder()
                        .toolId("auth:GET:/api/v3/auth/user")
                        .permissionCode("auth:user:select")
                        .riskLevel("LOW")
                        .build())
                .build();
        when(oauthMcpRuntimeService.listVisibleTools(1L, 100L, 300L, Set.of("mcp:tools:list")))
                .thenReturn(List.of(tool));

        GrpcRMcpToolListDTO response = stub.listTools(GrpcMcpToolListRequest.newBuilder()
                .setTenantId(1L)
                .setPrincipalId(100L)
                .setMcpConnectionId(300L)
                .setScope("mcp:tools:list")
                .build());

        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getToolsList()).hasSize(1);
        assertThat(response.getTools(0).getName()).isEqualTo("auth_user_get");
        assertThat(response.getTools(0).getAnnotations().getReadOnlyHint()).isTrue();
        assertThat(response.getTools(0).getMeta().getPermissionCode()).isEqualTo("auth:user:select");
    }

    @Test
    void resolveToolMapsInvocationMetadata() {
        when(oauthMcpRuntimeService.resolveVisibleTool(1L, 100L, 300L, "restart_device",
                Set.of("mcp:tools:call"))).thenReturn(McpToolResolveResponseDTO.builder()
                .toolId("manager:POST:/api/v3/manager/device/restart")
                .toolName("restart_device")
                .permissionCode("manager:device:update")
                .riskLevel("HIGH")
                .serviceName("dc3-center-manager")
                .apiPath("/api/v3/manager/device/restart")
                .httpMethod("POST")
                .build());

        GrpcRMcpToolResolveDTO response = stub.resolveTool(GrpcMcpToolResolveRequest.newBuilder()
                .setTenantId(1L)
                .setPrincipalId(100L)
                .setMcpConnectionId(300L)
                .setScope("mcp:tools:call")
                .setToolName("restart_device")
                .build());

        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getData().getRiskLevel()).isEqualTo("HIGH");
        assertThat(response.getData().getServiceName()).isEqualTo("dc3-center-manager");
        assertThat(response.getData().getApiPath()).isEqualTo("/api/v3/manager/device/restart");
    }

    @Test
    void auditMapsCommand() {
        GrpcRMcpBoolean response = stub.audit(GrpcMcpAuditCommand.newBuilder()
                .setTraceId("trace-1")
                .setTenantId(1L)
                .setPrincipalId(100L)
                .setConnectionId(300L)
                .setToolId("tool-1")
                .setToolName("restart_device")
                .setStatus("SUCCESS")
                .setDurationMs(20L)
                .build());

        ArgumentCaptor<McpAuditCommandDTO> captor = ArgumentCaptor.forClass(McpAuditCommandDTO.class);
        verify(oauthMcpRuntimeService).audit(captor.capture());

        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getData()).isTrue();
        assertThat(captor.getValue().getTraceId()).isEqualTo("trace-1");
        assertThat(captor.getValue().getToolName()).isEqualTo("restart_device");
        assertThat(captor.getValue().getDurationMs()).isEqualTo(20L);
    }

}
