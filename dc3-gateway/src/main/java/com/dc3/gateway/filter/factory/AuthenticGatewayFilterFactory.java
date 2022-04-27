/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.gateway.filter.factory;

import com.dc3.api.center.auth.feign.TenantClient;
import com.dc3.api.center.auth.feign.TokenClient;
import com.dc3.common.bean.Login;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Tenant;
import com.dc3.common.utils.Dc3Util;
import com.dc3.common.utils.JsonUtil;
import com.dc3.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


/**
 * 自定义 Request Header 校验过滤器
 *
 * @author pnoker
 */
@Slf4j
@Component
public class AuthenticGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return new AuthenticGatewayFilter();
    }

    @Component
    static class AuthenticGatewayFilter implements GatewayFilter {
        private static AuthenticGatewayFilter gatewayFilter;

        @Resource
        private TenantClient tenantClient;
        @Resource
        private TokenClient tokenClient;

        @PostConstruct
        public void init() {
            gatewayFilter = this;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();

            try {
                String cookieToken = GatewayUtil.getRequestCookie(request, Common.Service.DC3_AUTH_TOKEN);
                Login login = JsonUtil.parseObject(Dc3Util.decode(cookieToken), Login.class);
                log.debug("Request cookies: {}", login);

                R<Tenant> tenantR = gatewayFilter.tenantClient.selectByName(login.getTenant());
                if (!tenantR.isOk() || !tenantR.getData().getEnable()) {
                    throw new ServiceException("Invalid tenant");
                }

                R<Long> validR = gatewayFilter.tokenClient.checkTokenValid(login);
                if (!validR.isOk()) {
                    throw new ServiceException("Invalid token");
                }
                Tenant tenant = tenantR.getData();
                log.debug("Request tenant: {}", tenant);

                ServerHttpRequest build = request.mutate().headers(
                        httpHeader -> {
                            httpHeader.set(Common.Service.DC3_AUTH_TENANT_ID, tenant.getId().toString());
                            httpHeader.set(Common.Service.DC3_AUTH_TENANT, login.getTenant());
                            httpHeader.set(Common.Service.DC3_AUTH_USER, login.getName());
                        }
                ).build();
                exchange.mutate().request(build).build();
            } catch (Exception e) {
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add(Common.Response.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                log.error(e.getMessage(), e);

                DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(e.getMessage())));
                return response.writeWith(Mono.just(dataBuffer));
            }

            return chain.filter(exchange);
        }
    }

}
