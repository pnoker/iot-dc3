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

import io.github.pnoker.common.annotation.PublicEndpoint;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.facade.api.McpRuntimeFacade;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.HmacAuthSigner;
import io.github.pnoker.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Gateway MCP Resource Server. It keeps the public MCP surface on the gateway,
 * validates OAuth bearer tokens through the auth center, and re-checks tool
 * visibility before every tool call.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class McpGatewayController {

    private final McpGatewayClient mcpGatewayClient;

    private final McpGatewayProperties mcpGatewayProperties;

    private static Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    /**
     * Publish the RFC 9728 protected-resource metadata that MCP clients fetch to learn
     * where to obtain bearer tokens and which scopes are accepted.
     *
     * @return a metadata map describing the resource, authorization servers, supported bearer methods and scopes
     */
    @Operation(summary = "Get Protected Resource Metadata", description = "Return the OAuth 2.0 protected-resource metadata (RFC 9728) advertised at the well-known path, so MCP clients can discover the authorization server, supported bearer transport methods and accepted scopes.")
    @PublicEndpoint
    @GetMapping(McpConstant.WELL_KNOWN_PROTECTED_RESOURCE)
    public Mono<Map<String, Object>> protectedResourceMetadata() {
        return Mono.just(orderedMap(
                "resource", mcpGatewayProperties.getResource(),
                "authorization_servers", List.of(mcpGatewayProperties.getAuthorizationServer()),
                "bearer_methods_supported", List.of(McpConstant.Server.BEARER_METHOD_HEADER),
                "scopes_supported", McpConstant.Scope.SUPPORTED
        ));
    }

    /**
     * Handle a single JSON-RPC MCP request (initialize, ping, tools/list, tools/call).
     * <p>
     * Requires a valid bearer token; the token is introspected against the auth center
     * and tool visibility is re-checked before every tools/call.
     *
     * @param request  JSON-RPC request body carrying the MCP method, id and parameters
     * @param exchange current server exchange, used to read the Authorization header and client metadata
     * @return a JSON-RPC result or error entity; 401 with a WWW-Authenticate challenge when the token is missing or inactive
     */
    @Operation(summary = "Handle MCP JSON-RPC Request", description = "Process one MCP JSON-RPC request (initialize, ping, tools/list, tools/call) behind the gateway. Validates the bearer token by introspecting it against the auth center, re-checks tool visibility before every tools/call, and returns a JSON-RPC result or error entity, or a 401 challenge when the token is missing or inactive.")
    @PublicEndpoint
    @PostMapping(value = McpConstant.URL_PREFIX, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> mcp(@RequestBody Map<String, Object> request,
                                                         ServerWebExchange exchange) {
        String token = bearerToken(exchange);
        if (StringUtils.isBlank(token)) {
            return Mono.just(challenge());
        }
        return mcpGatewayClient.introspect(token)
                .flatMap(context -> {
                    if (!context.isActive()) {
                        return Mono.just(challenge());
                    }
                    return dispatch(request, context, exchange);
                })
                .onErrorResume(e -> {
                    log.warn("MCP request failed", e);
                    return Mono.just(jsonRpcError(request.get(McpConstant.JsonRpc.FIELD_ID),
                            McpConstant.JsonRpc.ERROR_INTERNAL, "MCP request failed"));
                });
    }

    private Mono<ResponseEntity<Map<String, Object>>> dispatch(Map<String, Object> request,
                                                               McpIntrospectResponseDTO context,
                                                               ServerWebExchange exchange) {
        String method = Objects.toString(request.get(McpConstant.JsonRpc.FIELD_METHOD), "");
        Object id = request.get(McpConstant.JsonRpc.FIELD_ID);
        if (McpConstant.JsonRpc.METHOD_INITIALIZE.equals(method)) {
            return Mono.just(jsonRpcResult(id, orderedMap(
                    "protocolVersion", McpConstant.Server.PROTOCOL_VERSION,
                    "capabilities", orderedMap(McpConstant.Server.CAPABILITY_TOOLS,
                            orderedMap(McpConstant.Server.CAPABILITY_LIST_CHANGED, true)),
                    "serverInfo", orderedMap("name", McpConstant.Server.NAME, "version", McpConstant.Server.VERSION)
            )));
        }
        if (McpConstant.JsonRpc.METHOD_NOTIFICATIONS_INITIALIZED.equals(method)) {
            return Mono.just(ResponseEntity.accepted().build());
        }
        if (McpConstant.JsonRpc.METHOD_PING.equals(method)) {
            return Mono.just(jsonRpcResult(id, Map.of()));
        }
        if (McpConstant.JsonRpc.METHOD_TOOLS_LIST.equals(method)) {
            return mcpGatewayClient.listTools(context)
                    .map(result -> jsonRpcResult(id, result));
        }
        if (McpConstant.JsonRpc.METHOD_TOOLS_CALL.equals(method)) {
            Map<String, Object> params = mapValue(request.get(McpConstant.Field.PARAMS));
            String toolName = Objects.toString(params.get(McpConstant.Field.NAME), "");
            Map<String, Object> arguments = mapValue(params.get(McpConstant.Field.ARGUMENTS));
            Map<String, Object> callMeta = mapValue(params.get(McpConstant.Field.META));
            return mcpGatewayClient.callTool(context, toolName, arguments, callMeta, exchange)
                    .map(result -> jsonRpcResult(id, result));
        }
        return Mono.just(jsonRpcError(id, McpConstant.JsonRpc.ERROR_METHOD_NOT_FOUND, "Method not found"));
    }

    private ResponseEntity<Map<String, Object>> challenge() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE,
                        McpConstant.OAuth.TOKEN_TYPE_BEARER + " resource_metadata=\""
                                + McpConstant.WELL_KNOWN_PROTECTED_RESOURCE + "\"")
                .body(Map.of(McpConstant.Field.ERROR, "invalid_token"));
    }

    private ResponseEntity<Map<String, Object>> jsonRpcResult(Object id, Object result) {
        return ResponseEntity.ok(orderedMap(McpConstant.JsonRpc.FIELD_JSONRPC, McpConstant.JsonRpc.VERSION,
                McpConstant.JsonRpc.FIELD_ID, id, McpConstant.JsonRpc.FIELD_RESULT, result));
    }

    private ResponseEntity<Map<String, Object>> jsonRpcError(Object id, int code, String message) {
        return ResponseEntity.ok(orderedMap(McpConstant.JsonRpc.FIELD_JSONRPC, McpConstant.JsonRpc.VERSION,
                McpConstant.JsonRpc.FIELD_ID, id,
                McpConstant.JsonRpc.FIELD_ERROR, orderedMap(McpConstant.JsonRpc.ERROR_FIELD_CODE, code,
                        McpConstant.JsonRpc.ERROR_FIELD_MESSAGE, message)));
    }

    private String bearerToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String prefix = McpConstant.OAuth.TOKEN_TYPE_BEARER + ' ';
        if (StringUtils.isBlank(header) || !header.startsWith(prefix)) {
            return "";
        }
        return header.substring(prefix.length());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    /**
     * Gateway MCP client that resolves auth-center runtime state through the
     * facade layer and invokes the selected backend HTTP endpoint.
     */
    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class McpGatewayClient {

        private final HmacAuthSigner hmacAuthSigner;
        private final McpRuntimeFacade mcpRuntimeFacade;
        private final McpGatewayProperties mcpGatewayProperties;

        private final WebClient.Builder webClientBuilder = WebClient.builder();

        Mono<McpIntrospectResponseDTO> introspect(String token) {
            return blocking(() -> mcpRuntimeFacade.introspect(token));
        }

        Mono<McpToolListResponseDTO> listTools(McpIntrospectResponseDTO context) {
            return blocking(() -> mcpRuntimeFacade.listTools(context.getTenantId(), context.getPrincipalId(),
                    context.getMcpConnectionId(), context.getScope()));
        }

        Mono<Map<String, Object>> callTool(McpIntrospectResponseDTO context, String toolName,
                                           Map<String, Object> arguments, Map<String, Object> callMeta,
                                           ServerWebExchange exchange) {
            long start = System.nanoTime();
            String traceId = UUID.randomUUID().toString();
            String argumentDigest = DecodeUtil.sha256Base64Url(JsonUtil.toJsonString(arguments));
            return blocking(() -> mcpRuntimeFacade.resolveTool(context.getTenantId(), context.getPrincipalId(),
                    context.getMcpConnectionId(), context.getScope(), toolName)).flatMap(tool -> {
                McpToolCallControls controls = controlValues(callMeta, exchange);
                return blocking(() -> mcpRuntimeFacade.authorizeToolCall(McpToolAuthorizeRequestDTO.builder()
                        .tenantId(context.getTenantId())
                        .principalId(context.getPrincipalId())
                        .mcpConnectionId(context.getMcpConnectionId())
                        .scope(context.getScope())
                        .toolName(toolName)
                        .argumentDigest(argumentDigest)
                        .confirmId(controls.confirmId())
                        .idempotencyKey(controls.idempotencyKey())
                        .build())).flatMap(decision -> {
                    if (!McpConstant.Confirmation.DECISION_AUTHORIZED.equals(decision.getDecision())) {
                        return audit(context, tool, traceId, arguments, controls, McpConstant.Audit.DENIED,
                                McpConstant.Audit.POLICY_DENIED, start, exchange)
                                .thenReturn(toolError(authorizationMessage(decision)));
                    }
                    return invokeBackend(context, tool, arguments, controls)
                            .flatMap(result -> audit(context, tool, traceId, arguments, controls,
                                    McpConstant.Audit.SUCCESS, "", start, exchange)
                                    // Audit is post-hoc telemetry: a failed SUCCESS audit must not turn an
                                    // already-executed backend call into a client-visible error.
                                    .onErrorResume(auditError -> {
                                        log.warn("MCP success audit failed for tool '{}'", tool.getToolName(),
                                                auditError);
                                        return Mono.empty();
                                    })
                                    .thenReturn(orderedMap(McpConstant.ToolResult.CONTENT, List.of(orderedMap(
                                            McpConstant.ToolResult.TYPE, McpConstant.ToolResult.TYPE_TEXT,
                                            McpConstant.ToolResult.TEXT, JsonUtil.toJsonString(result)
                                    )))))
                            .onErrorResume(e -> audit(context, tool, traceId, arguments, controls,
                                    McpConstant.Audit.ERROR, e.getClass().getSimpleName(), start, exchange)
                                    .onErrorResume(auditError -> Mono.empty())
                                    .thenReturn(toolError(e.getMessage())));
                });
            });
        }

        private Map<String, Object> toolError(String message) {
            return orderedMap(McpConstant.ToolResult.IS_ERROR, true,
                    McpConstant.ToolResult.CONTENT, List.of(orderedMap(
                            McpConstant.ToolResult.TYPE, McpConstant.ToolResult.TYPE_TEXT,
                            McpConstant.ToolResult.TEXT, StringUtils.defaultString(message)
                    )));
        }

        private String authorizationMessage(McpToolAuthorizeResponseDTO decision) {
            if (McpConstant.Confirmation.DECISION_CONFIRM_REQUIRED.equals(decision.getDecision())
                    && StringUtils.isNotBlank(decision.getConfirmId())) {
                return decision.getMessage() + " (confirmId=" + decision.getConfirmId() + ")";
            }
            return StringUtils.defaultString(decision.getMessage());
        }

        private Mono<Map<String, Object>> invokeBackend(McpIntrospectResponseDTO context,
                                                        McpToolResolveResponseDTO tool,
                                                        Map<String, Object> arguments,
                                                        McpToolCallControls controls) {
            String url = backendBase(StringUtils.defaultString(tool.getServiceName()))
                    + StringUtils.defaultString(tool.getApiPath());
            HttpMethod method = HttpMethod.valueOf(StringUtils.defaultIfBlank(tool.getHttpMethod(),
                    HttpMethod.POST.name()));
            WebClient.RequestBodySpec spec = webClientBuilder.build()
                    .method(method)
                    .uri(uriBuilder -> {
                        URI uri = URI.create(url);
                        var builder = uriBuilder.scheme(uri.getScheme()).host(uri.getHost()).port(uri.getPort())
                                .path(uri.getPath());
                        if (HttpMethod.GET.equals(method) || HttpMethod.DELETE.equals(method)) {
                            arguments.forEach((key, value) -> builder.queryParam(key, value));
                        }
                        return builder.build();
                    })
                    .headers(headers -> {
                        headers.addAll(principalHeaders(context));
                        if (StringUtils.isNotBlank(controls.idempotencyKey())) {
                            headers.set(RequestConstant.Header.IDEMPOTENCY_KEY, controls.idempotencyKey());
                        }
                        if (StringUtils.isNotBlank(controls.confirmId())) {
                            headers.set(RequestConstant.Header.X_MCP_CONFIRM_ID, controls.confirmId());
                        }
                    });
            if (HttpMethod.GET.equals(method) || HttpMethod.DELETE.equals(method)) {
                return spec.retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        });
            }
            return spec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(arguments)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        }

        private HttpHeaders principalHeaders(McpIntrospectResponseDTO context) {
            RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
            principal.setPrincipalId(context.getPrincipalId());
            principal.setPrincipalType(StringUtils.defaultString(context.getPrincipalType()));
            principal.setPrincipalName(StringUtils.defaultString(context.getPrincipalName()));
            principal.setDisplayName(StringUtils.defaultString(context.getDisplayName()));
            principal.setTenantId(context.getTenantId());
            principal.setClientId(StringUtils.defaultString(context.getClientId()));
            principal.setConnectionId(context.getMcpConnectionId());

            String payload = JsonUtil.toJsonString(principal);
            HttpHeaders headers = new HttpHeaders();
            headers.set(RequestConstant.Header.X_AUTH_PRINCIPAL, payload);
            if (hmacAuthSigner.isEnabled()) {
                headers.set(RequestConstant.Header.X_AUTH_SIGN, hmacAuthSigner.sign(payload));
            }
            return headers;
        }

        private Mono<Void> audit(McpIntrospectResponseDTO context, McpToolResolveResponseDTO tool, String traceId,
                                 Map<String, Object> arguments, McpToolCallControls controls, String status,
                                 String errorCode, long start, ServerWebExchange exchange) {
            long duration = (System.nanoTime() - start) / 1_000_000;
            McpAuditCommandDTO command = McpAuditCommandDTO.builder()
                    .traceId(traceId)
                    .tenantId(context.getTenantId())
                    .principalId(context.getPrincipalId())
                    .principalType(context.getPrincipalType())
                    .clientId(context.getClientId())
                    .connectionId(context.getMcpConnectionId())
                    .toolId(tool.getToolId())
                    .toolName(tool.getToolName())
                    .permissionCode(tool.getPermissionCode())
                    .riskLevel(tool.getRiskLevel())
                    .confirmId(controls.confirmId())
                    .idempotencyKey(controls.idempotencyKey())
                    .argumentDigest(DecodeUtil.sha256Base64Url(JsonUtil.toJsonString(arguments)))
                    .status(status)
                    .errorCode(errorCode)
                    .durationMs(duration)
                    .clientName(exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_CLIENT_NAME))
                    .clientVersion(exchange.getRequest().getHeaders()
                            .getFirst(RequestConstant.Header.MCP_CLIENT_VERSION))
                    .remoteIp(exchange.getRequest().getRemoteAddress() == null ? ""
                            : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress())
                    .build();
            return blocking(() -> {
                mcpRuntimeFacade.audit(command);
                return true;
            }).then();
        }

        private McpToolCallControls controlValues(Map<String, Object> callMeta, ServerWebExchange exchange) {
            return new McpToolCallControls(
                    firstNonBlank(callMeta.get(McpConstant.Field.CONFIRM_ID_META),
                            exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_CONFIRM_ID)),
                    firstNonBlank(callMeta.get(McpConstant.Field.IDEMPOTENCY_KEY_META),
                            exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_IDEMPOTENCY_KEY),
                            exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.IDEMPOTENCY_KEY))
            );
        }

        private String firstNonBlank(Object... values) {
            for (Object value : values) {
                String text = Objects.toString(value, "");
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
            return "";
        }

        private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callable) {
            return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
        }

        private String backendBase(String serviceName) {
            return mcpGatewayProperties.backendBaseUrl(serviceName);
        }

        private record McpToolCallControls(String confirmId, String idempotencyKey) {
        }

    }

}
