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
import io.github.pnoker.common.utils.JsonUtil;
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

/**
 * Request Header
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AuthenticGatewayFilter implements GatewayFilter {

    private final FilterService filterService;

    public AuthenticGatewayFilter(FilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        try {
            FacadeTenantBO tenant = filterService.getTenant(request);
            FacadeUserLoginBO userLogin = filterService.getUserLogin(request);
            filterService.checkValid(request, tenant, userLogin);

            RequestHeader.UserHeader userHeader = filterService.getUser(userLogin, tenant);
            ServerHttpRequest build = request.mutate()
                    .headers(headers -> headers.set(RequestConstant.Header.X_AUTH_USER, JsonUtil.toJsonString(userHeader)))
                    .build();
            return chain.filter(exchange.mutate().request(build).build());
        } catch (UnAuthorizedException e) {
            log.warn("AuthenticGatewayFilter unauthorized: {}, Url: {}", e.getMessage(), request.getURI());
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            log.error("AuthenticGatewayFilter unexpected error, Url: {}", request.getURI(), e);
            return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(status);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(message)));
        return response.writeWith(Mono.just(dataBuffer));
    }

}
