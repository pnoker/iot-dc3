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


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcBaseDTO;
import io.github.pnoker.api.common.GrpcPageDTO;
import io.github.pnoker.api.common.GrpcRDTO;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.query.DriverQuery;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.BuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public void list(GrpcPageDriverQueryDTO request, StreamObserver<GrpcRPageDriverDTO> responseObserver) {
        GrpcRPageDriverDTO.Builder builder = GrpcRPageDriverDTO.newBuilder();
        GrpcRDTO.Builder rBuilder = GrpcRDTO.newBuilder();

        DriverQuery pageQuery = buildPageQuery(request);

        Page<DriverBO> driverPage = driverService.selectByPage(pageQuery);
        if (ObjectUtil.isNull(driverPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPageDriverDTO.Builder pageDriverBuilder = GrpcPageDriverDTO.newBuilder();
            GrpcPageDTO.Builder pageBuilder = GrpcPageDTO.newBuilder();
            pageBuilder.setCurrent(driverPage.getCurrent());
            pageBuilder.setSize(driverPage.getSize());
            pageBuilder.setPages(driverPage.getPages());
            pageBuilder.setTotal(driverPage.getTotal());
            pageDriverBuilder.setPage(pageBuilder);
            List<GrpcDriverDTO> collect = driverPage.getRecords().stream().map(this::buildDTOByDO).collect(Collectors.toList());
            pageDriverBuilder.addAllData(collect);

            builder.setData(pageDriverBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDeviceId(GrpcByDeviceQueryDTO request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcRDTO.Builder rBuilder = GrpcRDTO.newBuilder();

        DriverBO entityDO = driverService.selectByDeviceId(request.getDeviceId());
        if (ObjectUtil.isNull(entityDO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDriverDTO driverDTO = buildDTOByDO(entityDO);

            builder.setData(driverDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDriverId(GrpcByDriverQueryDTO request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcRDTO.Builder rBuilder = GrpcRDTO.newBuilder();
        Set<Long> ids =new HashSet<>();
        ids.add(request.getDriverId());
        List<DriverBO> driverBOS = driverService.selectByIds(ids);
        if (ObjectUtil.isNull(driverBOS)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDriverDTO driverDTO = buildDTOByDO(driverBOS.get(0));

            builder.setData(driverDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * DTO to Query
     *
     * @param request PageDriverQueryDTO
     * @return DriverPageQuery
     */
    private DriverQuery buildPageQuery(GrpcPageDriverQueryDTO request) {
        DriverQuery pageQuery = new DriverQuery();
        Pages pages = new Pages();
        pages.setCurrent(request.getPage().getCurrent());
        pages.setSize(request.getPage().getSize());
        pageQuery.setPage(pages);

        GrpcDriverDTO driver = request.getDriver();
        pageQuery.setDriverName(driver.getDriverName());
        pageQuery.setServiceName(driver.getServiceName());
        pageQuery.setServiceHost(driver.getServiceHost());
        pageQuery.setDriverTypeFlag(DriverTypeFlagEnum.ofIndex((byte) driver.getDriverTypeFlag()));
        pageQuery.setEnableFlag(EnableFlagEnum.ofIndex((byte) driver.getEnableFlag()));
        pageQuery.setTenantId(driver.getTenantId());

        return pageQuery;
    }

    /**
     * DO to DTO
     *
     * @param entityDO Driver
     * @return DriverDTO
     */
    private GrpcDriverDTO buildDTOByDO(DriverBO entityDO) {
        GrpcDriverDTO.Builder builder = GrpcDriverDTO.newBuilder();
        GrpcBaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setDriverName(entityDO.getDriverName());
        builder.setDriverCode(entityDO.getDriverCode());
        builder.setServiceName(entityDO.getServiceName());
        builder.setDriverTypeFlag(entityDO.getDriverTypeFlag().getIndex());
        builder.setServiceHost(entityDO.getServiceHost());
        builder.setEnableFlag(entityDO.getEnableFlag().getIndex());
        builder.setTenantId(entityDO.getTenantId());
        return builder.build();
    }
}
