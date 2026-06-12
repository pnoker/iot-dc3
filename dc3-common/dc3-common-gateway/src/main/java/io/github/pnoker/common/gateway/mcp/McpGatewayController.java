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

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.HmacAuthSigner;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
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

    @Value("${dc3.mcp.resource:http://localhost:8000/mcp}")
    private String resource;

    @Value("${dc3.mcp.authorization-server:http://localhost:8000}")
    private String authorizationServer;

    @GetMapping(McpConstant.WELL_KNOWN_PROTECTED_RESOURCE)
    public Mono<Map<String, Object>> protectedResourceMetadata() {
        return Mono.just(orderedMap(
                "resource", resource,
                "authorization_servers", List.of(authorizationServer),
                "bearer_methods_supported", List.of(McpConstant.Server.BEARER_METHOD_HEADER),
                "scopes_supported", McpConstant.Scope.SUPPORTED
        ));
    }

    @PostMapping(value = McpConstant.URL_PREFIX, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> mcp(@RequestBody Map<String, Object> request,
                                                         ServerWebExchange exchange) {
        String token = bearerToken(exchange);
        if (StringUtils.isBlank(token)) {
            return Mono.just(challenge());
        }
        return mcpGatewayClient.introspect(token)
                .flatMap(context -> {
                    if (!Boolean.TRUE.equals(context.get(McpConstant.Field.ACTIVE))) {
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

    private Mono<ResponseEntity<Map<String, Object>>> dispatch(Map<String, Object> request, Map<String, Object> context,
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

    private static Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private static Map<String, String> orderedStringMap(Object... values) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), Objects.toString(values[i + 1], ""));
        }
        return map;
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(Objects.toString(value, "0"));
    }

    /**
     * Internal WebClient facade for auth-center MCP endpoints and backend tool
     * invocation.
     */
    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class McpGatewayClient {

        private static final String INTERNAL_CALLER = "dc3-gateway";

        private final HmacAuthSigner hmacAuthSigner;
        private final WebClient.Builder webClientBuilder;

        @Value("${dc3.mcp.auth-base-url:http://dc3-center-auth:8300/auth}")
        private String authBaseUrl;

        @Value("${dc3.mcp.backend.auth-url:http://dc3-center-auth:8300/auth}")
        private String authUrl;

        @Value("${dc3.mcp.backend.manager-url:http://dc3-center-manager:8400/manager}")
        private String managerUrl;

        @Value("${dc3.mcp.backend.data-url:http://dc3-center-data:8500/data}")
        private String dataUrl;

        @Value("${dc3.mcp.backend.agentic-url:http://dc3-center-agentic:8600/agentic}")
        private String agenticUrl;

        Mono<Map<String, Object>> introspect(String token) {
            return postInternal(authBaseUrl + McpConstant.OAUTH2_INTROSPECT,
                    Map.of(McpConstant.Field.TOKEN, token));
        }

        Mono<Map<String, Object>> listTools(Map<String, Object> context) {
            return postInternal(authBaseUrl + McpConstant.INTERNAL_TOOLS_LIST, orderedMap(
                    McpConstant.Field.TENANT_ID, context.get(McpConstant.Field.TENANT_ID),
                    McpConstant.Field.PRINCIPAL_ID, context.get(McpConstant.Field.PRINCIPAL_ID),
                    McpConstant.Field.MCP_CONNECTION_ID, context.get(McpConstant.Field.MCP_CONNECTION_ID),
                    McpConstant.Field.SCOPE, context.get(McpConstant.Field.SCOPE)
            ));
        }

        Mono<Map<String, Object>> callTool(Map<String, Object> context, String toolName,
                                           Map<String, Object> arguments, Map<String, Object> callMeta,
                                           ServerWebExchange exchange) {
            long start = System.nanoTime();
            String traceId = UUID.randomUUID().toString();
            return postInternal(authBaseUrl + McpConstant.INTERNAL_TOOLS_RESOLVE, orderedMap(
                    McpConstant.Field.TENANT_ID, context.get(McpConstant.Field.TENANT_ID),
                    McpConstant.Field.PRINCIPAL_ID, context.get(McpConstant.Field.PRINCIPAL_ID),
                    McpConstant.Field.MCP_CONNECTION_ID, context.get(McpConstant.Field.MCP_CONNECTION_ID),
                    McpConstant.Field.SCOPE, context.get(McpConstant.Field.SCOPE),
                    McpConstant.Field.TOOL_NAME_REQUEST, toolName
            )).flatMap(tool -> {
                Map<String, String> controls = controlValues(callMeta, exchange);
                String policyError = policyError(tool, controls);
                if (StringUtils.isNotBlank(policyError)) {
                    return audit(context, tool, traceId, arguments, controls, McpConstant.Audit.DENIED,
                            McpConstant.Audit.POLICY_DENIED, start, exchange)
                            .thenReturn(orderedMap(McpConstant.ToolResult.IS_ERROR, true,
                                    McpConstant.ToolResult.CONTENT, List.of(orderedMap(
                                            McpConstant.ToolResult.TYPE, McpConstant.ToolResult.TYPE_TEXT,
                                            McpConstant.ToolResult.TEXT, policyError
                            ))));
                }
                return invokeBackend(context, tool, arguments, controls)
                        .flatMap(result -> audit(context, tool, traceId, arguments, controls,
                                McpConstant.Audit.SUCCESS, "", start, exchange)
                                .thenReturn(orderedMap(McpConstant.ToolResult.CONTENT, List.of(orderedMap(
                                        McpConstant.ToolResult.TYPE, McpConstant.ToolResult.TYPE_TEXT,
                                        McpConstant.ToolResult.TEXT, JsonUtil.toJsonString(result)
                                )))))
                        .onErrorResume(e -> audit(context, tool, traceId, arguments, controls,
                                McpConstant.Audit.ERROR, e.getClass().getSimpleName(), start, exchange)
                                .thenReturn(orderedMap(McpConstant.ToolResult.IS_ERROR, true,
                                        McpConstant.ToolResult.CONTENT, List.of(orderedMap(
                                                McpConstant.ToolResult.TYPE, McpConstant.ToolResult.TYPE_TEXT,
                                                McpConstant.ToolResult.TEXT, e.getMessage()
                                        )))));
            });
        }

        private Mono<Map<String, Object>> invokeBackend(Map<String, Object> context, Map<String, Object> tool,
                                                        Map<String, Object> arguments,
                                                        Map<String, String> controls) {
            String url = backendBase(Objects.toString(tool.get("serviceName"), ""))
                    + Objects.toString(tool.get("apiPath"), "");
            HttpMethod method = HttpMethod.valueOf(Objects.toString(tool.get("httpMethod"), "POST"));
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
                        if (StringUtils.isNotBlank(controls.get(McpConstant.Field.IDEMPOTENCY_KEY))) {
                            headers.set(RequestConstant.Header.IDEMPOTENCY_KEY,
                                    controls.get(McpConstant.Field.IDEMPOTENCY_KEY));
                        }
                        if (StringUtils.isNotBlank(controls.get(McpConstant.Field.CONFIRM_ID))) {
                            headers.set(RequestConstant.Header.X_MCP_CONFIRM_ID,
                                    controls.get(McpConstant.Field.CONFIRM_ID));
                        }
            });
            if (HttpMethod.GET.equals(method) || HttpMethod.DELETE.equals(method)) {
                return spec.retrieve()
                        .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                        });
            }
            return spec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(arguments)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
        }

        private HttpHeaders principalHeaders(Map<String, Object> context) {
            RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
            principal.setPrincipalId(longValue(context.get(McpConstant.Field.PRINCIPAL_ID)));
            principal.setPrincipalType(Objects.toString(context.get(McpConstant.Field.PRINCIPAL_TYPE), ""));
            principal.setPrincipalName(Objects.toString(context.get(McpConstant.Field.PRINCIPAL_NAME), ""));
            principal.setDisplayName(Objects.toString(context.get(McpConstant.Field.DISPLAY_NAME), ""));
            principal.setTenantId(longValue(context.get(McpConstant.Field.TENANT_ID)));
            principal.setClientId(Objects.toString(context.get(McpConstant.Field.CLIENT_ID), ""));
            principal.setConnectionId(longValue(context.get(McpConstant.Field.MCP_CONNECTION_ID)));

            String payload = JsonUtil.toJsonString(principal);
            HttpHeaders headers = new HttpHeaders();
            headers.set(RequestConstant.Header.X_AUTH_PRINCIPAL, payload);
            if (hmacAuthSigner.isEnabled()) {
                headers.set(RequestConstant.Header.X_AUTH_SIGN, hmacAuthSigner.sign(payload));
            }
            return headers;
        }

        private Mono<Void> audit(Map<String, Object> context, Map<String, Object> tool, String traceId,
                                 Map<String, Object> arguments, Map<String, String> controls, String status,
                                 String errorCode, long start, ServerWebExchange exchange) {
            long duration = (System.nanoTime() - start) / 1_000_000;
            Map<String, Object> command = orderedMap(
                    McpConstant.Field.TRACE_ID, traceId,
                    McpConstant.Field.TENANT_ID_CAMEL, context.get(McpConstant.Field.TENANT_ID),
                    McpConstant.Field.PRINCIPAL_ID_CAMEL, context.get(McpConstant.Field.PRINCIPAL_ID),
                    McpConstant.Field.PRINCIPAL_TYPE_CAMEL, context.get(McpConstant.Field.PRINCIPAL_TYPE),
                    McpConstant.Field.CLIENT_ID_CAMEL, context.get(McpConstant.Field.CLIENT_ID),
                    McpConstant.Field.CONNECTION_ID, context.get(McpConstant.Field.MCP_CONNECTION_ID),
                    McpConstant.Field.TOOL_ID, tool.get(McpConstant.Field.TOOL_ID),
                    McpConstant.Field.TOOL_NAME, tool.get(McpConstant.Field.TOOL_NAME),
                    McpConstant.Field.PERMISSION_CODE, tool.get(McpConstant.Field.PERMISSION_CODE),
                    McpConstant.Field.RISK_LEVEL, tool.get(McpConstant.Field.RISK_LEVEL),
                    McpConstant.Field.CONFIRM_ID, controls.get(McpConstant.Field.CONFIRM_ID),
                    McpConstant.Field.IDEMPOTENCY_KEY, controls.get(McpConstant.Field.IDEMPOTENCY_KEY),
                    McpConstant.Field.ARGUMENT_DIGEST, DecodeUtil.sha256Base64Url(JsonUtil.toJsonString(arguments)),
                    McpConstant.Field.STATUS, status,
                    McpConstant.Field.ERROR_CODE, errorCode,
                    McpConstant.Field.DURATION_MS, duration,
                    McpConstant.Field.CLIENT_NAME,
                    exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_CLIENT_NAME),
                    McpConstant.Field.CLIENT_VERSION,
                    exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_CLIENT_VERSION),
                    McpConstant.Field.REMOTE_IP, exchange.getRequest().getRemoteAddress() == null ? ""
                            : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            );
            return postInternal(authBaseUrl + McpConstant.INTERNAL_AUDIT, command).then();
        }

        private Map<String, String> controlValues(Map<String, Object> callMeta, ServerWebExchange exchange) {
            return orderedStringMap(
                    McpConstant.Field.CONFIRM_ID, firstNonBlank(callMeta.get(McpConstant.Field.CONFIRM_ID_META),
                            exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_CONFIRM_ID)),
                    McpConstant.Field.IDEMPOTENCY_KEY, firstNonBlank(
                            callMeta.get(McpConstant.Field.IDEMPOTENCY_KEY_META),
                            exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.MCP_IDEMPOTENCY_KEY),
                            exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.IDEMPOTENCY_KEY))
            );
        }

        private String policyError(Map<String, Object> tool, Map<String, String> controls) {
            if (!McpConstant.RiskLevel.HIGH.equals(Objects.toString(tool.get(McpConstant.Field.RISK_LEVEL), ""))) {
                return "";
            }
            if (StringUtils.isBlank(controls.get(McpConstant.Field.CONFIRM_ID))) {
                return "High risk MCP tool requires confirmation";
            }
            if (StringUtils.isBlank(controls.get(McpConstant.Field.IDEMPOTENCY_KEY))) {
                return "High risk MCP tool requires an idempotency key";
            }
            return "";
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

        private Mono<Map<String, Object>> postInternal(String url, Map<String, Object> body) {
            String path = URI.create(url).getPath();
            return webClientBuilder.build()
                    .post()
                    .uri(url)
                    .headers(headers -> internalHeaders(headers, path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
        }

        private void internalHeaders(HttpHeaders headers, String path) {
            if (!hmacAuthSigner.isEnabled()) {
                return;
            }
            String caller = INTERNAL_CALLER;
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String nonce = UUID.randomUUID().toString();
            String payload = caller + '\n' + timestamp + '\n' + nonce + '\n' + path;
            headers.set(RequestConstant.Header.X_INTERNAL_CALLER, caller);
            headers.set(RequestConstant.Header.X_INTERNAL_TIMESTAMP, timestamp);
            headers.set(RequestConstant.Header.X_INTERNAL_NONCE, nonce);
            headers.set(RequestConstant.Header.X_INTERNAL_SIGN, hmacAuthSigner.sign(payload));
        }

        private String backendBase(String serviceName) {
            if (serviceName.contains("auth")) {
                return authUrl;
            }
            if (serviceName.contains("manager")) {
                return managerUrl;
            }
            if (serviceName.contains("data")) {
                return dataUrl;
            }
            if (serviceName.contains("agentic")) {
                return agenticUrl;
            }
            throw new IllegalArgumentException("Unknown backend service: " + serviceName);
        }

    }

}
