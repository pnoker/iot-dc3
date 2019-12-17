/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.gateway.filter;

import com.pnoker.gateway.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * <p>自定义权限过滤器
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Component
public class AuthenticFilter extends AbstractGatewayFilterFactory {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            // 获取Header信息
            String token = exchange.getRequest().getHeaders().getFirst("Token");
            String appId = exchange.getRequest().getHeaders().getFirst("AppId");
            /*if (!StringUtils.isBlank(token) && !StringUtils.isBlank(appId)) {
                if (authService.isPermitted(new TokenDto().setToken(token).setAppId(appId))) {
                    return chain.filter(exchange);
                }
            }*/
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }
}
