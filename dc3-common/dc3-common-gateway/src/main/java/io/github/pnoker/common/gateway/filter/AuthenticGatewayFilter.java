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

import io.github.pnoker.api.center.auth.GrpcRTenantDTO;
import io.github.pnoker.api.center.auth.GrpcRUserLoginDTO;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.gateway.service.FilterService;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
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
 * 自定义 Request Header 校验过滤器逻辑类
 *
 * @author pnoker
 * @version 2025.2.1
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AuthenticGatewayFilter implements GatewayFilter {

    @Resource
    private FilterService filterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Get the HTTP request from the exchange
        ServerHttpRequest request = exchange.getRequest();

        try {
            // Get tenant and login information from the request
            // Tenant, Login
            GrpcRTenantDTO rTenantDTO = filterService.getTenantDTO(request);
            GrpcRUserLoginDTO rUserLoginDTO = filterService.getLoginDTO(request);

            // Validate the authentication token
            // Check Token Valid
            filterService.checkValid(request, rTenantDTO, rUserLoginDTO);

            // Build a new request with modified headers containing user authentication info
            // Header
            ServerHttpRequest build = request.mutate().headers(headers -> {
                RequestHeader.UserHeader entityBO = filterService.getUserDTO(rUserLoginDTO, rTenantDTO);
                headers.set(RequestConstant.Header.X_AUTH_USER, JsonUtil.toJsonString(entityBO));
            }).build();

            // Continue the filter chain with the modified request
            return chain.filter(exchange.mutate().request(build).build());
        } catch (Exception e) {
            // Log the authentication error
            log.error("AuthenticGatewayFilter error: {}, Url: {}", e.getMessage(), request.getURI(), e);

            // Prepare error response
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);

            // Create response data buffer with error message
            DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(e.getMessage())));
            return response.writeWith(Mono.just(dataBuffer));
        }
    }
}
