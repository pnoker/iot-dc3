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

package io.github.pnoker.center.manager.grpc.api.point;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.GrpcPointQuery;
import io.github.pnoker.api.common.driver.GrpcRPointDTO;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Device Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class DriverOfPointApi extends PointApiGrpc.PointApiImplBase {

    private final PointService pointService;

    public DriverOfPointApi(PointService pointService) {
        this.pointService = pointService;
    }

    @Override
    public void selectById(GrpcPointQuery request, StreamObserver<GrpcRPointDTO> responseObserver) {
        GrpcRPointDTO.Builder builder = GrpcRPointDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        // TODO 添加位号是否属于设备，设备是否属于驱动的校验
        PointBO pointBO = pointService.selectById(request.getPointId());
        if (ObjectUtil.isNull(pointBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPointDTO pointDTO = GrpcPointBuilder.buildGrpcDTOByBO(pointBO);

            builder.setData(pointDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
