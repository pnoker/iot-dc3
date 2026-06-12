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
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpAuditResponseDTO;
import io.github.pnoker.common.entity.dto.McpCatalogRefreshResponseDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectRequestDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolListRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolListResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveRequestDTO;
import io.github.pnoker.common.utils.HmacAuthSigner;
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

    @PostMapping(McpConstant.OAUTH2_INTROSPECT)
    public Mono<ResponseEntity<McpIntrospectResponseDTO>> introspect(@RequestBody McpIntrospectRequestDTO request,
                                                                     ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            String token = request == null ? "" : Objects.toString(request.getToken(), "");
            return ResponseEntity.ok(oauthMcpRuntimeService.introspect(token));
        });
    }

    @PostMapping(McpConstant.INTERNAL_CATALOG_REFRESH)
    public Mono<ResponseEntity<McpCatalogRefreshResponseDTO>> refreshCatalog(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            return ResponseEntity.ok(McpCatalogRefreshResponseDTO.builder()
                    .changed(oauthMcpRuntimeService.refreshToolCatalog())
                    .build());
        });
    }

    @PostMapping(McpConstant.INTERNAL_TOOLS_LIST)
    public Mono<ResponseEntity<?>> listTools(@RequestBody McpToolListRequestDTO request,
                                             ServerWebExchange exchange) {
        return Mono.<ResponseEntity<?>>fromSupplier(() -> {
            requireInternal(exchange);
            McpToolListRequestDTO body = request == null ? new McpToolListRequestDTO() : request;
            return ResponseEntity.ok(McpToolListResponseDTO.builder()
                    .tools(oauthMcpRuntimeService.listVisibleTools(
                            body.getTenantId(),
                            body.getPrincipalId(),
                            body.getMcpConnectionId(),
                            scopes(body.getScope())
                    ))
                    .build());
        }).onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    @PostMapping(McpConstant.INTERNAL_TOOLS_RESOLVE)
    public Mono<ResponseEntity<?>> resolveTool(@RequestBody McpToolResolveRequestDTO request,
                                               ServerWebExchange exchange) {
        return Mono.<ResponseEntity<?>>fromSupplier(() -> {
            requireInternal(exchange);
            McpToolResolveRequestDTO body = request == null ? new McpToolResolveRequestDTO() : request;
            return ResponseEntity.ok(oauthMcpRuntimeService.resolveVisibleTool(
                    body.getTenantId(),
                    body.getPrincipalId(),
                    body.getMcpConnectionId(),
                    Objects.toString(body.getToolName(), ""),
                    scopes(body.getScope())
            ));
        }).onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    @PostMapping(McpConstant.INTERNAL_AUDIT)
    public Mono<ResponseEntity<McpAuditResponseDTO>> audit(@RequestBody McpAuditCommandDTO command,
                                                           ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            requireInternal(exchange);
            oauthMcpRuntimeService.audit(command);
            return ResponseEntity.ok(McpAuditResponseDTO.builder().stored(true).build());
        });
    }

    private Mono<ResponseEntity<?>> oauthError(OAuthProtocolException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(McpConstant.Field.ERROR, exception.getError());
        body.put(McpConstant.Field.ERROR_DESCRIPTION, exception.getDescription());
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
            return diff <= RequestConstant.DEFAULT_INTERNAL_SIGNATURE_TTL_MS;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Set<String> scopes(String value) {
        String raw = Objects.toString(value, "");
        if (StringUtils.isBlank(raw)) {
            return Set.of();
        }
        return Set.of(raw.trim().split("[\\s,]+"));
    }

}
