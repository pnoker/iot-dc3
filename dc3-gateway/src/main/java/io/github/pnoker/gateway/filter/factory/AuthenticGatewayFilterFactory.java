/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.gateway.filter.factory;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.pnoker.api.center.auth.feign.TenantClient;
import io.github.pnoker.api.center.auth.feign.TokenClient;
import io.github.pnoker.api.center.auth.feign.UserClient;
import io.github.pnoker.common.annotation.Logs;
import io.github.pnoker.common.bean.Login;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.model.Tenant;
import io.github.pnoker.common.model.User;
import io.github.pnoker.common.utils.Dc3Util;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.gateway.bean.TokenRequestHeader;
import io.github.pnoker.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
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
        private UserClient userClient;
        @Resource
        private TokenClient tokenClient;

        @PostConstruct
        public void init() {
            gatewayFilter = this;
        }

        @Override
        @Logs("Authentic Gateway Filter")
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();

            try {
                String tenantHeader = GatewayUtil.getRequestHeader(request, ServiceConstant.Header.X_AUTH_TENANT);
                String tenant = Dc3Util.decodeString(tenantHeader);
                if (ObjectUtil.isEmpty(tenant)) {
                    throw new UnAuthorizedException("Invalid request tenant header");
                }
                R<Tenant> tenantR = gatewayFilter.tenantClient.selectByName(tenant);
                if (!tenantR.isOk() || !tenantR.getData().getEnable()) {
                    throw new UnAuthorizedException("Invalid request tenant header");
                }

                String userHeader = GatewayUtil.getRequestHeader(request, ServiceConstant.Header.X_AUTH_USER);
                String user = Dc3Util.decodeString(userHeader);
                if (ObjectUtil.isEmpty(user)) {
                    throw new UnAuthorizedException("Invalid request user header");
                }
                R<User> userR = gatewayFilter.userClient.selectByName(user);
                if (!userR.isOk() || !userR.getData().getEnable()) {
                    throw new UnAuthorizedException("Invalid request user header");
                }

                String tokenHeader = GatewayUtil.getRequestHeader(request, ServiceConstant.Header.X_AUTH_TOKEN);
                TokenRequestHeader token = JsonUtil.parseObject(Dc3Util.decode(tokenHeader), TokenRequestHeader.class);
                if (ObjectUtil.isEmpty(token) || !StrUtil.isAllNotEmpty(token.getSalt(), token.getToken())) {
                    throw new UnAuthorizedException("Invalid request token header");
                }
                Login login = new Login();
                login.setTenant(tenantR.getData().getName()).setName(userR.getData().getName()).setSalt(token.getSalt()).setToken(token.getToken());
                R<String> tokenR = gatewayFilter.tokenClient.checkTokenValid(login);
                if (!tokenR.isOk()) {
                    throw new UnAuthorizedException("Invalid request token header");
                }

                ServerHttpRequest build = request.mutate().headers(
                        httpHeader -> {
                            httpHeader.set(ServiceConstant.Header.X_AUTH_TENANT_ID, tenantR.getData().getId());
                            httpHeader.set(ServiceConstant.Header.X_AUTH_TENANT, tenantR.getData().getName());
                            httpHeader.set(ServiceConstant.Header.X_AUTH_USER_ID, userR.getData().getId());
                            httpHeader.set(ServiceConstant.Header.X_AUTH_USER, userR.getData().getName());
                        }
                ).build();

                exchange.mutate().request(build).build();
            } catch (Exception e) {
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                log.error(e.getMessage(), e);

                DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(e.getMessage())));
                return response.writeWith(Mono.just(dataBuffer));
            }

            return chain.filter(exchange);
        }
    }

}
