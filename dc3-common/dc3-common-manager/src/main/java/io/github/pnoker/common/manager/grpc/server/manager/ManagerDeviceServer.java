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
import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcDriverQuery;
import io.github.pnoker.api.center.manager.GrpcPageDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcRDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcRDeviceListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageDeviceDTO;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.service.DeviceService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling manager-to-manager device requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {

    private final GrpcDeviceBuilder grpcDeviceBuilder;

    private final DeviceService deviceService;

    @Override
    public void listByPage(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
        GrpcRPageDeviceDTO.Builder builder = GrpcRPageDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceQuery query = grpcDeviceBuilder.buildQueryByGrpcQuery(request);

        Page<DeviceBO> entityPage = deviceService.list(query);
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

            List<GrpcDeviceDTO> entityGrpcDTOList = entityPage.getRecords()
                    .stream()
                    .map(grpcDeviceBuilder::buildGrpcDTOByBO)
                    .toList();
            pageBuilder.addAllData(entityGrpcDTOList);

            builder.setData(pageBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listByDriverId(GrpcDriverQuery driver, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> entityBOList = deviceService.listByDriverId(driver.getDriverId());
        if (CollectionUtils.isEmpty(entityBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> entityGrpcDTOList = entityBOList.stream()
                    .map(grpcDeviceBuilder::buildGrpcDTOByBO)
                    .toList();

            builder.addAllData(entityGrpcDTOList);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listByProfileId(GrpcProfileQuery request, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> entityBOList = deviceService.listByProfileId(request.getProfileId());
        if (CollectionUtils.isEmpty(entityBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> entityGrpcDTOList = entityBOList.stream()
                    .map(grpcDeviceBuilder::buildGrpcDTOByBO)
                    .toList();

            builder.addAllData(entityGrpcDTOList);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listByDeviceIds(GrpcDeviceIdsQuery request, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> entityBOList = deviceService.listByIds(request.getDeviceIdsList().stream().distinct().toList());
        if (CollectionUtils.isEmpty(entityBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> entityGrpcDTOList = entityBOList.stream()
                    .map(grpcDeviceBuilder::buildGrpcDTOByBO)
                    .toList();

            builder.addAllData(entityGrpcDTOList);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcRDeviceDTO> responseObserver) {
        GrpcRDeviceDTO.Builder builder = GrpcRDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceBO entityBO = deviceService.getById(request.getDeviceId());
        if (Objects.isNull(entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            builder.setData(grpcDeviceBuilder.buildGrpcDTOByBO(entityBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
