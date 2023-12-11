/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.gateway.filter;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticGatewayFilter implements GatewayFilter, Ordered {

    @GrpcClient(AuthServiceConstant.SERVICE_NAME)
    private UserLoginApiGrpc.UserLoginApiBlockingStub userLoginApiBlockingStub;
    @GrpcClient(AuthServiceConstant.SERVICE_NAME)
    private UserApiGrpc.UserApiBlockingStub userApiBlockingStub;
    @GrpcClient(AuthServiceConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;
    @GrpcClient(AuthServiceConstant.SERVICE_NAME)
    private TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        try {
            // Tenant, Login
            GrpcRTenantDTO rTenantDTO = getTenantDTO(request);
            GrpcRUserLoginDTO rUserLoginDTO = getLoginDTO(request);

            // Check Token Valid
            checkTokenValid(request, rTenantDTO, rUserLoginDTO);

            // Header
            ServerHttpRequest build = request.mutate().headers(headers -> {
                RequestHeader.UserHeader entityBO = getUserDTO(rUserLoginDTO, rTenantDTO);
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
    private GrpcRTenantDTO getTenantDTO(ServerHttpRequest request) {
        String tenant = DecodeUtil.byteToString(DecodeUtil.decode(GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT)));
        if (ObjectUtil.isEmpty(tenant)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        GrpcRTenantDTO entityDTO = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(tenant).build());
        if (!entityDTO.getResult().getOk() || EnableFlagEnum.ENABLE.getIndex() != entityDTO.getData().getEnableFlag()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return entityDTO;
    }

    @NotNull
    private GrpcRUserLoginDTO getLoginDTO(ServerHttpRequest request) {
        String user = DecodeUtil.byteToString(DecodeUtil.decode(GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN)));
        if (ObjectUtil.isEmpty(user)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        GrpcRUserLoginDTO entityDTO = userLoginApiBlockingStub.selectByName(GrpcNameQuery.newBuilder().setName(user).build());
        if (!entityDTO.getResult().getOk() || EnableFlagEnum.ENABLE.getIndex() != entityDTO.getData().getEnableFlag()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return entityDTO;
    }

    @NotNull
    private RequestHeader.UserHeader getUserDTO(GrpcRUserLoginDTO rUserLoginDTO, GrpcRTenantDTO rTenantDTO) {
        GrpcRUserDTO entityDTO = userApiBlockingStub.selectById(GrpcIdQuery.newBuilder().setId(rUserLoginDTO.getData().getBase().getId()).build());
        if (!entityDTO.getResult().getOk()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        RequestHeader.UserHeader entityBO = new RequestHeader.UserHeader();
        entityBO.setUserId(entityDTO.getData().getBase().getId());
        entityBO.setNickName(entityDTO.getData().getNickName());
        entityBO.setUserName(entityDTO.getData().getUserName());
        entityBO.setTenantId(rTenantDTO.getData().getBase().getId());
        return entityBO;
    }

    private void checkTokenValid(ServerHttpRequest request, GrpcRTenantDTO rTenantDTO, GrpcRUserLoginDTO rUserLoginDTO) {
        String token = GatewayUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
        RequestHeader.TokenHeader entityBO = JsonUtil.parseObject(DecodeUtil.decode(token), RequestHeader.TokenHeader.class);
        if (ObjectUtil.isEmpty(entityBO) || !CharSequenceUtil.isAllNotEmpty(entityBO.getSalt(), entityBO.getToken())) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        GrpcLoginQuery login = GrpcLoginQuery.newBuilder().setTenant(rTenantDTO.getData().getTenantCode()).setName(rUserLoginDTO.getData().getLoginName()).setSalt(entityBO.getSalt()).setToken(entityBO.getToken()).build();
        GrpcRTokenDTO entityDTO = tokenApiBlockingStub.checkTokenValid(login);
        if (!entityDTO.getResult().getOk()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
    }
}
