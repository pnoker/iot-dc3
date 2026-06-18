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

package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.McpRuntimeFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * gRPC {@link McpRuntimeFacade}.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpRuntimeGrpcFacade implements McpRuntimeFacade {

    private final McpRuntimeApiGrpc.McpRuntimeApiBlockingStub mcpRuntimeApiBlockingStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public McpIntrospectResponseDTO introspect(String token) {
        GrpcMcpIntrospectRequest request = GrpcMcpIntrospectRequest.newBuilder()
                .setToken(StringUtils.defaultString(token))
                .build();
        GrpcRMcpIntrospectDTO response = grpcFacadeSupport.call("McpRuntimeFacade.introspect",
                mcpRuntimeApiBlockingStub, stub -> stub.introspect(request));
        requireOk("McpRuntimeFacade.introspect", response.getResult());
        return response.hasData() ? toDTO(response.getData()) : McpIntrospectResponseDTO.inactive();
    }

    @Override
    public McpToolListResponseDTO listTools(Long tenantId, Long principalId, Long mcpConnectionId, String scope) {
        GrpcMcpToolListRequest request = GrpcMcpToolListRequest.newBuilder()
                .setTenantId(value(tenantId))
                .setPrincipalId(value(principalId))
                .setMcpConnectionId(value(mcpConnectionId))
                .setScope(StringUtils.defaultString(scope))
                .build();
        GrpcRMcpToolListDTO response = grpcFacadeSupport.call("McpRuntimeFacade.listTools",
                mcpRuntimeApiBlockingStub, stub -> stub.listTools(request));
        requireOk("McpRuntimeFacade.listTools", response.getResult());
        return McpToolListResponseDTO.builder()
                .tools(response.getToolsList().stream().map(this::toDTO).toList())
                .build();
    }

    @Override
    public McpToolResolveResponseDTO resolveTool(Long tenantId, Long principalId, Long mcpConnectionId, String scope,
                                                 String toolName) {
        GrpcMcpToolResolveRequest request = GrpcMcpToolResolveRequest.newBuilder()
                .setTenantId(value(tenantId))
                .setPrincipalId(value(principalId))
                .setMcpConnectionId(value(mcpConnectionId))
                .setScope(StringUtils.defaultString(scope))
                .setToolName(StringUtils.defaultString(toolName))
                .build();
        GrpcRMcpToolResolveDTO response = grpcFacadeSupport.call("McpRuntimeFacade.resolveTool",
                mcpRuntimeApiBlockingStub, stub -> stub.resolveTool(request));
        requireOk("McpRuntimeFacade.resolveTool", response.getResult());
        return response.hasData() ? toDTO(response.getData()) : new McpToolResolveResponseDTO();
    }

    @Override
    public McpToolAuthorizeResponseDTO authorizeToolCall(McpToolAuthorizeRequestDTO request) {
        request = request == null ? new McpToolAuthorizeRequestDTO() : request;
        GrpcMcpToolAuthorizeRequest grpcRequest = GrpcMcpToolAuthorizeRequest.newBuilder()
                .setTenantId(value(request.getTenantId()))
                .setPrincipalId(value(request.getPrincipalId()))
                .setMcpConnectionId(value(request.getMcpConnectionId()))
                .setScope(StringUtils.defaultString(request.getScope()))
                .setToolName(StringUtils.defaultString(request.getToolName()))
                .setArgumentDigest(StringUtils.defaultString(request.getArgumentDigest()))
                .setConfirmId(StringUtils.defaultString(request.getConfirmId()))
                .setIdempotencyKey(StringUtils.defaultString(request.getIdempotencyKey()))
                .build();
        GrpcRMcpToolAuthorizeDTO response = grpcFacadeSupport.call("McpRuntimeFacade.authorizeToolCall",
                mcpRuntimeApiBlockingStub, stub -> stub.authorizeToolCall(grpcRequest));
        requireOk("McpRuntimeFacade.authorizeToolCall", response.getResult());
        return response.hasData() ? toDTO(response.getData()) : new McpToolAuthorizeResponseDTO();
    }

    @Override
    public void audit(McpAuditCommandDTO command) {
        GrpcRMcpBoolean response = grpcFacadeSupport.call("McpRuntimeFacade.audit", mcpRuntimeApiBlockingStub,
                stub -> stub.audit(toGrpc(command)));
        requireOk("McpRuntimeFacade.audit", response.getResult());
    }

    private McpToolAuthorizeResponseDTO toDTO(GrpcMcpToolAuthorizeDTO source) {
        return McpToolAuthorizeResponseDTO.builder()
                .decision(source.getDecision())
                .confirmId(source.getConfirmId())
                .message(source.getMessage())
                .riskLevel(source.getRiskLevel())
                .build();
    }

    private McpIntrospectResponseDTO toDTO(GrpcMcpIntrospectDTO source) {
        return McpIntrospectResponseDTO.builder()
                .active(source.getActive())
                .iss(source.getIss())
                .aud(new HashSet<>(source.getAudList()))
                .sub(source.getSub())
                .jti(source.getJti())
                .exp(zeroToNull(source.getExp()))
                .iat(zeroToNull(source.getIat()))
                .tenantId(zeroToNull(source.getTenantId()))
                .principalId(zeroToNull(source.getPrincipalId()))
                .principalType(source.getPrincipalType())
                .principalName(source.getPrincipalName())
                .displayName(source.getDisplayName())
                .clientId(source.getClientId())
                .mcpConnectionId(zeroToNull(source.getMcpConnectionId()))
                .grantType(source.getGrantType())
                .scope(source.getScope())
                .build();
    }

    private McpToolDefinitionDTO toDTO(GrpcMcpToolDefinitionDTO source) {
        return McpToolDefinitionDTO.builder()
                .name(source.getName())
                .title(source.getTitle())
                .description(source.getDescription())
                .inputSchema(McpConstant.ToolDefinition.DEFAULT_INPUT_SCHEMA)
                .annotations(toDTO(source.getAnnotations()))
                .meta(toDTO(source.getMeta()))
                .build();
    }

    private McpToolDefinitionDTO.Annotations toDTO(GrpcMcpToolAnnotationsDTO source) {
        return McpToolDefinitionDTO.Annotations.builder()
                .readOnlyHint(source.getReadOnlyHint())
                .destructiveHint(source.getDestructiveHint())
                .idempotentHint(source.getIdempotentHint())
                .openWorldHint(source.getOpenWorldHint())
                .build();
    }

    private McpToolDefinitionDTO.Metadata toDTO(GrpcMcpToolMetadataDTO source) {
        return McpToolDefinitionDTO.Metadata.builder()
                .toolId(source.getToolId())
                .permissionCode(source.getPermissionCode())
                .riskLevel(source.getRiskLevel())
                .build();
    }

    private McpToolResolveResponseDTO toDTO(GrpcMcpToolResolveDTO source) {
        return McpToolResolveResponseDTO.builder()
                .toolId(source.getToolId())
                .toolName(source.getToolName())
                .permissionCode(source.getPermissionCode())
                .riskLevel(source.getRiskLevel())
                .serviceName(source.getServiceName())
                .apiPath(source.getApiPath())
                .httpMethod(source.getHttpMethod())
                .build();
    }

    private GrpcMcpAuditCommand toGrpc(McpAuditCommandDTO source) {
        source = source == null ? new McpAuditCommandDTO() : source;
        return GrpcMcpAuditCommand.newBuilder()
                .setTraceId(StringUtils.defaultString(source.getTraceId()))
                .setTenantId(value(source.getTenantId()))
                .setPrincipalId(value(source.getPrincipalId()))
                .setPrincipalType(StringUtils.defaultString(source.getPrincipalType()))
                .setClientId(StringUtils.defaultString(source.getClientId()))
                .setConnectionId(value(source.getConnectionId()))
                .setToolId(StringUtils.defaultString(source.getToolId()))
                .setToolName(StringUtils.defaultString(source.getToolName()))
                .setPermissionCode(StringUtils.defaultString(source.getPermissionCode()))
                .setRiskLevel(StringUtils.defaultString(source.getRiskLevel()))
                .setConfirmId(StringUtils.defaultString(source.getConfirmId()))
                .setIdempotencyKey(StringUtils.defaultString(source.getIdempotencyKey()))
                .setArgumentDigest(StringUtils.defaultString(source.getArgumentDigest()))
                .setStatus(StringUtils.defaultString(source.getStatus()))
                .setErrorCode(StringUtils.defaultString(source.getErrorCode()))
                .setDurationMs(value(source.getDurationMs()))
                .setClientName(StringUtils.defaultString(source.getClientName()))
                .setClientVersion(StringUtils.defaultString(source.getClientVersion()))
                .setRemoteIp(StringUtils.defaultString(source.getRemoteIp()))
                .build();
    }

    private Long zeroToNull(long value) {
        return value == 0 ? null : value;
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private void requireOk(String operation, GrpcR result) {
        if (!result.getOk()) {
            throw new ServiceException(operation + " failed: [" + result.getCode() + "] " + result.getMessage());
        }
    }

}
