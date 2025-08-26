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


import io.github.pnoker.api.center.auth.GrpcLoginQuery;
import io.github.pnoker.api.center.auth.GrpcRTokenDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.TimeUtil;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Objects;

/**
 * Token Api
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class TokenServer extends TokenApiGrpc.TokenApiImplBase {

    @Resource
    private TokenService tokenService;

    @Override
    public void checkValid(GrpcLoginQuery request, StreamObserver<GrpcRTokenDTO> responseObserver) {
        GrpcRTokenDTO.Builder builder = GrpcRTokenDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        TokenValid entity = tokenService.checkValid(request.getName(), request.getSalt(), request.getToken(), request.getTenant());
        if (Objects.isNull(entity)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else if (!entity.isValid()) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.TOKEN_INVALID.getCode());
            rBuilder.setMessage(ResponseEnum.TOKEN_INVALID.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            builder.setData(TimeUtil.completeFormat(entity.getExpireTime()));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
