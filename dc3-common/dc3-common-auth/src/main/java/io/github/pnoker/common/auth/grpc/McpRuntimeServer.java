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

import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.biz.impl.OAuthMcpRuntimeServiceImpl.OAuthProtocolException;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * gRPC server for gateway-to-auth MCP runtime calls.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpRuntimeServer extends McpRuntimeApiGrpc.McpRuntimeApiImplBase {

    private final OAuthMcpRuntimeService oauthMcpRuntimeService;

    @Override
    public void introspect(GrpcMcpIntrospectRequest request,
                           StreamObserver<GrpcRMcpIntrospectDTO> responseObserver) {
        GrpcRMcpIntrospectDTO.Builder response = GrpcRMcpIntrospectDTO.newBuilder();
        try {
            response.setResult(ok());
            response.setData(toGrpc(oauthMcpRuntimeService.introspect(request.getToken())));
        } catch (Exception e) {
            log.warn("MCP introspect failed", e);
            response.setResult(failure(e));
            response.setData(toGrpc(McpIntrospectResponseDTO.inactive()));
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listTools(GrpcMcpToolListRequest request,
                          StreamObserver<GrpcRMcpToolListDTO> responseObserver) {
        GrpcRMcpToolListDTO.Builder response = GrpcRMcpToolListDTO.newBuilder();
        try {
            response.setResult(ok());
            // PublicEndpoint (McpGatewayController.mcp via gRPC): no tenant interceptor on the
            // gateway path; queries carry an explicit tenant_id argument, so bypass tenant-line
            // filtering here. The management controller calls the service directly and is unaffected.
            TenantContextHolder.runIgnoreAction(() ->
                    oauthMcpRuntimeService.listVisibleTools(request.getTenantId(), request.getPrincipalId(),
                                    request.getMcpConnectionId(), scopes(request.getScope()))
                            .forEach(tool -> response.addTools(toGrpc(tool))));
        } catch (OAuthProtocolException e) {
            response.setResult(protocolFailure(e));
        } catch (Exception e) {
            log.warn("MCP list tools failed", e);
            response.setResult(failure(e));
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void resolveTool(GrpcMcpToolResolveRequest request,
                            StreamObserver<GrpcRMcpToolResolveDTO> responseObserver) {
        GrpcRMcpToolResolveDTO.Builder response = GrpcRMcpToolResolveDTO.newBuilder();
        try {
            // PublicEndpoint (McpGatewayController.mcp via gRPC): see listTools above.
            McpToolResolveResponseDTO tool = TenantContextHolder.runIgnore(() ->
                    oauthMcpRuntimeService.resolveVisibleTool(request.getTenantId(),
                            request.getPrincipalId(), request.getMcpConnectionId(), request.getToolName(),
                            scopes(request.getScope())));
            response.setResult(ok());
            response.setData(toGrpc(tool));
        } catch (OAuthProtocolException e) {
            response.setResult(protocolFailure(e));
        } catch (Exception e) {
            log.warn("MCP resolve tool failed", e);
            response.setResult(failure(e));
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void authorizeToolCall(GrpcMcpToolAuthorizeRequest request,
                                  StreamObserver<GrpcRMcpToolAuthorizeDTO> responseObserver) {
        GrpcRMcpToolAuthorizeDTO.Builder response = GrpcRMcpToolAuthorizeDTO.newBuilder();
        try {
            McpToolAuthorizeRequestDTO requestDTO = McpToolAuthorizeRequestDTO.builder()
                    .tenantId(request.getTenantId())
                    .principalId(request.getPrincipalId())
                    .mcpConnectionId(request.getMcpConnectionId())
                    .scope(request.getScope())
                    .toolName(request.getToolName())
                    .argumentDigest(request.getArgumentDigest())
                    .confirmId(request.getConfirmId())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();
            // PublicEndpoint (McpGatewayController.mcp via gRPC): see listTools above.
            McpToolAuthorizeResponseDTO decision = TenantContextHolder.runIgnore(() ->
                    oauthMcpRuntimeService.authorizeToolCall(requestDTO));
            response.setResult(ok());
            response.setData(toGrpc(decision));
        } catch (OAuthProtocolException e) {
            response.setResult(protocolFailure(e));
        } catch (Exception e) {
            log.warn("MCP authorize tool call failed", e);
            response.setResult(failure(e));
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void audit(GrpcMcpAuditCommand request, StreamObserver<GrpcRMcpBoolean> responseObserver) {
        GrpcRMcpBoolean.Builder response = GrpcRMcpBoolean.newBuilder();
        try {
            // PublicEndpoint (McpGatewayController.mcp via gRPC): see listTools above.
            McpAuditCommandDTO command = toDTO(request);
            TenantContextHolder.runIgnoreAction(() -> oauthMcpRuntimeService.audit(command));
            response.setResult(ok());
            response.setData(true);
        } catch (Exception e) {
            log.warn("MCP audit failed", e);
            response.setResult(failure(e));
            response.setData(false);
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    /**
     * Convert an introspection response DTO to its gRPC form, defaulting nulls to
     * empty/zero.
     *
     * @param source the DTO
     * @return the gRPC introspection DTO
     */
    private GrpcMcpIntrospectDTO toGrpc(McpIntrospectResponseDTO source) {
        GrpcMcpIntrospectDTO.Builder builder = GrpcMcpIntrospectDTO.newBuilder()
                .setActive(source.isActive());
        if (source.getAud() != null) {
            builder.addAllAud(source.getAud());
        }
        return builder
                .setIss(StringUtils.defaultString(source.getIss()))
                .setSub(StringUtils.defaultString(source.getSub()))
                .setJti(StringUtils.defaultString(source.getJti()))
                .setExp(source.getExp() == null ? 0 : source.getExp())
                .setIat(source.getIat() == null ? 0 : source.getIat())
                .setTenantId(source.getTenantId() == null ? 0 : source.getTenantId())
                .setPrincipalId(source.getPrincipalId() == null ? 0 : source.getPrincipalId())
                .setPrincipalType(StringUtils.defaultString(source.getPrincipalType()))
                .setPrincipalName(StringUtils.defaultString(source.getPrincipalName()))
                .setDisplayName(StringUtils.defaultString(source.getDisplayName()))
                .setClientId(StringUtils.defaultString(source.getClientId()))
                .setMcpConnectionId(source.getMcpConnectionId() == null ? 0 : source.getMcpConnectionId())
                .setGrantType(StringUtils.defaultString(source.getGrantType()))
                .setScope(StringUtils.defaultString(source.getScope()))
                .build();
    }

    /**
     * Convert a tool definition DTO to its gRPC form, including annotations and metadata.
     *
     * @param source the DTO
     * @return the gRPC tool definition DTO
     */
    private GrpcMcpToolDefinitionDTO toGrpc(McpToolDefinitionDTO source) {
        return GrpcMcpToolDefinitionDTO.newBuilder()
                .setName(StringUtils.defaultString(source.getName()))
                .setTitle(StringUtils.defaultString(source.getTitle()))
                .setDescription(StringUtils.defaultString(source.getDescription()))
                .setAnnotations(toGrpc(source.getAnnotations()))
                .setMeta(toGrpc(source.getMeta()))
                .build();
    }

    /**
     * Convert tool annotation hints to their gRPC form, defaulting null to empty.
     *
     * @param source the annotations
     * @return the gRPC annotations DTO
     */
    private GrpcMcpToolAnnotationsDTO toGrpc(McpToolDefinitionDTO.Annotations source) {
        if (source == null) {
            return GrpcMcpToolAnnotationsDTO.getDefaultInstance();
        }
        return GrpcMcpToolAnnotationsDTO.newBuilder()
                .setReadOnlyHint(source.isReadOnlyHint())
                .setDestructiveHint(source.isDestructiveHint())
                .setIdempotentHint(source.isIdempotentHint())
                .setOpenWorldHint(source.isOpenWorldHint())
                .build();
    }

    /**
     * Convert tool metadata (tool id, permission code, risk level) to its gRPC form,
     * defaulting null to empty.
     *
     * @param source the metadata
     * @return the gRPC metadata DTO
     */
    private GrpcMcpToolMetadataDTO toGrpc(McpToolDefinitionDTO.Metadata source) {
        if (source == null) {
            return GrpcMcpToolMetadataDTO.getDefaultInstance();
        }
        return GrpcMcpToolMetadataDTO.newBuilder()
                .setToolId(StringUtils.defaultString(source.getToolId()))
                .setPermissionCode(StringUtils.defaultString(source.getPermissionCode()))
                .setRiskLevel(StringUtils.defaultString(source.getRiskLevel()))
                .build();
    }

    /**
     * Convert a resolved tool response DTO to its gRPC form.
     *
     * @param source the DTO
     * @return the gRPC resolved tool DTO
     */
    private GrpcMcpToolResolveDTO toGrpc(McpToolResolveResponseDTO source) {
        return GrpcMcpToolResolveDTO.newBuilder()
                .setToolId(StringUtils.defaultString(source.getToolId()))
                .setToolName(StringUtils.defaultString(source.getToolName()))
                .setPermissionCode(StringUtils.defaultString(source.getPermissionCode()))
                .setRiskLevel(StringUtils.defaultString(source.getRiskLevel()))
                .setServiceName(StringUtils.defaultString(source.getServiceName()))
                .setApiPath(StringUtils.defaultString(source.getApiPath()))
                .setHttpMethod(StringUtils.defaultString(source.getHttpMethod()))
                .build();
    }

    /**
     * Convert an authorization decision DTO to its gRPC form.
     *
     * @param source the DTO
     * @return the gRPC authorization DTO
     */
    private GrpcMcpToolAuthorizeDTO toGrpc(McpToolAuthorizeResponseDTO source) {
        return GrpcMcpToolAuthorizeDTO.newBuilder()
                .setDecision(StringUtils.defaultString(source.getDecision()))
                .setConfirmId(StringUtils.defaultString(source.getConfirmId()))
                .setMessage(StringUtils.defaultString(source.getMessage()))
                .setRiskLevel(StringUtils.defaultString(source.getRiskLevel()))
                .build();
    }

    /**
     * Convert a gRPC audit command to its DTO form.
     *
     * @param source the gRPC audit command
     * @return the audit command DTO
     */
    private McpAuditCommandDTO toDTO(GrpcMcpAuditCommand source) {
        return McpAuditCommandDTO.builder()
                .traceId(source.getTraceId())
                .tenantId(source.getTenantId())
                .principalId(source.getPrincipalId())
                .principalType(source.getPrincipalType())
                .clientId(source.getClientId())
                .connectionId(source.getConnectionId())
                .toolId(source.getToolId())
                .toolName(source.getToolName())
                .permissionCode(source.getPermissionCode())
                .riskLevel(source.getRiskLevel())
                .confirmId(source.getConfirmId())
                .idempotencyKey(source.getIdempotencyKey())
                .argumentDigest(source.getArgumentDigest())
                .status(source.getStatus())
                .errorCode(source.getErrorCode())
                .durationMs(source.getDurationMs())
                .clientName(source.getClientName())
                .clientVersion(source.getClientVersion())
                .remoteIp(source.getRemoteIp())
                .build();
    }

    /**
     * Parse a space-delimited scope string into a set, returning empty for blank input.
     *
     * @param value the raw scope string
     * @return the parsed scope set
     */
    private Set<String> scopes(String value) {
        if (StringUtils.isBlank(value)) {
            return Set.of();
        }
        return Arrays.stream(value.trim().split(McpConstant.Scope.DELIMITER_REGEX))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private GrpcR ok() {
        return GrpcRFactory.ok();
    }

    private GrpcR protocolFailure(OAuthProtocolException exception) {
        return GrpcR.newBuilder()
                .setOk(false)
                .setCode(exception.getError())
                .setMessage(exception.getDescription())
                .build();
    }

    private GrpcR failure(Exception exception) {
        return StringUtils.isBlank(exception.getMessage())
                ? GrpcRFactory.fail(ErrorCode.FAILURE)
                : GrpcRFactory.fail(ErrorCode.FAILURE, exception.getMessage());
    }

}
