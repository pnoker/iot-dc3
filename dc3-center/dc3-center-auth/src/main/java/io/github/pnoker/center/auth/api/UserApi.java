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

package io.github.pnoker.center.auth.api;


import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.GrpcIdQuery;
import io.github.pnoker.api.center.auth.GrpcRUserDTO;
import io.github.pnoker.api.center.auth.GrpcUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.auth.entity.bo.UserBO;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

/**
 * User Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class UserApi extends UserApiGrpc.UserApiImplBase {

    @Resource
    private UserService userService;

    @Override
    public void selectById(GrpcIdQuery request, StreamObserver<GrpcRUserDTO> responseObserver) {
        GrpcRUserDTO.Builder builder = GrpcRUserDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();
        UserBO select = userService.selectById(request.getId());
        if (ObjectUtil.isNull(select)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());
            GrpcUserDTO user = buildGrpcDTOByBO(select);
            builder.setData(user);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    /**
     * DO to DTO
     *
     * @param entityBO User
     * @return UserDTO
     */
    private GrpcUserDTO buildGrpcDTOByBO(UserBO entityBO) {
        GrpcUserDTO.Builder builder = GrpcUserDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO);
        builder.setNickName(entityBO.getNickName());
        builder.setUserName(entityBO.getUserName());
        builder.setPhone(entityBO.getPhone());
        builder.setEmail(entityBO.getEmail());
        builder.setSocialExt(JsonUtil.toJsonString(entityBO.getSocialExt()));
        builder.setIdentityExt(JsonUtil.toJsonString(entityBO.getIdentityExt()));
        return builder.build();
    }
}
