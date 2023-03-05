/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.gateway.filter.factory;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.gateway.entity.bean.TokenRequestHeader;
import io.github.pnoker.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
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


/**
 * 自定义 Request Header 校验过滤器
 *
 * @author pnoker
 * @since 2022.1.0
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

        @GrpcClient("dc3-center-auth")
        private UserApiGrpc.UserApiBlockingStub userApiBlockingStub;
        @GrpcClient("dc3-center-auth")
        private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;
        @GrpcClient("dc3-center-auth")
        private TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub;

        @PostConstruct
        public void init() {
            gatewayFilter = this;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();

            try {
                String tenantHeader = GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT);
                String tenant = DecodeUtil.byteToString(DecodeUtil.decode(tenantHeader));
                if (ObjectUtil.isEmpty(tenant)) {
                    throw new UnAuthorizedException("Invalid request tenant header");
                }

                RTenantDTO rTenantDTO = gatewayFilter.tenantApiBlockingStub.selectByCode(CodeQuery.newBuilder().setCode(tenant).build());
                if (!rTenantDTO.getResult().getOk() || !EnableFlagDTOEnum.ENABLE.equals(rTenantDTO.getData().getEnableFlag())) {
                    throw new UnAuthorizedException("Invalid request tenant header");
                }

                String userHeader = GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_USER);
                String user = DecodeUtil.byteToString(DecodeUtil.decode(userHeader));
                if (ObjectUtil.isEmpty(user)) {
                    throw new UnAuthorizedException("Invalid request user header");
                }

                RUserDTO rUserDTO = gatewayFilter.userApiBlockingStub.selectByName(NameQuery.newBuilder().setName(user).build());
                if (!rUserDTO.getResult().getOk() || !EnableFlagDTOEnum.ENABLE.equals(rUserDTO.getData().getEnableFlag())) {
                    throw new UnAuthorizedException("Invalid request user header");
                }

                String tokenHeader = GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
                TokenRequestHeader token = JsonUtil.parseObject(DecodeUtil.decode(tokenHeader), TokenRequestHeader.class);
                if (ObjectUtil.isEmpty(token) || !CharSequenceUtil.isAllNotEmpty(token.getSalt(), token.getToken())) {
                    throw new UnAuthorizedException("Invalid request token header");
                }

                LoginQuery login = LoginQuery.newBuilder().setTenant(rTenantDTO.getData().getTenantName())
                        .setName(rUserDTO.getData().getLoginName())
                        .setSalt(token.getSalt())
                        .setToken(token.getToken()).build();
                RTokenDTO rTokenDTO = gatewayFilter.tokenApiBlockingStub.checkTokenValid(login);
                if (!rTokenDTO.getResult().getOk()) {
                    throw new UnAuthorizedException("Invalid request token header");
                }

                ServerHttpRequest build = request.mutate().headers(
                        httpHeader -> {
                            httpHeader.set(RequestConstant.Header.X_AUTH_TENANT_ID, rTenantDTO.getData().getBase().getId());
                            httpHeader.set(RequestConstant.Header.X_AUTH_TENANT, rTenantDTO.getData().getTenantName());
                            httpHeader.set(RequestConstant.Header.X_AUTH_USER_ID, rUserDTO.getData().getBase().getId());
                            httpHeader.set(RequestConstant.Header.X_AUTH_USER, rUserDTO.getData().getLoginName());
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
