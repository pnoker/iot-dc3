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

import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * 自定义权限过滤器
 *
 * @author pnoker
 */
@Component
public class AuthenticFilter extends AbstractGatewayFilterFactory<Object> {
    /*@Resource
    private TokenClient tokenClient;*/

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = request.getHeaders().getFirst("Auth-Token");
            String user = request.getHeaders().getFirst("Auth-User");
            if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(user)) {
                try {
                    /*R<Boolean> tokenValid = tokenClient.checkTokenValid(user, token);
                    if (tokenValid.isOk()) {*/
                    return chain.filter(exchange);
                    /*}*/
                } catch (Exception ignored) {
                }
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }

}
