package io.github.pnoker.common.auth.grpc;

import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.common.auth.biz.ReactiveOAuthMcpRuntimeService;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpCallToolRequestDTO;
import io.github.pnoker.common.entity.dto.McpCallToolResponseDTO;
import io.github.pnoker.common.entity.dto.McpPrincipalContextDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.AccessDeniedException;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.grpc.stub.ServerCallStreamObserver;
import reactor.core.Disposable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Reactive gRPC server for MCP runtime decisions. */
@Service
@RequiredArgsConstructor
public class McpRuntimeServer extends McpRuntimeApiGrpc.McpRuntimeApiImplBase {
    private final ReactiveOAuthMcpRuntimeService service;

    @Override
    public void listTools(GrpcMcpListToolsRequest request, StreamObserver<GrpcMcpToolListDTO> observer) {
        subscribe(service.listTools(request.getToken()).map(this::toGrpc), observer);
    }

    @Override
    public void callTool(GrpcMcpCallToolRequest request, StreamObserver<GrpcMcpCallToolDTO> observer) {
        McpCallToolRequestDTO command = McpCallToolRequestDTO.builder().token(request.getToken()).toolName(request.getToolName())
                .argumentDigest(request.getArgumentDigest()).confirmId(request.getConfirmId()).idempotencyKey(request.getIdempotencyKey())
                .clientName(request.getClientName()).clientVersion(request.getClientVersion()).remoteIp(request.getRemoteIp()).build();
        subscribe(service.callTool(command).map(this::toGrpc), observer);
    }

    @Override
    public void audit(GrpcMcpAuditCommand request, StreamObserver<GrpcMcpBoolean> observer) {
        Mono<GrpcMcpBoolean> result = service.audit(McpAuditCommandDTO.builder().traceId(request.getTraceId()).tenantId(request.getTenantId()).principalId(request.getPrincipalId())
                .principalType(request.getPrincipalType().name()).clientId(request.getClientId()).connectionId(request.getConnectionId()).toolId(request.getToolId())
                .toolName(request.getToolName()).permissionCode(request.getPermissionCode()).riskLevel(request.getRiskLevel().name()).confirmId(request.getConfirmId())
                .idempotencyKey(request.getIdempotencyKey()).argumentDigest(request.getArgumentDigest()).status(request.getStatus().name()).errorCode(request.getErrorCode())
                .durationMs(request.getDurationMs()).clientName(request.getClientName()).clientVersion(request.getClientVersion()).remoteIp(request.getRemoteIp()).build())
                .thenReturn(GrpcMcpBoolean.newBuilder().setValue(true).build());
        subscribe(result, observer);
    }

    private <T> void subscribe(Mono<T> publisher, StreamObserver<T> observer) {
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        if (observer instanceof ServerCallStreamObserver<?> serverObserver) {
            serverObserver.setOnCancelHandler(() -> {
                Disposable disposable = subscription.get();
                if (disposable != null) disposable.dispose();
            });
        }
        Disposable disposable = publisher.subscribe(observer::onNext, error -> observer.onError(toStatus(error)), observer::onCompleted);
        subscription.set(disposable);
    }

    private GrpcMcpToolListDTO toGrpc(McpToolListResponseDTO value) {
        GrpcMcpToolListDTO.Builder result = GrpcMcpToolListDTO.newBuilder();
        if (value != null && value.getTools() != null) value.getTools().stream().map(this::toGrpc).forEach(result::addTools);
        return result.build();
    }
    private GrpcMcpToolDefinitionDTO toGrpc(McpToolDefinitionDTO value) {
        return GrpcMcpToolDefinitionDTO.newBuilder().setName(StringUtils.defaultString(value.getName())).setTitle(StringUtils.defaultString(value.getTitle()))
                .setDescription(StringUtils.defaultString(value.getDescription())).setInputSchema(io.github.pnoker.common.utils.JsonUtil.toJsonString(value.getInputSchema()))
                .setAnnotations(GrpcMcpToolAnnotationsDTO.newBuilder().setReadOnlyHint(value.getAnnotations() != null && value.getAnnotations().isReadOnlyHint()).setDestructiveHint(value.getAnnotations() != null && value.getAnnotations().isDestructiveHint()).setIdempotentHint(value.getAnnotations() != null && value.getAnnotations().isIdempotentHint()).setOpenWorldHint(value.getAnnotations() != null && value.getAnnotations().isOpenWorldHint()).build())
                .setMeta(GrpcMcpToolMetadataDTO.newBuilder().setToolId(value.getMeta() == null ? "" : StringUtils.defaultString(value.getMeta().getToolId())).setPermissionCode(value.getMeta() == null ? "" : StringUtils.defaultString(value.getMeta().getPermissionCode())).setRiskLevel(risk(value.getMeta() == null ? null : value.getMeta().getRiskLevel())).build()).build();
    }
    private GrpcMcpCallToolDTO toGrpc(McpCallToolResponseDTO value) {
        GrpcMcpCallToolDTO.Builder result = GrpcMcpCallToolDTO.newBuilder().setDecision(decision(value.getDecision())).setConfirmId(StringUtils.defaultString(value.getConfirmId())).setMessage(StringUtils.defaultString(value.getMessage())).setRiskLevel(risk(value.getRiskLevel()));
        if (value.getTool() != null) result.setTool(toGrpc(value.getTool()));
        if (value.getPrincipal() != null) result.setPrincipal(toGrpc(value.getPrincipal()));
        return result.build();
    }
    private GrpcMcpToolResolveDTO toGrpc(McpToolResolveResponseDTO value) {
        return GrpcMcpToolResolveDTO.newBuilder().setToolId(StringUtils.defaultString(value.getToolId())).setToolName(StringUtils.defaultString(value.getToolName())).setPermissionCode(StringUtils.defaultString(value.getPermissionCode())).setRiskLevel(risk(value.getRiskLevel())).setServiceName(StringUtils.defaultString(value.getServiceName())).setApiPath(StringUtils.defaultString(value.getApiPath())).setHttpMethod(StringUtils.defaultString(value.getHttpMethod())).setInputSchema(io.github.pnoker.common.utils.JsonUtil.toJsonString(value.getInputSchema())).build();
    }
    private GrpcMcpPrincipalContext toGrpc(McpPrincipalContextDTO value) { return GrpcMcpPrincipalContext.newBuilder().setTenantId(value.getTenantId() == null ? 0 : value.getTenantId()).setPrincipalId(value.getPrincipalId() == null ? 0 : value.getPrincipalId()).setPrincipalType(principal(value.getPrincipalType())).setPrincipalName(StringUtils.defaultString(value.getPrincipalName())).setDisplayName(StringUtils.defaultString(value.getDisplayName())).setClientId(StringUtils.defaultString(value.getClientId())).setConnectionId(value.getConnectionId() == null ? 0 : value.getConnectionId()).build(); }
    private GrpcMcpRiskLevel risk(String value) { try { return GrpcMcpRiskLevel.valueOf(StringUtils.defaultString(value)); } catch (Exception e) { return GrpcMcpRiskLevel.MCP_RISK_LEVEL_UNSPECIFIED; } }
    private GrpcMcpDecision decision(String value) { try { return GrpcMcpDecision.valueOf(StringUtils.defaultString(value)); } catch (Exception e) { return GrpcMcpDecision.MCP_DECISION_UNSPECIFIED; } }
    private GrpcMcpPrincipalType principal(String value) { try { return GrpcMcpPrincipalType.valueOf(StringUtils.defaultString(value)); } catch (Exception e) { return GrpcMcpPrincipalType.MCP_PRINCIPAL_TYPE_UNSPECIFIED; } }
    private RuntimeException toStatus(Throwable error) {
        String description = Objects.requireNonNullElse(error.getMessage(), error.getClass().getSimpleName());
        Status status;
        if (error instanceof NotFoundException) status = Status.NOT_FOUND;
        else if (error instanceof DuplicateException) status = Status.ALREADY_EXISTS;
        else if (error instanceof RequestException || error instanceof IllegalArgumentException) status = Status.INVALID_ARGUMENT;
        else if (error instanceof AccessDeniedException) status = Status.PERMISSION_DENIED;
        else if (error instanceof AssociatedException || error instanceof BusinessException) status = Status.FAILED_PRECONDITION;
        else status = Status.INTERNAL;
        return status.withDescription(description).withCause(error).asRuntimeException();
    }
}
