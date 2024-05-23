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


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.query.DriverQuery;
import io.github.pnoker.center.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Objects;

/**
 * Driver Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class ManagerDriverServer extends DriverApiGrpc.DriverApiImplBase {

    private final GrpcDriverBuilder grpcDriverBuilder;
    private final DriverService driverService;

    public ManagerDriverServer(GrpcDriverBuilder grpcDriverBuilder, DriverService driverService) {
        this.grpcDriverBuilder = grpcDriverBuilder;
        this.driverService = driverService;
    }

    @Override
    public void list(GrpcPageDriverQuery request, StreamObserver<GrpcRPageDriverDTO> responseObserver) {
        GrpcRPageDriverDTO.Builder builder = GrpcRPageDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverQuery pageQuery = grpcDriverBuilder.buildQueryByGrpcQuery(request);

        Page<DriverBO> driverPage = driverService.selectByPage(pageQuery);
        if (Objects.isNull(driverPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPageDriverDTO.Builder pageDriverBuilder = GrpcPageDriverDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(driverPage.getCurrent());
            page.setSize(driverPage.getSize());
            page.setPages(driverPage.getPages());
            page.setTotal(driverPage.getTotal());
            pageDriverBuilder.setPage(page);
            List<GrpcDriverDTO> collect = driverPage.getRecords().stream().map(grpcDriverBuilder::buildGrpcDTOByBO).toList();
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
        if (Objects.isNull(entityDO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDriverDTO driverDTO = grpcDriverBuilder.buildGrpcDTOByBO(entityDO);

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

        DriverBO driverBO = driverService.selectById(request.getDriverId());
        if (Objects.isNull(driverBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcDriverDTO driverDTO = grpcDriverBuilder.buildGrpcDTOByBO(driverBO);

            builder.setData(driverDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
