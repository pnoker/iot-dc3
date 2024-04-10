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

package io.github.pnoker.center.manager.api;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Device Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class DeviceApi extends DeviceApiGrpc.DeviceApiImplBase {

    @Resource
    private DeviceService deviceService;

    @Override
    public void list(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
        GrpcRPageDeviceDTO.Builder builder = GrpcRPageDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceQuery pageQuery = buildQueryByGrpcQuery(request);

        Page<DeviceBO> devicePage = deviceService.selectByPage(pageQuery);
        if (ObjectUtil.isNull(devicePage)) {
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
            List<GrpcDeviceDTO> collect = devicePage.getRecords().stream().map(this::buildDTOByDO).collect(Collectors.toList());
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
        List<DeviceBO> deviceBOS = deviceService.selectByDriverId(driver.getDriverId());
        if (CollUtil.isEmpty(deviceBOS)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());
            List<GrpcDeviceDTO> deviceDTOS = deviceBOS.stream().map(this::buildDTOByDO).collect(Collectors.toList());
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

        List<DeviceBO> deviceBOS = deviceService.selectByProfileId(request.getProfileId());
        if (CollUtil.isEmpty(deviceBOS)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> deviceDTOS = deviceBOS.stream().map(this::buildDTOByDO).collect(Collectors.toList());

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
        Set<Long> ids = new HashSet<>();
        ids.add(request.getDeviceId());
        List<DeviceBO> deviceBOS = deviceService.selectByIds(ids);
        if (ObjectUtil.isNull(deviceBOS)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDeviceDTO deviceDTO = buildDTOByDO(deviceBOS.get(0));

            builder.setData(deviceDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDeviceQuery
     * @return DeviceQuery
     */
    private DeviceQuery buildQueryByGrpcQuery(GrpcPageDeviceQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery)) {
            return null;
        }

        DeviceQuery query = new DeviceQuery();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityQuery.getPage());
        query.setPage(pages);

        query.setProfileId(entityQuery.getProfileId() > DefaultConstant.DEFAULT_INT ? entityQuery.getProfileId() : null);
        query.setDeviceName(entityQuery.getDeviceName());
        query.setDriverId(entityQuery.getDriverId() > DefaultConstant.DEFAULT_INT ? entityQuery.getDriverId() : null);
        query.setEnableFlag(EnableFlagEnum.ofIndex((byte) entityQuery.getEnableFlag()));
        query.setTenantId(entityQuery.getTenantId());

        return query;
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DeviceBO
     * @return GrpcDeviceDTO
     */
    private GrpcDeviceDTO buildDTOByDO(DeviceBO entityBO) {
        if (ObjectUtil.isNull(entityBO)) {
            return null;
        }

        GrpcDeviceDTO.Builder builder = GrpcDeviceDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO);

        builder.setDeviceName(entityBO.getDeviceName());
        builder.setDeviceCode(entityBO.getDeviceCode());
        builder.setDriverId(entityBO.getDriverId());
        builder.setGroupId(entityBO.getGroupId());
        builder.setEnableFlag(entityBO.getEnableFlag().getIndex());
        builder.setTenantId(entityBO.getTenantId());
        return builder.build();
    }

}
