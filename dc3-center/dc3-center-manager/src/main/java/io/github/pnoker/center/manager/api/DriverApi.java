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

package io.github.pnoker.center.manager.api;


import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.ByDeviceQueryDTO;
import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.DriverDTO;
import io.github.pnoker.api.center.manager.RDriverDTO;
import io.github.pnoker.api.common.BaseDTO;
import io.github.pnoker.api.common.DriverTypeFlagDTOEnum;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.RDTO;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.center.manager.utils.BuilderUtil;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.Driver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

/**
 * Driver Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class DriverApi extends DriverApiGrpc.DriverApiImplBase {

    @Resource
    private DriverService driverService;

    @Override
    public void selectByDeviceId(ByDeviceQueryDTO request, StreamObserver<RDriverDTO> responseObserver) {
        RDriverDTO.Builder builder = RDriverDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();

        Driver driver = driverService.selectByDeviceId(request.getDeviceId());
        if (ObjectUtil.isNull(driver)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());

            DriverDTO driverDTO = buildDTOByDO(driver);

            builder.setData(driverDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * DO to DTO
     *
     * @param entityDO Driver
     * @return DriverDTO
     */
    private DriverDTO buildDTOByDO(Driver entityDO) {
        DriverDTO.Builder builder = DriverDTO.newBuilder();
        BaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setId(entityDO.getId());
        builder.setDriverName(entityDO.getDriverName());
        builder.setDriverCode(entityDO.getDriverCode());
        builder.setServiceName(entityDO.getServiceName());
        builder.setDriverTypeFlag(DriverTypeFlagDTOEnum.valueOf(entityDO.getDriverTypeFlag().name()));
        builder.setServiceHost(entityDO.getServiceHost());
        builder.setServicePort(entityDO.getServicePort());
        builder.setEnableFlag(EnableFlagDTOEnum.valueOf(entityDO.getEnableFlag().name()));
        builder.setTenantId(entityDO.getTenantId());
        return builder.build();
    }

}
