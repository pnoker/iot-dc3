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


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.BaseDTO;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.PageDTO;
import io.github.pnoker.api.common.RDTO;
import io.github.pnoker.center.manager.entity.query.DevicePageQuery;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.utils.BuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;
import java.util.List;
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
    public void list(PageDeviceQueryDTO request, StreamObserver<RPageDeviceDTO> responseObserver) {
        RPageDeviceDTO.Builder builder = RPageDeviceDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();

        DevicePageQuery pageQuery = buildPageQuery(request);

        Page<Device> devicePage = deviceService.list(pageQuery);
        if (ObjectUtil.isNull(devicePage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());

            PageDeviceDTO.Builder pageDeviceBuilder = PageDeviceDTO.newBuilder();
            PageDTO.Builder pageBuilder = PageDTO.newBuilder();
            pageBuilder.setCurrent(devicePage.getCurrent());
            pageBuilder.setSize(devicePage.getSize());
            pageBuilder.setPages(devicePage.getPages());
            pageBuilder.setTotal(devicePage.getTotal());
            pageDeviceBuilder.setPage(pageBuilder);
            List<DeviceDTO> collect = devicePage.getRecords().stream().map(this::buildDTOByDO).collect(Collectors.toList());
            pageDeviceBuilder.addAllData(collect);

            builder.setData(pageDeviceBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByProfileId(ByProfileQueryDTO request, StreamObserver<RDeviceListDTO> responseObserver) {
        RDeviceListDTO.Builder builder = RDeviceListDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();

        List<Device> devices = deviceService.selectByProfileId(request.getProfileId());
        if (CollUtil.isEmpty(devices)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());

            List<DeviceDTO> deviceDTOS = devices.stream().map(this::buildDTOByDO).collect(Collectors.toList());

            builder.addAllData(deviceDTOS);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * DTO to Query
     *
     * @param request PageDeviceQueryDTO
     * @return DevicePageQuery
     */
    private DevicePageQuery buildPageQuery(PageDeviceQueryDTO request) {
        DevicePageQuery pageQuery = new DevicePageQuery();
        Pages pages = new Pages();
        pages.setCurrent(request.getPage().getCurrent());
        pages.setSize(request.getPage().getSize());
        pageQuery.setPage(pages);

        DeviceDTO device = request.getDevice();
        pageQuery.setProfileId(request.getProfileId());
        pageQuery.setDeviceName(device.getDeviceName());
        pageQuery.setDriverId(device.getDriverId());
        pageQuery.setTenantId(device.getTenantId());
        pageQuery.setEnableFlag(EnableFlagEnum.ofName(device.getEnableFlag().name()));

        return pageQuery;
    }

    /**
     * DO to DTO
     *
     * @param entityDO Device
     * @return DeviceDTO
     */
    private DeviceDTO buildDTOByDO(Device entityDO) {
        DeviceDTO.Builder builder = DeviceDTO.newBuilder();
        BaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setDeviceName(entityDO.getDeviceName());
        builder.setDeviceCode(entityDO.getDeviceCode());
        builder.setDriverId(entityDO.getDriverId());
        builder.setGroupId(entityDO.getGroupId());
        builder.setEnableFlag(EnableFlagDTOEnum.valueOf(entityDO.getEnableFlag().name()));
        builder.setTenantId(entityDO.getTenantId());
        return builder.build();
    }

}
