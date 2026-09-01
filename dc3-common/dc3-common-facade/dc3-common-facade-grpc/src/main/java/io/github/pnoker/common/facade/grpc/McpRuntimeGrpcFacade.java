package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.common.entity.dto.*;
import io.github.pnoker.common.facade.api.McpRuntimeFacade;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Reactive gRPC MCP runtime facade. */
@Component
@RequiredArgsConstructor
public class McpRuntimeGrpcFacade implements McpRuntimeFacade {
    private final McpRuntimeApiGrpc.McpRuntimeApiStub stub;

    @Override
    public Mono<McpToolListResponseDTO> listTools(String token) {
        return ReactiveGrpcClientSupport.<GrpcMcpListToolsRequest, GrpcMcpToolListDTO>unary(
                "McpRuntimeFacade.listTools", observer -> stub.listTools(
                        GrpcMcpListToolsRequest.newBuilder().setToken(StringUtils.defaultString(token)).build(), observer))
                .map(response -> McpToolListResponseDTO.builder().tools(response.getToolsList().stream().map(this::toTool).toList()).build());
    }

    @Override
    public Mono<McpCallToolResponseDTO> callTool(McpCallToolRequestDTO request) {
        McpCallToolRequestDTO value = request == null ? new McpCallToolRequestDTO() : request;
        GrpcMcpCallToolRequest grpc = GrpcMcpCallToolRequest.newBuilder()
                .setToken(StringUtils.defaultString(value.getToken()))
                .setToolName(StringUtils.defaultString(value.getToolName()))
                .setArgumentDigest(StringUtils.defaultString(value.getArgumentDigest()))
                .setConfirmId(StringUtils.defaultString(value.getConfirmId()))
                .setIdempotencyKey(StringUtils.defaultString(value.getIdempotencyKey()))
                .setClientName(StringUtils.defaultString(value.getClientName()))
                .setClientVersion(StringUtils.defaultString(value.getClientVersion()))
                .setRemoteIp(StringUtils.defaultString(value.getRemoteIp())).build();
        return ReactiveGrpcClientSupport.<GrpcMcpCallToolRequest, GrpcMcpCallToolDTO>unary(
                "McpRuntimeFacade.callTool", observer -> stub.callTool(grpc, observer))
                .map(this::toCall);
    }

    @Override
    public Mono<Void> audit(McpAuditCommandDTO command) {
        McpAuditCommandDTO value = command == null ? new McpAuditCommandDTO() : command;
        GrpcMcpAuditCommand grpc = GrpcMcpAuditCommand.newBuilder()
                .setTraceId(StringUtils.defaultString(value.getTraceId())).setTenantId(value(value.getTenantId()))
                .setPrincipalId(value(value.getPrincipalId())).setClientId(StringUtils.defaultString(value.getClientId()))
                .setConnectionId(value(value.getConnectionId())).setToolId(StringUtils.defaultString(value.getToolId()))
                .setToolName(StringUtils.defaultString(value.getToolName())).setPermissionCode(StringUtils.defaultString(value.getPermissionCode()))
                .setRiskLevel(risk(value.getRiskLevel())).setConfirmId(StringUtils.defaultString(value.getConfirmId()))
                .setIdempotencyKey(StringUtils.defaultString(value.getIdempotencyKey())).setArgumentDigest(StringUtils.defaultString(value.getArgumentDigest()))
                .setStatus(status(value.getStatus())).setErrorCode(StringUtils.defaultString(value.getErrorCode()))
                .setDurationMs(value(value.getDurationMs())).setClientName(StringUtils.defaultString(value.getClientName()))
                .setClientVersion(StringUtils.defaultString(value.getClientVersion())).setRemoteIp(StringUtils.defaultString(value.getRemoteIp())).build();
        return ReactiveGrpcClientSupport.<GrpcMcpAuditCommand, GrpcMcpBoolean>unary(
                "McpRuntimeFacade.audit", observer -> stub.audit(grpc, observer)).then();
    }
    private McpToolDefinitionDTO toTool(GrpcMcpToolDefinitionDTO source) {
        return McpToolDefinitionDTO.builder().name(source.getName()).title(source.getTitle()).description(source.getDescription())
                .inputSchema(source.getInputSchema().isBlank() ? java.util.Map.of() : io.github.pnoker.common.utils.JsonUtil.parseObject(source.getInputSchema(), java.util.Map.class))
                .annotations(McpToolDefinitionDTO.Annotations.builder().readOnlyHint(source.getAnnotations().getReadOnlyHint()).destructiveHint(source.getAnnotations().getDestructiveHint()).idempotentHint(source.getAnnotations().getIdempotentHint()).openWorldHint(source.getAnnotations().getOpenWorldHint()).build())
                .meta(McpToolDefinitionDTO.Metadata.builder().toolId(source.getMeta().getToolId()).permissionCode(source.getMeta().getPermissionCode()).riskLevel(source.getMeta().getRiskLevel().name()).build()).build();
    }
    private McpCallToolResponseDTO toCall(GrpcMcpCallToolDTO source) {
        GrpcMcpPrincipalContext principal = source.getPrincipal();
        return McpCallToolResponseDTO.builder().decision(source.getDecision().name()).confirmId(source.getConfirmId()).message(source.getMessage()).riskLevel(source.getRiskLevel().name())
                .tool(McpToolResolveResponseDTO.builder().toolId(source.getTool().getToolId()).toolName(source.getTool().getToolName()).permissionCode(source.getTool().getPermissionCode()).riskLevel(source.getTool().getRiskLevel().name()).serviceName(source.getTool().getServiceName()).apiPath(source.getTool().getApiPath()).httpMethod(source.getTool().getHttpMethod()).inputSchema(source.getTool().getInputSchema().isBlank() ? java.util.Map.of() : io.github.pnoker.common.utils.JsonUtil.parseObject(source.getTool().getInputSchema(), java.util.Map.class)).build())
                .principal(McpPrincipalContextDTO.builder().tenantId(principal.getTenantId()).principalId(principal.getPrincipalId()).principalType(principal.getPrincipalType().name()).principalName(principal.getPrincipalName()).displayName(principal.getDisplayName()).clientId(principal.getClientId()).connectionId(principal.getConnectionId()).build()).build();
    }
    private long value(Long value) { return value == null ? 0 : value; }
    private GrpcMcpRiskLevel risk(String value) { try { return GrpcMcpRiskLevel.valueOf(StringUtils.defaultString(value)); } catch (Exception e) { return GrpcMcpRiskLevel.MCP_RISK_LEVEL_UNSPECIFIED; } }
    private GrpcMcpAuditStatus status(String value) { try { return GrpcMcpAuditStatus.valueOf(StringUtils.defaultString(value)); } catch (Exception e) { return GrpcMcpAuditStatus.MCP_AUDIT_STATUS_UNSPECIFIED; } }
}
