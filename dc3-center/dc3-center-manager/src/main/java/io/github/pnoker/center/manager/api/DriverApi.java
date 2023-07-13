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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.*;
import io.github.pnoker.center.manager.entity.query.DriverPageQuery;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.model.DriverDO;
import io.github.pnoker.common.utils.BuilderUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;
import java.util.List;
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
    public void list(PageDriverQueryDTO request, StreamObserver<RPageDriverDTO> responseObserver) {
        RPageDriverDTO.Builder builder = RPageDriverDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();

        DriverPageQuery pageQuery = buildPageQuery(request);

        Page<DriverDO> driverPage = driverService.list(pageQuery);
        if (ObjectUtil.isNull(driverPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());

            PageDriverDTO.Builder pageDriverBuilder = PageDriverDTO.newBuilder();
            PageDTO.Builder pageBuilder = PageDTO.newBuilder();
            pageBuilder.setCurrent(driverPage.getCurrent());
            pageBuilder.setSize(driverPage.getSize());
            pageBuilder.setPages(driverPage.getPages());
            pageBuilder.setTotal(driverPage.getTotal());
            pageDriverBuilder.setPage(pageBuilder);
            List<DriverDTO> collect = driverPage.getRecords().stream().map(this::buildDTOByDO).collect(Collectors.toList());
            pageDriverBuilder.addAllData(collect);

            builder.setData(pageDriverBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDeviceId(ByDeviceQueryDTO request, StreamObserver<RDriverDTO> responseObserver) {
        RDriverDTO.Builder builder = RDriverDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();

        DriverDO entityDO = driverService.selectByDeviceId(request.getDeviceId());
        if (ObjectUtil.isNull(entityDO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());

            DriverDTO driverDTO = buildDTOByDO(entityDO);

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
    private DriverPageQuery buildPageQuery(PageDriverQueryDTO request) {
        DriverPageQuery pageQuery = new DriverPageQuery();
        Pages pages = new Pages();
        pages.setCurrent(request.getPage().getCurrent());
        pages.setSize(request.getPage().getSize());
        pageQuery.setPage(pages);

        DriverDTO driver = request.getDriver();
        pageQuery.setDriverName(driver.getDriverName());
        pageQuery.setServiceName(driver.getServiceName());
        pageQuery.setServiceHost(driver.getServiceHost());
        pageQuery.setTenantId(driver.getTenantId());
        pageQuery.setDriverTypeFlag(DriverTypeFlagEnum.ofName(driver.getDriverTypeFlag().name()));
        pageQuery.setEnableFlag(EnableFlagEnum.ofName(driver.getEnableFlag().name()));
        return pageQuery;
    }

    /**
     * DO to DTO
     *
     * @param entityDO Driver
     * @return DriverDTO
     */
    private DriverDTO buildDTOByDO(DriverDO entityDO) {
        DriverDTO.Builder builder = DriverDTO.newBuilder();
        BaseDTO baseDTO = BuilderUtil.buildBaseDTOByDO(entityDO);
        builder.setBase(baseDTO);
        builder.setDriverName(entityDO.getDriverName());
        builder.setDriverCode(entityDO.getDriverCode());
        builder.setServiceName(entityDO.getServiceName());
        builder.setDriverTypeFlag(DriverTypeFlagDTOEnum.valueOf(entityDO.getDriverTypeFlag().name()));
        builder.setServiceHost(entityDO.getServiceHost());
        builder.setEnableFlag(EnableFlagDTOEnum.valueOf(entityDO.getEnableFlag().name()));
        builder.setTenantId(entityDO.getTenantId());
        return builder.build();
    }
}
