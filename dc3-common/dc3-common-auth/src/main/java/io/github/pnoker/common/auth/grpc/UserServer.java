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

package io.github.pnoker.common.auth.grpc;


import io.github.pnoker.api.center.auth.GrpcIdQuery;
import io.github.pnoker.api.center.auth.GrpcRUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcUserBuilder;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Objects;

/**
 * User Api
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class UserServer extends UserApiGrpc.UserApiImplBase {

    @Resource
    private GrpcUserBuilder grpcUserBuilder;

    @Resource
    private UserService userService;

    @Override
    public void selectById(GrpcIdQuery request, StreamObserver<GrpcRUserDTO> responseObserver) {
        GrpcRUserDTO.Builder builder = GrpcRUserDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        UserBO entityBO = userService.selectById(request.getId());
        if (Objects.isNull(entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            builder.setData(grpcUserBuilder.buildGrpcDTOByBO(entityBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
