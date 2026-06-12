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
import io.github.pnoker.common.entity.common.RequestHeader;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
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

    @GetMapping("/.well-known/oauth-protected-resource")
    public Mono<Map<String, Object>> protectedResourceMetadata() {
        return Mono.just(orderedMap(
                "resource", resource,
                "authorization_servers", List.of(authorizationServer),
                "bearer_methods_supported", List.of("header"),
                "scopes_supported", List.of("mcp:tools:list", "mcp:tools:call", "mcp:tools:call:high",
                        "mcp:resources:read")
        ));
    }

    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> mcp(@RequestBody Map<String, Object> request,
                                                         ServerWebExchange exchange) {
        String token = bearerToken(exchange);
        if (StringUtils.isBlank(token)) {
            return Mono.just(challenge());
        }
        return mcpGatewayClient.introspect(token)
                .flatMap(context -> {
                    if (!Boolean.TRUE.equals(context.get("active"))) {
                        return Mono.just(challenge());
                    }
                    return dispatch(request, context, exchange);
                })
                .onErrorResume(e -> {
                    log.warn("MCP request failed", e);
                    return Mono.just(jsonRpcError(request.get("id"), -32000, "MCP request failed"));
                });
    }

    private Mono<ResponseEntity<Map<String, Object>>> dispatch(Map<String, Object> request, Map<String, Object> context,
                                                               ServerWebExchange exchange) {
        String method = Objects.toString(request.get("method"), "");
        Object id = request.get("id");
        if ("initialize".equals(method)) {
            return Mono.just(jsonRpcResult(id, orderedMap(
                    "protocolVersion", "2025-06-18",
                    "capabilities", orderedMap("tools", orderedMap("listChanged", true)),
                    "serverInfo", orderedMap("name", "iot-dc3-gateway", "version", "2026.5.22")
            )));
        }
        if ("notifications/initialized".equals(method)) {
            return Mono.just(ResponseEntity.accepted().build());
        }
        if ("ping".equals(method)) {
            return Mono.just(jsonRpcResult(id, Map.of()));
        }
        if ("tools/list".equals(method)) {
            return mcpGatewayClient.listTools(context)
                    .map(result -> jsonRpcResult(id, result));
        }
        if ("tools/call".equals(method)) {
            Map<String, Object> params = mapValue(request.get("params"));
            String toolName = Objects.toString(params.get("name"), "");
            Map<String, Object> arguments = mapValue(params.get("arguments"));
            Map<String, Object> callMeta = mapValue(params.get("_meta"));
            return mcpGatewayClient.callTool(context, toolName, arguments, callMeta, exchange)
                    .map(result -> jsonRpcResult(id, result));
        }
        return Mono.just(jsonRpcError(id, -32601, "Method not found"));
    }

    private ResponseEntity<Map<String, Object>> challenge() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer resource_metadata=\"/.well-known/oauth-protected-resource\"")
                .body(Map.of("error", "invalid_token"));
    }

    private ResponseEntity<Map<String, Object>> jsonRpcResult(Object id, Object result) {
        return ResponseEntity.ok(orderedMap("jsonrpc", "2.0", "id", id, "result", result));
    }

    private ResponseEntity<Map<String, Object>> jsonRpcError(Object id, int code, String message) {
        return ResponseEntity.ok(orderedMap("jsonrpc", "2.0", "id", id,
                "error", orderedMap("code", code, "message", message)));
    }

    private String bearerToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(header) || !header.startsWith("Bearer ")) {
            return "";
        }
        return header.substring(7);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    /**
     * Internal WebClient facade for auth-center MCP endpoints and backend tool
     * invocation.
     */
    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class McpGatewayClient {

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
            return postInternal(authBaseUrl + "/oauth2/introspect", Map.of("token", token));
        }

        Mono<Map<String, Object>> listTools(Map<String, Object> context) {
            return postInternal(authBaseUrl + "/mcp/internal/tools/list", orderedMap(
                    "tenant_id", context.get("tenant_id"),
                    "principal_id", context.get("principal_id"),
                    "mcp_connection_id", context.get("mcp_connection_id"),
                    "scope", context.get("scope")
            ));
        }

        Mono<Map<String, Object>> callTool(Map<String, Object> context, String toolName,
                                           Map<String, Object> arguments, Map<String, Object> callMeta,
                                           ServerWebExchange exchange) {
            long start = System.nanoTime();
            String traceId = UUID.randomUUID().toString();
            return postInternal(authBaseUrl + "/mcp/internal/tools/resolve", orderedMap(
                    "tenant_id", context.get("tenant_id"),
                    "principal_id", context.get("principal_id"),
                    "mcp_connection_id", context.get("mcp_connection_id"),
                    "scope", context.get("scope"),
                    "tool_name", toolName
            )).flatMap(tool -> {
                Map<String, String> controls = controlValues(callMeta, exchange);
                String policyError = policyError(tool, controls);
                if (StringUtils.isNotBlank(policyError)) {
                    return audit(context, tool, traceId, arguments, controls, "DENIED", "POLICY_DENIED", start,
                            exchange)
                            .thenReturn(orderedMap("isError", true, "content", List.of(orderedMap(
                                    "type", "text",
                                    "text", policyError
                            ))));
                }
                return invokeBackend(context, tool, arguments, controls)
                    .flatMap(result -> audit(context, tool, traceId, arguments, controls, "SUCCESS", "", start,
                            exchange)
                            .thenReturn(orderedMap("content", List.of(orderedMap(
                                    "type", "text",
                                    "text", JsonUtil.toJsonString(result)
                            )))))
                    .onErrorResume(e -> audit(context, tool, traceId, arguments, controls, "ERROR",
                            e.getClass().getSimpleName(), start, exchange)
                            .thenReturn(orderedMap("isError", true, "content", List.of(orderedMap(
                                    "type", "text",
                                    "text", e.getMessage()
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
                        if (StringUtils.isNotBlank(controls.get("idempotencyKey"))) {
                            headers.set("Idempotency-Key", controls.get("idempotencyKey"));
                        }
                        if (StringUtils.isNotBlank(controls.get("confirmId"))) {
                            headers.set("X-Mcp-Confirm-Id", controls.get("confirmId"));
                        }
                    });
            if (HttpMethod.GET.equals(method) || HttpMethod.DELETE.equals(method)) {
                return spec.retrieve().bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
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
            principal.setPrincipalId(longValue(context.get("principal_id")));
            principal.setPrincipalType(Objects.toString(context.get("principal_type"), ""));
            principal.setPrincipalName(Objects.toString(context.get("principal_name"), ""));
            principal.setDisplayName(Objects.toString(context.get("display_name"), ""));
            principal.setTenantId(longValue(context.get("tenant_id")));
            principal.setClientId(Objects.toString(context.get("client_id"), ""));
            principal.setConnectionId(longValue(context.get("mcp_connection_id")));

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
                    "traceId", traceId,
                    "tenantId", context.get("tenant_id"),
                    "principalId", context.get("principal_id"),
                    "principalType", context.get("principal_type"),
                    "clientId", context.get("client_id"),
                    "connectionId", context.get("mcp_connection_id"),
                    "toolId", tool.get("toolId"),
                    "toolName", tool.get("toolName"),
                    "permissionCode", tool.get("permissionCode"),
                    "riskLevel", tool.get("riskLevel"),
                    "confirmId", controls.get("confirmId"),
                    "idempotencyKey", controls.get("idempotencyKey"),
                    "argumentDigest", digest(JsonUtil.toJsonString(arguments)),
                    "status", status,
                    "errorCode", errorCode,
                    "durationMs", duration,
                    "clientName", exchange.getRequest().getHeaders().getFirst("Mcp-Client-Name"),
                    "clientVersion", exchange.getRequest().getHeaders().getFirst("Mcp-Client-Version"),
                    "remoteIp", exchange.getRequest().getRemoteAddress() == null ? ""
                            : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            );
            return postInternal(authBaseUrl + "/mcp/internal/audit", command).then();
        }

        private Map<String, String> controlValues(Map<String, Object> callMeta, ServerWebExchange exchange) {
            return orderedStringMap(
                    "confirmId", firstNonBlank(callMeta.get("confirm_id"),
                            exchange.getRequest().getHeaders().getFirst("Mcp-Confirm-Id")),
                    "idempotencyKey", firstNonBlank(callMeta.get("idempotency_key"),
                            exchange.getRequest().getHeaders().getFirst("Mcp-Idempotency-Key"),
                            exchange.getRequest().getHeaders().getFirst("Idempotency-Key"))
            );
        }

        private String policyError(Map<String, Object> tool, Map<String, String> controls) {
            if (!"HIGH".equals(Objects.toString(tool.get("riskLevel"), ""))) {
                return "";
            }
            if (StringUtils.isBlank(controls.get("confirmId"))) {
                return "High risk MCP tool requires confirmation";
            }
            if (StringUtils.isBlank(controls.get("idempotencyKey"))) {
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
            String caller = "dc3-gateway";
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

        private String digest(String value) {
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Exception e) {
                return "";
            }
        }

        private Long longValue(Object value) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(Objects.toString(value, "0"));
        }

        private Map<String, Object> orderedMap(Object... values) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i += 2) {
                map.put(String.valueOf(values[i]), values[i + 1]);
            }
            return map;
        }

        private Map<String, String> orderedStringMap(Object... values) {
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i += 2) {
                map.put(String.valueOf(values[i]), Objects.toString(values[i + 1], ""));
            }
            return map;
        }

    }

}
