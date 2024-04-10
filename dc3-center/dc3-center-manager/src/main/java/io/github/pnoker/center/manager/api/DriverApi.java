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
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.query.DriverQuery;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
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
    public void list(GrpcPageDriverQuery request, StreamObserver<GrpcRPageDriverDTO> responseObserver) {
        GrpcRPageDriverDTO.Builder builder = GrpcRPageDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverQuery pageQuery = buildQueryByGrpcQuery(request);

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
            GrpcPage.Builder pageBuilder = GrpcPage.newBuilder();
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
    public void selectByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

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
    public void selectByDriverId(GrpcDriverQuery request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();
        Set<Long> ids = new HashSet<>();
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
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDriverQuery
     * @return DriverQuery
     */
    private DriverQuery buildQueryByGrpcQuery(GrpcPageDriverQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery)) {
            return null;
        }

        DriverQuery query = new DriverQuery();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityQuery.getPage());
        query.setPage(pages);

        StringOptional.of(entityQuery.getDriverName()).ifPresent(query::setDriverName);
        StringOptional.of(entityQuery.getServiceName()).ifPresent(query::setServiceName);
        StringOptional.of(entityQuery.getServiceHost()).ifPresent(query::setServiceHost);
        query.setDriverTypeFlag(DriverTypeFlagEnum.ofIndex((byte) entityQuery.getDriverTypeFlag()));
        query.setEnableFlag(EnableFlagEnum.ofIndex((byte) entityQuery.getEnableFlag()));
        LongOptional.of(entityQuery.getTenantId()).ifPresent(query::setTenantId);

        return query;
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DriverBO
     * @return GrpcDriverDTO
     */
    private GrpcDriverDTO buildDTOByDO(DriverBO entityBO) {
        if (ObjectUtil.isNull(entityBO)) {
            return null;
        }

        GrpcDriverDTO.Builder builder = GrpcDriverDTO.newBuilder();
        GrpcBase baseDTO = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(baseDTO);

        builder.setDriverName(entityBO.getDriverName());
        builder.setDriverCode(entityBO.getDriverCode());
        builder.setServiceName(entityBO.getServiceName());
        builder.setDriverTypeFlag(entityBO.getDriverTypeFlag().getIndex());
        builder.setServiceHost(entityBO.getServiceHost());
        builder.setEnableFlag(entityBO.getEnableFlag().getIndex());
        builder.setTenantId(entityBO.getTenantId());
        return builder.build();
    }
}
