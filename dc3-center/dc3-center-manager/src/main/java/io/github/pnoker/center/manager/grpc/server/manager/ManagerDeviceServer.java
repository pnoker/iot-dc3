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

package io.github.pnoker.center.manager.grpc.server.manager;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.center.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Objects;

/**
 * 设备 Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class ManagerDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {

    private final GrpcDeviceBuilder grpcDeviceBuilder;
    private final DeviceService deviceService;

    public ManagerDeviceServer(GrpcDeviceBuilder grpcDeviceBuilder, DeviceService deviceService) {
        this.grpcDeviceBuilder = grpcDeviceBuilder;
        this.deviceService = deviceService;
    }

    @Override
    public void list(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
        GrpcRPageDeviceDTO.Builder builder = GrpcRPageDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceQuery pageQuery = grpcDeviceBuilder.buildQueryByGrpcQuery(request);

        Page<DeviceBO> devicePage = deviceService.selectByPage(pageQuery);
        if (Objects.isNull(devicePage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPageDeviceDTO.Builder pageDeviceBuilder = GrpcPageDeviceDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(devicePage.getCurrent());
            page.setSize(devicePage.getSize());
            page.setPages(devicePage.getPages());
            page.setTotal(devicePage.getTotal());
            pageDeviceBuilder.setPage(page);
            List<GrpcDeviceDTO> collect = devicePage.getRecords().stream().map(grpcDeviceBuilder::buildGrpcDTOByBO).toList();
            pageDeviceBuilder.addAllData(collect);

            builder.setData(pageDeviceBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDriverId(GrpcDriverQuery driver, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> deviceBOList = deviceService.selectByDriverId(driver.getDriverId());
        if (CollUtil.isEmpty(deviceBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> deviceDTOS = deviceBOList.stream().map(grpcDeviceBuilder::buildGrpcDTOByBO).toList();

            builder.addAllData(deviceDTOS);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByProfileId(GrpcProfileQuery request, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> deviceBOList = deviceService.selectByProfileId(request.getProfileId());
        if (CollUtil.isEmpty(deviceBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> deviceDTOS = deviceBOList.stream().map(grpcDeviceBuilder::buildGrpcDTOByBO).toList();

            builder.addAllData(deviceDTOS);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcRDeviceDTO> responseObserver) {
        GrpcRDeviceDTO.Builder builder = GrpcRDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceBO deviceBO = deviceService.selectById(request.getDeviceId());
        if (Objects.isNull(deviceBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDeviceDTO deviceDTO = grpcDeviceBuilder.buildGrpcDTOByBO(deviceBO);

            builder.setData(deviceDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
