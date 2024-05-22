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

package io.github.pnoker.center.manager.grpc.server.driver;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.*;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.center.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.center.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.center.manager.grpc.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.center.manager.grpc.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.optional.CollectionOptional;
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
public class DriverDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {

    private final GrpcDeviceBuilder grpcDeviceBuilder;
    private final GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;
    private final GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;
    private final DeviceService deviceService;
    private final DriverAttributeConfigService driverAttributeConfigService;
    private final PointAttributeConfigService pointAttributeConfigService;
    private final PointService pointService;

    public DriverDeviceServer(GrpcDeviceBuilder grpcDeviceBuilder, GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder,
                              GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder, DeviceService deviceService,
                              DriverAttributeConfigService driverAttributeConfigService, PointAttributeConfigService pointAttributeConfigService,
                              PointService pointService) {
        this.grpcDeviceBuilder = grpcDeviceBuilder;
        this.grpcDriverAttributeConfigBuilder = grpcDriverAttributeConfigBuilder;
        this.grpcPointAttributeConfigBuilder = grpcPointAttributeConfigBuilder;
        this.deviceService = deviceService;
        this.driverAttributeConfigService = driverAttributeConfigService;
        this.pointAttributeConfigService = pointAttributeConfigService;
        this.pointService = pointService;
    }

    @Override
    public void selectByPage(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
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
            GrpcPage.Builder pageBuilder = GrpcPage.newBuilder();
            pageBuilder.setCurrent(devicePage.getCurrent());
            pageBuilder.setSize(devicePage.getSize());
            pageBuilder.setPages(devicePage.getPages());
            pageBuilder.setTotal(devicePage.getTotal());
            pageDeviceBuilder.setPage(pageBuilder);

            List<GrpcRDeviceAttachDTO> collect = devicePage.getRecords().stream().map(entityBO -> getDeviceAttachDTO(entityBO).build()).toList();
            pageDeviceBuilder.addAllData(collect);

            builder.setData(pageDeviceBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectById(GrpcDeviceQuery request, StreamObserver<GrpcRDeviceDTO> responseObserver) {
        GrpcRDeviceDTO.Builder builder = GrpcRDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceBO entityBO = deviceService.selectById(request.getDeviceId());
        if (Objects.isNull(entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcRDeviceAttachDTO.Builder dBuilder = getDeviceAttachDTO(entityBO);

            builder.setData(dBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private GrpcRDeviceAttachDTO.Builder getDeviceAttachDTO(DeviceBO entityBO) {
        GrpcRDeviceAttachDTO.Builder dBuilder = GrpcRDeviceAttachDTO.newBuilder();
        GrpcDeviceDTO deviceDTO = grpcDeviceBuilder.buildGrpcDTOByBO(entityBO);
        dBuilder.setDevice(deviceDTO);

        // 附加字段
        List<PointBO> pointBOList = pointService.selectByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(pointBOList).ifPresent(list -> dBuilder.addAllPointIds(list.stream().map(PointBO::getId).toList()));

        List<DriverAttributeConfigBO> driverAttributeConfigBOList = driverAttributeConfigService.selectByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(driverAttributeConfigBOList).ifPresent(list -> dBuilder.addAllDriverConfigs(list.stream()
                .map(grpcDriverAttributeConfigBuilder::buildGrpcDTOByBO)
                .toList()));

        List<PointAttributeConfigBO> pointAttributeConfigBOList = pointAttributeConfigService.selectByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(pointAttributeConfigBOList).ifPresent(list -> dBuilder.addAllPointConfigs(list.stream()
                .map(grpcPointAttributeConfigBuilder::buildGrpcDTOByBO)
                .toList()));
        return dBuilder;
    }
}
