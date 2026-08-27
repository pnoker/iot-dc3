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
import io.netty.channel.ConnectTimeoutException;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
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

import java.net.ConnectException;

/**
 * Gateway filter that validates authentication headers.
 *
 * @author pnoker
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
        return Mono.fromCallable(() -> resolvePrincipalHeader(request)).subscribeOn(Schedulers.boundedElastic())
                .flatMap(userHeader -> {
                    String principalJson = JsonUtil.toJsonString(userHeader);
                    ServerHttpRequest mutated = request.mutate().headers(headers -> {
                        headers.set(RequestConstant.Header.X_AUTH_PRINCIPAL, principalJson);
                        if (hmacAuthSigner.isEnabled()) {
                            headers.set(RequestConstant.Header.X_AUTH_SIGN, hmacAuthSigner.sign(principalJson));
                        } else {
                            // Strip any inbound sign header so a downstream service can't be
                            // tricked into trusting a client-supplied one.
                            headers.remove(RequestConstant.Header.X_AUTH_SIGN);
                        }
                    }).build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                }).onErrorResume(UnAuthorizedException.class, e -> {
                    log.warn("Gateway request unauthorized, path={}", request.getURI().getRawPath(), e);
                    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
                }).onErrorResume(AuthenticGatewayFilter::isDownstreamUnreachable, e -> {
                    log.error("Gateway route unreachable, path={}", request.getURI().getRawPath(), e);
                    return writeErrorResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
                }).onErrorResume(e -> {
                    log.error("Gateway authentication failed unexpectedly, path={}",
                            request.getURI().getRawPath(), e);
                    return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
                });
    }

    /**
     * Whether the failure comes from the routed backend (TCP connect refused or timed
     * out) rather than from the authentication chain. Connect failures propagate up
     * through {@code chain.filter(...)} because this filter sits in front of the routing
     * chain; without this classification a down backend masquerades as an authentication
     * failure answered with 500 instead of 503.
     *
     * @param error the propagated error, possibly wrapped
     * @return true when the error chain contains a connect failure
     */
    private static boolean isDownstreamUnreachable(Throwable error) {
        while (error != null) {
            if (error instanceof ConnectException || error instanceof ConnectTimeoutException) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    /**
     * Resolve the principal header by running the full auth chain: tenant, credential,
     * token validation, then principal assembly.
     *
     * @param request the incoming request
     * @return the resolved principal header
     */
    private RequestHeader.PrincipalHeader resolvePrincipalHeader(ServerHttpRequest request) {
        FacadeTenantBO tenant = filterService.getTenant(request);
        FacadeLocalCredentialBO credential = filterService.getLocalCredential(request);
        filterService.checkValid(request, tenant, credential);
        return filterService.getUser(credential, tenant);
    }

    /**
     * Write a JSON error response with the given status and message.
     *
     * @param exchange current server exchange
     * @param status   the HTTP status to set
     * @param message  the error message
     * @return a mono completing when the response is written
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(status);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(message)));
        return response.writeWith(Mono.just(dataBuffer));
    }

}
