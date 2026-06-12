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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.biz.impl.OAuthMcpRuntimeServiceImpl.OAuthProtocolException;
import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.utils.HmacAuthSigner;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Internal endpoints consumed by the gateway MCP runtime. The endpoints are not
 * business APIs; every request must be signed by the gateway HMAC secret when HMAC
 * is configured.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@RestController
@RequiredArgsConstructor
public class McpInternalController {

    private final OAuthMcpRuntimeService oauthMcpRuntimeService;
    private final HmacAuthSigner hmacAuthSigner;

    @PostMapping("/oauth2/introspect")
    public Mono<ResponseEntity<Map<String, Object>>> introspect(@RequestBody Map<String, Object> request,
                                                                ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            return ResponseEntity.ok(oauthMcpRuntimeService.introspect(Objects.toString(request.get("token"), "")));
        });
    }

    @PostMapping("/mcp/internal/catalog/refresh")
    public Mono<ResponseEntity<Map<String, Object>>> refreshCatalog(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            return ResponseEntity.ok(Map.of("changed", oauthMcpRuntimeService.refreshToolCatalog()));
        });
    }

    @PostMapping("/mcp/internal/tools/list")
    public Mono<ResponseEntity<Map<String, Object>>> listTools(@RequestBody Map<String, Object> request,
                                                               ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tools", oauthMcpRuntimeService.listVisibleTools(
                    longValue(request.get("tenant_id")),
                    longValue(request.get("principal_id")),
                    longValue(request.get("mcp_connection_id")),
                    scopes(request.get("scope"))
            ));
            return ResponseEntity.ok(body);
        }).onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    @PostMapping("/mcp/internal/tools/resolve")
    public Mono<ResponseEntity<Map<String, Object>>> resolveTool(@RequestBody Map<String, Object> request,
                                                                 ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            McpToolRecord tool = oauthMcpRuntimeService.resolveVisibleTool(
                    longValue(request.get("tenant_id")),
                    longValue(request.get("principal_id")),
                    longValue(request.get("mcp_connection_id")),
                    Objects.toString(request.get("tool_name"), ""),
                    scopes(request.get("scope"))
            );
            return ResponseEntity.ok(toolRecord(tool));
        }).onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    @PostMapping("/mcp/internal/audit")
    public Mono<ResponseEntity<Map<String, Object>>> audit(@RequestBody McpAuditCommand command,
                                                           ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            oauthMcpRuntimeService.audit(command);
            return ResponseEntity.ok(Map.of("stored", true));
        });
    }

    private Mono<ResponseEntity<Map<String, Object>>> oauthError(OAuthProtocolException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getError());
        body.put("error_description", exception.getDescription());
        return Mono.just(ResponseEntity.status(exception.getStatusCode()).body(body));
    }

    private void requireInternal(ServerWebExchange exchange) {
        if (!hmacAuthSigner.isEnabled()) {
            return;
        }
        String caller = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_CALLER);
        String timestamp = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_TIMESTAMP);
        String nonce = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_NONCE);
        String sign = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_SIGN);
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String payload = caller + '\n' + timestamp + '\n' + nonce + '\n' + path;
        if (StringUtils.isAnyBlank(caller, timestamp, nonce, sign) || !fresh(timestamp)
                || !hmacAuthSigner.verify(payload, sign)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid internal signature");
        }
    }

    private boolean fresh(String timestamp) {
        try {
            long diff = Math.abs(Instant.now().toEpochMilli() - Long.parseLong(timestamp));
            return diff <= 300_000;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Set<String> scopes(Object value) {
        String raw = Objects.toString(value, "");
        if (StringUtils.isBlank(raw)) {
            return Set.of();
        }
        return Set.of(raw.trim().split("[\\s,]+"));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(Objects.toString(value, "0"));
    }

    private Map<String, Object> toolRecord(McpToolRecord tool) {
        Map<String, Object> map = JsonUtil.parseObject(JsonUtil.toJsonString(tool),
                new tools.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
        map.values().removeIf(Objects::isNull);
        return map;
    }

}
