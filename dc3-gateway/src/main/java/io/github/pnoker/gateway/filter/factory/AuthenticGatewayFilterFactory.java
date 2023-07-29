/*
 * Copyright 2016-present the original author or authors.
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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.bo.RequestHeaderBO;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.jetbrains.annotations.NotNull;
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

        @GrpcClient(AuthServiceConstant.SERVICE_NAME)
        private UserLoginApiGrpc.UserLoginApiBlockingStub userLoginApiBlockingStub;
        @GrpcClient(AuthServiceConstant.SERVICE_NAME)
        private UserApiGrpc.UserApiBlockingStub userApiBlockingStub;
        @GrpcClient(AuthServiceConstant.SERVICE_NAME)
        private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;
        @GrpcClient(AuthServiceConstant.SERVICE_NAME)
        private TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub;

        @PostConstruct
        public void init() {
            gatewayFilter = this;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();

            try {
                // Tenant, Login
                RTenantDTO rTenantDTO = getTenantDTO(request);
                RUserLoginDTO rUserLoginDTO = getLoginDTO(request);

                // Check Token Valid
                checkTokenValid(request, rTenantDTO, rUserLoginDTO);

                // Header
                ServerHttpRequest build = request.mutate().headers(headers -> {
                    RequestHeaderBO.UserHeader entityBO = getUserDTO(rUserLoginDTO, rTenantDTO);
                    headers.set(RequestConstant.Header.X_AUTH_USER, DecodeUtil.byteToString(DecodeUtil.encode(JsonUtil.toJsonBytes(entityBO))));
                }).build();

                exchange.mutate().request(build).build();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.toJsonBytes(R.fail(e.getMessage())));
                return response.writeWith(Mono.just(dataBuffer));
            }

            return chain.filter(exchange);
        }

        @NotNull
        private static RTenantDTO getTenantDTO(ServerHttpRequest request) {
            String tenant = DecodeUtil.byteToString(DecodeUtil.decode(GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT)));
            if (ObjectUtil.isEmpty(tenant)) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }

            RTenantDTO entityDTO = gatewayFilter.tenantApiBlockingStub.selectByCode(CodeQuery.newBuilder().setCode(tenant).build());
            if (!entityDTO.getResult().getOk() || !EnableFlagDTOEnum.ENABLE.equals(entityDTO.getData().getEnableFlag())) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }
            return entityDTO;
        }

        @NotNull
        private static RUserLoginDTO getLoginDTO(ServerHttpRequest request) {
            String user = DecodeUtil.byteToString(DecodeUtil.decode(GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN)));
            if (ObjectUtil.isEmpty(user)) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }

            RUserLoginDTO entityDTO = gatewayFilter.userLoginApiBlockingStub.selectByName(NameQuery.newBuilder().setName(user).build());
            if (!entityDTO.getResult().getOk() || !EnableFlagDTOEnum.ENABLE.equals(entityDTO.getData().getEnableFlag())) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }
            return entityDTO;
        }

        @NotNull
        private static RequestHeaderBO.UserHeader getUserDTO(RUserLoginDTO rUserLoginDTO, RTenantDTO rTenantDTO) {
            RUserDTO entityDTO = gatewayFilter.userApiBlockingStub.selectById(IdQuery.newBuilder().setId(rUserLoginDTO.getData().getBase().getId()).build());
            if (!entityDTO.getResult().getOk()) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }

            RequestHeaderBO.UserHeader entityBO = new RequestHeaderBO.UserHeader();
            entityBO.setUserId(entityDTO.getData().getBase().getId());
            entityBO.setNickName(entityDTO.getData().getNickName());
            entityBO.setUserName(entityDTO.getData().getUserName());
            entityBO.setTenantId(rTenantDTO.getData().getBase().getId());
            return entityBO;
        }

        private static void checkTokenValid(ServerHttpRequest request, RTenantDTO rTenantDTO, RUserLoginDTO rUserLoginDTO) {
            String token = GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
            RequestHeaderBO.TokenHeader entityBO = JsonUtil.parseObject(DecodeUtil.decode(token), RequestHeaderBO.TokenHeader.class);
            if (ObjectUtil.isEmpty(entityBO) || !CharSequenceUtil.isAllNotEmpty(entityBO.getSalt(), entityBO.getToken())) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }

            LoginQuery login = LoginQuery.newBuilder().setTenant(rTenantDTO.getData().getTenantCode()).setName(rUserLoginDTO.getData().getLoginName()).setSalt(entityBO.getSalt()).setToken(entityBO.getToken()).build();
            RTokenDTO entityDTO = gatewayFilter.tokenApiBlockingStub.checkTokenValid(login);
            if (!entityDTO.getResult().getOk()) {
                throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
            }
        }
    }

}
