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

package io.github.pnoker.common.gateway.filter;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import io.github.pnoker.common.gateway.service.FilterService;
import io.github.pnoker.common.utils.HmacAuthSigner;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Gateway filter that validates authentication headers.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticGatewayFilter implements GatewayFilter {

    private final FilterService filterService;

    private final HmacAuthSigner hmacAuthSigner;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // The auth lookups (tenant / user / token) are blocking gRPC calls. Run the whole
        // synchronous block on boundedElastic so the Netty event loop stays free to accept
        // new connections under load. FilterServiceImpl caches the lookups for ~60s.
        return Mono.fromCallable(() -> resolveUserHeader(request)).subscribeOn(Schedulers.boundedElastic())
                .flatMap(userHeader -> {
                    String userJson = JsonUtil.toJsonString(userHeader);
                    ServerHttpRequest mutated = request.mutate().headers(headers -> {
                        headers.set(RequestConstant.Header.X_AUTH_USER, userJson);
                        if (hmacAuthSigner.isEnabled()) {
                            headers.set(RequestConstant.Header.X_AUTH_SIGN, hmacAuthSigner.sign(userJson));
                        } else {
                            // Strip any inbound sign header so a downstream service can't be
                            // tricked into trusting a client-supplied one.
                            headers.remove(RequestConstant.Header.X_AUTH_SIGN);
                        }
                    }).build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                }).onErrorResume(UnAuthorizedException.class, e -> {
                    log.warn("AuthenticGatewayFilter unauthorized, Url: {}", request.getURI(), e);
                    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
                }).onErrorResume(e -> {
                    log.error("AuthenticGatewayFilter unexpected error, Url: {}", request.getURI(), e);
                    return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
                });
    }

    private RequestHeader.UserHeader resolveUserHeader(ServerHttpRequest request) {
        FacadeTenantBO tenant = filterService.getTenant(request);
        FacadeUserLoginBO userLogin = filterService.getUserLogin(request);
        filterService.checkValid(request, tenant, userLogin);
        return filterService.getUser(userLogin, tenant);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(status);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(message)));
        return response.writeWith(Mono.just(dataBuffer));
    }

}
