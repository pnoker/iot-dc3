/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.manager.grpc.server.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcDriverIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDriverQuery;
import io.github.pnoker.api.center.manager.GrpcPageDriverDTO;
import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.center.manager.GrpcRDriverDTO;
import io.github.pnoker.api.center.manager.GrpcRDriverListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageDriverDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.service.DriverService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling manager driver facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerDriverServer extends DriverApiGrpc.DriverApiImplBase {

    private final GrpcDriverBuilder grpcDriverBuilder;

    private final DriverService driverService;

    @Override
    public void listByPage(GrpcPageDriverQuery request, StreamObserver<GrpcRPageDriverDTO> responseObserver) {
        GrpcRPageDriverDTO.Builder builder = GrpcRPageDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverQuery query = grpcDriverBuilder.buildQueryByGrpcQuery(request);

        Page<DriverBO> entityPage = driverService.list(query);
        if (Objects.isNull(entityPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getRemark());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getRemark());

            GrpcPageDriverDTO.Builder pageBuilder = GrpcPageDriverDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(entityPage.getCurrent());
            page.setSize(entityPage.getSize());
            page.setPages(entityPage.getPages());
            page.setTotal(entityPage.getTotal());
            pageBuilder.setPage(page);

            List<GrpcDriverDTO> entityGrpcDTOList = entityPage.getRecords()
                    .stream()
                    .map(grpcDriverBuilder::buildGrpcDTOByBO)
                    .toList();
            pageBuilder.addAllData(entityGrpcDTOList);

            builder.setData(pageBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverBO entityDO = driverService.getByDeviceId(request.getDeviceId(), null);
        if (Objects.isNull(entityDO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getRemark());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getRemark());

            builder.setData(grpcDriverBuilder.buildGrpcDTOByBO(entityDO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listByDriverIds(GrpcDriverIdsQuery request, StreamObserver<GrpcRDriverListDTO> responseObserver) {
        GrpcRDriverListDTO.Builder builder = GrpcRDriverListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DriverBO> entityBOList = driverService.listByIds(new HashSet<>(request.getDriverIdsList()));
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getRemark());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getRemark());

            List<GrpcDriverDTO> entityGrpcDTOList = entityBOList.stream()
                    .map(grpcDriverBuilder::buildGrpcDTOByBO)
                    .toList();

            builder.addAllData(entityGrpcDTOList);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getByDriverId(GrpcDriverQuery request, StreamObserver<GrpcRDriverDTO> responseObserver) {
        GrpcRDriverDTO.Builder builder = GrpcRDriverDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverBO driverBO;
        try {
            driverBO = driverService.getById(request.getDriverId());
        } catch (NotFoundException e) {
            driverBO = null;
        }
        if (Objects.isNull(driverBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getRemark());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getRemark());

            builder.setData(grpcDriverBuilder.buildGrpcDTOByBO(driverBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
