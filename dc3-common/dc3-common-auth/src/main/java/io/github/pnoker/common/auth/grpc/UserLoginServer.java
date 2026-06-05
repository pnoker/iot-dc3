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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server handling user login facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServer extends UserLoginApiGrpc.UserLoginApiImplBase {

    private final GrpcUserLoginBuilder grpcUserLoginBuilder;

    private final UserLoginService userLoginService;

    @Override
    public void getByName(GrpcNameQuery request, StreamObserver<GrpcRUserLoginDTO> responseObserver) {
        GrpcRUserLoginDTO.Builder builder = GrpcRUserLoginDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            UserLoginBO entityBO = userLoginService.getByLoginName(request.getName(), false);
            if (Objects.isNull(entityBO)) {
                rBuilder.setOk(false);
                rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
                rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getRemark());
            } else {
                rBuilder.setOk(true);
                rBuilder.setCode(ResponseEnum.OK.getCode());
                rBuilder.setMessage(ResponseEnum.OK.getRemark());

                builder.setData(grpcUserLoginBuilder.buildGrpcDTOByBO(entityBO));
            }
        } catch (Exception e) {
            log.warn("getByName failed", e);
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(ResponseEnum.FAILURE.getRemark());
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
