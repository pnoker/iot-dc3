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

package io.github.pnoker.common.manager.grpc.server.driver;

import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcPointAttributeDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.api.common.driver.GrpcDriverQuery;
import io.github.pnoker.api.common.driver.GrpcRDriverRegisterDTO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.biz.DriverRegisterService;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * gRPC server handling driver-to-manager driver requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverDriverServer extends DriverApiGrpc.DriverApiImplBase {

    private final GrpcDriverBuilder grpcDriverBuilder;

    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    private final DriverRegisterService driverRegisterService;

    private final DriverService driverService;

    private final DriverAttributeService driverAttributeService;

    private final PointAttributeService pointAttributeService;

    private final DeviceService deviceService;

    @Override
    public void driverRegister(GrpcDriverRegisterDTO request, StreamObserver<GrpcRDriverRegisterDTO> responseObserver) {
        GrpcRDriverRegisterDTO.Builder builder = GrpcRDriverRegisterDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            //
            DriverBO entityBO = driverRegisterService.registerDriver(request);
            GrpcDriverDTO entityGrpcDTO = grpcDriverBuilder.buildGrpcDTOByBO(entityBO);
            builder.setDriver(entityGrpcDTO);

            //
            List<DriverAttributeBO> driverAttributeBOList = driverRegisterService.registerDriverAttribute(request,
                    entityBO);
            List<GrpcDriverAttributeDTO> grpcDriverAttributeDTOList = driverAttributeBOList.stream()
                    .map(grpcDriverAttributeBuilder::buildGrpcDTOByBO)
                    .toList();
            builder.addAllDriverAttributes(grpcDriverAttributeDTOList);

            //
            List<PointAttributeBO> pointAttributeBOList = driverRegisterService.registerPointAttribute(request,
                    entityBO);
            List<GrpcPointAttributeDTO> grpcPointAttributeDTOList = pointAttributeBOList.stream()
                    .map(grpcPointAttributeBuilder::buildGrpcDTOByBO)
                    .toList();
            builder.addAllPointAttributes(grpcPointAttributeDTOList);

            //
            List<Long> idList = deviceService.listIdsByDriverId(entityBO.getId());
            builder.addAllDeviceIds(idList);

            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());
        } catch (Exception e) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(e.getMessage());

            log.error("Driver register gRPC request failed, tenant={}, client={}, driver={}", request.getTenant(),
                    request.getClient(), request.getDriver(), e);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getById(GrpcDriverQuery request, StreamObserver<GrpcRDriverRegisterDTO> responseObserver) {
        GrpcRDriverRegisterDTO.Builder builder = GrpcRDriverRegisterDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            DriverBO entityBO = driverService.getById(request.getDriverId());
            if (Objects.isNull(entityBO)) {
                rBuilder.setOk(false);
                rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
                rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
            } else {
                buildMetadataResponse(builder, entityBO);

                rBuilder.setOk(true);
                rBuilder.setCode(ResponseEnum.OK.getCode());
                rBuilder.setMessage(ResponseEnum.OK.getText());
            }
        } catch (Exception e) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(e.getMessage());

            log.error("Driver metadata gRPC query failed, driverId={}", request.getDriverId(), e);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void buildMetadataResponse(GrpcRDriverRegisterDTO.Builder builder, DriverBO entityBO) {
        builder.setDriver(grpcDriverBuilder.buildGrpcDTOByBO(entityBO));

        List<GrpcDriverAttributeDTO> driverAttributeDTOList = Optional
                .ofNullable(driverAttributeService.listByDriverId(entityBO.getId()))
                .orElseGet(List::of)
                .stream()
                .filter(attribute -> Objects.equals(entityBO.getTenantId(), attribute.getTenantId()))
                .map(grpcDriverAttributeBuilder::buildGrpcDTOByBO)
                .toList();
        builder.addAllDriverAttributes(driverAttributeDTOList);

        List<GrpcPointAttributeDTO> pointAttributeDTOList = Optional
                .ofNullable(pointAttributeService.listByDriverId(entityBO.getId()))
                .orElseGet(List::of)
                .stream()
                .filter(attribute -> Objects.equals(entityBO.getTenantId(), attribute.getTenantId()))
                .map(grpcPointAttributeBuilder::buildGrpcDTOByBO)
                .toList();
        builder.addAllPointAttributes(pointAttributeDTOList);

        List<Long> idList = Optional.ofNullable(deviceService.listIdsByDriverId(entityBO.getId())).orElseGet(List::of);
        builder.addAllDeviceIds(idList);
    }

}
