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
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcRDeviceDTO;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.center.manager.service.DeviceService;
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
public class DriverOfDeviceApi extends DeviceApiGrpc.DeviceApiImplBase {

    private final DeviceService deviceService;

    public DriverOfDeviceApi(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public void selectById(GrpcDeviceQuery request, StreamObserver<GrpcRDeviceDTO> responseObserver) {
        GrpcRDeviceDTO.Builder builder = GrpcRDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        // TODO 添加设备是否属于该驱动的校验
        DeviceBO entityBO = deviceService.selectById(request.getDeviceId());
        if (ObjectUtil.isEmpty(entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDeviceDTO deviceDTO = GrpcDeviceBuilder.buildGrpcDTOByBO(entityBO);

            builder.setData(deviceDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
