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

package io.github.pnoker.common.driver.grpc.client;

import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcPointAttributeDTO;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.api.common.driver.GrpcRDriverRegisterDTO;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.bo.RegisterBO;
import io.github.pnoker.common.driver.entity.builder.DriverBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.exception.RegisterException;
import io.github.pnoker.common.optional.CollectionOptional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DriverClient {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DriverBuilder driverBuilder;
    @Resource
    private GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    @Resource
    private GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    /**
     * Register driver with metadata
     *
     * @param entityBO DriverRegisterBO containing driver registration information
     */
    public void driverRegister(RegisterBO entityBO) {
        // Build driver registration information
        GrpcDriverRegisterDTO.Builder builder = GrpcDriverRegisterDTO.newBuilder();
        GrpcDriverDTO grpcDriverDTO = driverBuilder.buildGrpcDTOByDTO(entityBO.getDriver());
        builder.setTenant(entityBO.getTenant())
                .setClient(entityBO.getClient())
                .setDriver(grpcDriverDTO);

        CollectionOptional.ofNullable(entityBO.getDriverAttributes()).ifPresent(value -> {
                    List<GrpcDriverAttributeDTO> grpcDriverAttributeDTOList = value.stream().map(grpcDriverAttributeBuilder::buildGrpcDTOByDTO).toList();
                    builder.addAllDriverAttributes(grpcDriverAttributeDTOList);
                }
        );
        CollectionOptional.ofNullable(entityBO.getPointAttributes()).ifPresent(value -> {
                    List<GrpcPointAttributeDTO> grpcPointAttributeDTOList = value.stream().map(grpcPointAttributeBuilder::buildGrpcDTOByDTO).toList();
                    builder.addAllPointAttributes(grpcPointAttributeDTOList);
                }
        );

        // Initiate driver registration
        GrpcRDriverRegisterDTO rDriverRegisterDTO = driverApiBlockingStub.driverRegister(builder.build());
        if (!rDriverRegisterDTO.getResult().getOk()) {
            throw new RegisterException(rDriverRegisterDTO.getResult().getMessage());
        }

        DriverBO driverBO = driverBuilder.buildDTOByGrpcDTO(rDriverRegisterDTO.getDriver());
        driverMetadata.setDriver(driverBO);

        driverMetadata.setDeviceIds(new HashSet<>(rDriverRegisterDTO.getDeviceIdsList()));

        List<GrpcDriverAttributeDTO> driverAttributesList = rDriverRegisterDTO.getDriverAttributesList();
        Map<Long, DriverAttributeDTO> driverAttributeIdMap = driverAttributesList.stream().collect(Collectors.toMap(entity -> entity.getBase().getId(), grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, DriverAttributeDTO> driverAttributeNameMap = driverAttributesList.stream().collect(Collectors.toMap(GrpcDriverAttributeDTO::getAttributeCode, grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setDriverAttributeIdMap(driverAttributeIdMap);
        driverMetadata.setDriverAttributeNameMap(driverAttributeNameMap);

        List<GrpcPointAttributeDTO> pointAttributesList = rDriverRegisterDTO.getPointAttributesList();
        Map<Long, PointAttributeDTO> pointAttributeIdMap = pointAttributesList.stream().collect(Collectors.toMap(entity -> entity.getBase().getId(), grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, PointAttributeDTO> pointAttributeNameMap = pointAttributesList.stream().collect(Collectors.toMap(GrpcPointAttributeDTO::getAttributeCode, grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setPointAttributeIdMap(pointAttributeIdMap);
        driverMetadata.setPointAttributeNameMap(pointAttributeNameMap);

        driverMetadata.setDriverStatus(DriverStatusEnum.ONLINE);
    }
}
