/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.gateway.filter;

import com.dc3.api.center.auth.token.feign.TokenClient;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 自定义权限过滤器
 *
 * @author pnoker
 */
@Slf4j
@Component
public class AuthenticGatewayFilter implements GatewayFilter, Ordered {
    private static AuthenticGatewayFilter authenticGatewayFilter;

    @PostConstruct
    public void init() {
        authenticGatewayFilter = this;
    }

    @Resource
    private TokenClient tokenClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        try {
            R<Boolean> tokenValid = authenticGatewayFilter.tokenClient.checkTokenValid(
                    GatewayUtil.getRequestHeader(request, Common.Service.DC3_GATEWAY_AUTH_USER),
                    GatewayUtil.getRequestHeader(request, Common.Service.DC3_GATEWAY_AUTH_TOKEN)
            );
            if (tokenValid.isOk()) {
                return chain.filter(exchange);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
