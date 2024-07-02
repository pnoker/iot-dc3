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

package io.github.pnoker.common.manager.grpc.server.driver;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.*;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.optional.CollectionOptional;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
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

    @Resource
    private GrpcDeviceBuilder grpcDeviceBuilder;
    @Resource
    private GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;
    @Resource
    private GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;

    @Resource
    private DeviceService deviceService;
    @Resource
    private PointService pointService;
    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;
    @Resource
    private PointAttributeConfigService pointAttributeConfigService;

    @Override
    public void selectByPage(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
        GrpcRPageDeviceDTO.Builder builder = GrpcRPageDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceQuery query = grpcDeviceBuilder.buildQueryByGrpcQuery(request);

        Page<DeviceBO> entityPage = deviceService.selectByPage(query);
        if (Objects.isNull(entityPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPageDeviceDTO.Builder pageBuilder = GrpcPageDeviceDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(entityPage.getCurrent());
            page.setSize(entityPage.getSize());
            page.setPages(entityPage.getPages());
            page.setTotal(entityPage.getTotal());
            pageBuilder.setPage(page);

            List<GrpcRDeviceAttachDTO> entityGrpcDTOList = entityPage.getRecords().stream().map(entityBO -> getDeviceAttachDTO(entityBO).build()).toList();
            pageBuilder.addAllData(entityGrpcDTOList);

            builder.setData(pageBuilder);
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

            builder.setData(getDeviceAttachDTO(entityBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private GrpcRDeviceAttachDTO.Builder getDeviceAttachDTO(DeviceBO entityBO) {
        GrpcRDeviceAttachDTO.Builder builder = GrpcRDeviceAttachDTO.newBuilder();
        GrpcDeviceDTO entityGrpcDTO = grpcDeviceBuilder.buildGrpcDTOByBO(entityBO);
        builder.setDevice(entityGrpcDTO);

        // 附加字段
        List<PointBO> pointBOList = pointService.selectByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(pointBOList)
                .ifPresent(value -> builder.addAllPointIds(value.stream().map(PointBO::getId).toList()));

        List<DriverAttributeConfigBO> driverAttributeConfigBOList = driverAttributeConfigService.selectByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(driverAttributeConfigBOList)
                .ifPresent(value -> builder.addAllDriverConfigs(value.stream()
                        .map(grpcDriverAttributeConfigBuilder::buildGrpcDTOByBO)
                        .toList())
                );

        List<PointAttributeConfigBO> pointAttributeConfigBOList = pointAttributeConfigService.selectByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(pointAttributeConfigBOList)
                .ifPresent(value -> builder.addAllPointConfigs(value.stream()
                        .map(grpcPointAttributeConfigBuilder::buildGrpcDTOByBO)
                        .toList())
                );
        return builder;
    }
}
