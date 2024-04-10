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
import io.github.pnoker.api.center.auth.GrpcNameQuery;
import io.github.pnoker.api.center.auth.GrpcRUserLoginDTO;
import io.github.pnoker.api.center.auth.GrpcUserLoginDTO;
import io.github.pnoker.api.center.auth.UserLoginApiGrpc;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.auth.entity.bo.UserLoginBO;
import io.github.pnoker.center.auth.service.UserLoginService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

/**
 * UserLogin Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class UserLoginApi extends UserLoginApiGrpc.UserLoginApiImplBase {

    @Resource
    private UserLoginService userLoginService;

    @Override
    public void selectByName(GrpcNameQuery request, StreamObserver<GrpcRUserLoginDTO> responseObserver) {
        GrpcRUserLoginDTO.Builder builder = GrpcRUserLoginDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();
        UserLoginBO select = userLoginService.selectByLoginName(request.getName(), false);
        if (ObjectUtil.isNull(select)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());
            GrpcUserLoginDTO user = buildGrpcDTOByBO(select);
            builder.setData(user);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    /**
     * DO to DTO
     *
     * @param entityBO UserLogin
     * @return UserLoginDTO
     */
    private GrpcUserLoginDTO buildGrpcDTOByBO(UserLoginBO entityBO) {
        GrpcUserLoginDTO.Builder builder = GrpcUserLoginDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO);
        builder.setLoginName(entityBO.getLoginName());
        builder.setUserId(entityBO.getUserId());
        builder.setUserPasswordId(entityBO.getUserPasswordId());
        builder.setEnableFlag(entityBO.getEnableFlag().getIndex());
        return builder.build();
    }
}
