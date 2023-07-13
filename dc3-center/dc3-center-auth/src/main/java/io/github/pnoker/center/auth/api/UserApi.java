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

package io.github.pnoker.center.auth.api;


import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.IdQuery;
import io.github.pnoker.api.center.auth.RUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.api.center.auth.UserDTO;
import io.github.pnoker.api.common.BaseDTO;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.RDTO;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.utils.BuilderUtil;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.User;
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
    public void selectById(IdQuery request, StreamObserver<RUserDTO> responseObserver) {
        RUserDTO.Builder builder = RUserDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();
        User select = userService.selectById(request.getId());
        if (ObjectUtil.isNull(select)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());
            UserDTO user = buildDTOByDO(select);
            builder.setData(user);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    /**
     * DO to DTO
     *
     * @param entityDO User
     * @return UserDTO
     */
    private UserDTO buildDTOByDO(User entityDO) {
        UserDTO.Builder builder = UserDTO.newBuilder();
        BaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setNickName(entityDO.getNickName());
        builder.setUserName(entityDO.getUserName());
        builder.setPhone(entityDO.getPhone());
        builder.setEmail(entityDO.getEmail());
        builder.setSocialExt(entityDO.getSocialExt());
        builder.setIdentityExt(entityDO.getIdentityExt());
        return builder.build();
    }
}
