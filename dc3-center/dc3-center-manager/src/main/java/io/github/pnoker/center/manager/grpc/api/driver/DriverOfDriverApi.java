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

package io.github.pnoker.center.manager.grpc.api.driver;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDriverQuery;
import io.github.pnoker.api.common.driver.GrpcRDriverDTO;
import io.github.pnoker.api.common.driver.GrpcRDriverReportDTO;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.center.manager.service.DriverService;
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
public class DriverOfDriverApi extends DriverApiGrpc.DriverApiImplBase {

    private final DriverService driverService;

    public DriverOfDriverApi(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public void selectById(GrpcDriverQuery request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverBO driverBO = driverService.selectById(request.getDriverId());
        if (ObjectUtil.isNull(driverBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDriverDTO driverDTO = GrpcDriverBuilder.buildGrpcDTOByBO(driverBO);

            builder.setData(driverDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDriverReport(GrpcDriverQuery request, StreamObserver<GrpcRDriverReportDTO> responseObserver) {
        super.getDriverReport(request, responseObserver);
    }
}
