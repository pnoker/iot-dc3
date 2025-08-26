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


import io.github.pnoker.api.center.auth.GrpcNameQuery;
import io.github.pnoker.api.center.auth.GrpcRUserLoginDTO;
import io.github.pnoker.api.center.auth.UserLoginApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcUserLoginBuilder;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Objects;

/**
 * UserLogin Api
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class UserLoginServer extends UserLoginApiGrpc.UserLoginApiImplBase {

    @Resource
    private GrpcUserLoginBuilder grpcUserLoginBuilder;

    @Resource
    private UserLoginService userLoginService;

    @Override
    public void selectByName(GrpcNameQuery request, StreamObserver<GrpcRUserLoginDTO> responseObserver) {
        GrpcRUserLoginDTO.Builder builder = GrpcRUserLoginDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        UserLoginBO entityBO = userLoginService.selectByLoginName(request.getName(), false);
        if (Objects.isNull(entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            builder.setData(grpcUserLoginBuilder.buildGrpcDTOByBO(entityBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
