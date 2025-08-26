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

package io.github.pnoker.common.gateway.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.api.center.auth.*;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.gateway.service.FilterService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class FilterServiceImpl implements FilterService {

    @GrpcClient(AuthConstant.SERVICE_NAME)
    private UserLoginApiGrpc.UserLoginApiBlockingStub userLoginApiBlockingStub;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private UserApiGrpc.UserApiBlockingStub userApiBlockingStub;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub;

    @Override
    public GrpcRTenantDTO getTenantDTO(ServerHttpRequest request) {
        // Get tenant code from request header
        String tenant = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT);
        if (CharSequenceUtil.isEmpty(tenant)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Query tenant info by tenant code
        GrpcRTenantDTO entityDTO = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(tenant).build());
        if (!entityDTO.getResult().getOk() || EnableFlagEnum.ENABLE.getIndex() != entityDTO.getData().getEnableFlag()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return entityDTO;
    }

    @Override
    public GrpcRUserLoginDTO getLoginDTO(ServerHttpRequest request) {
        // Get user login name from request header
        String user = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN);
        if (CharSequenceUtil.isEmpty(user)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Query user login info by login name
        GrpcRUserLoginDTO entityDTO = userLoginApiBlockingStub.selectByName(GrpcNameQuery.newBuilder().setName(user).build());
        if (!entityDTO.getResult().getOk() || EnableFlagEnum.ENABLE.getIndex() != entityDTO.getData().getEnableFlag()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return entityDTO;
    }

    @Override
    public RequestHeader.UserHeader getUserDTO(GrpcRUserLoginDTO rUserLoginDTO, GrpcRTenantDTO rTenantDTO) {
        // Query user info by user id
        GrpcRUserDTO entityDTO = userApiBlockingStub.selectById(GrpcIdQuery.newBuilder().setId(rUserLoginDTO.getData().getBase().getId()).build());
        if (!entityDTO.getResult().getOk()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Build user header info
        RequestHeader.UserHeader entityBO = new RequestHeader.UserHeader();
        entityBO.setUserId(entityDTO.getData().getBase().getId());
        entityBO.setNickName(entityDTO.getData().getNickName());
        entityBO.setUserName(entityDTO.getData().getUserName());
        entityBO.setTenantId(rTenantDTO.getData().getBase().getId());
        return entityBO;
    }

    @Override
    public void checkValid(ServerHttpRequest request, GrpcRTenantDTO rTenantDTO, GrpcRUserLoginDTO rUserLoginDTO) {
        // Get token from request header and parse it
        String token = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
        RequestHeader.TokenHeader entityBO = JsonUtil.parseObject(token, RequestHeader.TokenHeader.class);
        if (Objects.isNull(entityBO) || !CharSequenceUtil.isAllNotEmpty(entityBO.getSalt(), entityBO.getToken())) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Build login query and validate token
        GrpcLoginQuery login = GrpcLoginQuery.newBuilder()
                .setTenant(rTenantDTO.getData().getTenantCode())
                .setName(rUserLoginDTO.getData().getLoginName())
                .setSalt(entityBO.getSalt())
                .setToken(entityBO.getToken())
                .build();
        GrpcRTokenDTO entityDTO = tokenApiBlockingStub.checkValid(login);
        if (!entityDTO.getResult().getOk()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
    }
}
