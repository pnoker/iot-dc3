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
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * 设备 Api
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class DriverDriverServer extends DriverApiGrpc.DriverApiImplBase {

    @Resource
    private GrpcDriverBuilder grpcDriverBuilder;
    @Resource
    private GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    @Resource
    private GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    @Resource
    private DriverRegisterService driverRegisterService;
    @Resource
    private DeviceService deviceService;

    @Override
    public void driverRegister(GrpcDriverRegisterDTO request, StreamObserver<GrpcRDriverRegisterDTO> responseObserver) {
        GrpcRDriverRegisterDTO.Builder builder = GrpcRDriverRegisterDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            // 注册驱动
            DriverBO entityBO = driverRegisterService.registerDriver(request);
            GrpcDriverDTO entityGrpcDTO = grpcDriverBuilder.buildGrpcDTOByBO(entityBO);
            builder.setDriver(entityGrpcDTO);

            // 注册驱动属性
            List<DriverAttributeBO> driverAttributeBOList = driverRegisterService.registerDriverAttribute(request, entityBO);
            List<GrpcDriverAttributeDTO> grpcDriverAttributeDTOList = driverAttributeBOList.stream().map(grpcDriverAttributeBuilder::buildGrpcDTOByBO).toList();
            builder.addAllDriverAttributes(grpcDriverAttributeDTOList);

            // 注册位号属性
            List<PointAttributeBO> pointAttributeBOList = driverRegisterService.registerPointAttribute(request, entityBO);
            List<GrpcPointAttributeDTO> grpcPointAttributeDTOList = pointAttributeBOList.stream().map(grpcPointAttributeBuilder::buildGrpcDTOByBO).toList();
            builder.addAllPointAttributes(grpcPointAttributeDTOList);

            // 查询驱动下设备
            List<Long> idList = deviceService.selectIdsByDriverId(entityBO.getId());
            builder.addAllDeviceIds(idList);

            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());
        } catch (Exception e) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(e.getMessage());

            log.error(e.getMessage(), e);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
