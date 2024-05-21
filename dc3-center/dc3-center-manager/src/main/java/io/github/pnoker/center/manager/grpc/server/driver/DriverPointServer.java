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
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.*;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.center.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
public class DriverPointServer extends PointApiGrpc.PointApiImplBase {

    private final GrpcPointBuilder grpcPointBuilder;
    private final PointService pointService;

    public DriverPointServer(GrpcPointBuilder grpcPointBuilder, PointService pointService) {
        this.grpcPointBuilder = grpcPointBuilder;
        this.pointService = pointService;
    }

    @Override
    public void selectByPage(GrpcPagePointQuery request, StreamObserver<GrpcRPagePointDTO> responseObserver) {
        GrpcRPagePointDTO.Builder builder = GrpcRPagePointDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        PointQuery pageQuery = grpcPointBuilder.buildQueryByGrpcQuery(request);
        pageQuery.setEnableFlag(EnableFlagEnum.ENABLE);

        Page<PointBO> pointPage = pointService.selectByPage(pageQuery);
        if (Objects.isNull(pointPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPagePointDTO.Builder pagePointBuilder = GrpcPagePointDTO.newBuilder();
            GrpcPage.Builder pageBuilder = GrpcPage.newBuilder();
            pageBuilder.setCurrent(pointPage.getCurrent());
            pageBuilder.setSize(pointPage.getSize());
            pageBuilder.setPages(pointPage.getPages());
            pageBuilder.setTotal(pointPage.getTotal());
            pagePointBuilder.setPage(pageBuilder);
            List<GrpcPointDTO> collect = pointPage.getRecords().stream().map(grpcPointBuilder::buildGrpcDTOByBO).toList();
            pagePointBuilder.addAllData(collect);

            builder.setData(pagePointBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectById(GrpcPointQuery request, StreamObserver<GrpcRPointDTO> responseObserver) {
        GrpcRPointDTO.Builder builder = GrpcRPointDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        PointBO pointBO = pointService.selectById(request.getPointId());
        if (Objects.isNull(pointBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPointDTO pointDTO = grpcPointBuilder.buildGrpcDTOByBO(pointBO);

            builder.setData(pointDTO);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
