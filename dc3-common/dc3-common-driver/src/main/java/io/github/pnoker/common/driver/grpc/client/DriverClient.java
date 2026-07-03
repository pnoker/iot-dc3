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

import io.github.pnoker.api.common.GrpcCommandAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcEventAttributeDTO;
import io.github.pnoker.api.common.GrpcPointAttributeDTO;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDriverQuery;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.api.common.driver.GrpcRDriverRegisterDTO;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.bo.RegisterBO;
import io.github.pnoker.common.driver.entity.builder.DriverBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.driver.entity.dto.CommandAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.exception.RegisterException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.optional.CollectionOptional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * gRPC client responsible for driver registration and for loading the metadata returned
 * by the manager center after registration succeeds.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverClient {

    private final DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    private final DriverMetadata driverMetadata;

    private final DriverBuilder driverBuilder;

    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    private final GrpcCommandAttributeBuilder grpcCommandAttributeBuilder;

    private final GrpcEventAttributeBuilder grpcEventAttributeBuilder;

    /**
     * Registers the current driver and stores the returned metadata in the shared driver
     * cache.
     *
     * @param entityBO driver registration payload
     */
    public void driverRegister(RegisterBO entityBO) {
        // Build driver registration information
        GrpcDriverRegisterDTO.Builder builder = GrpcDriverRegisterDTO.newBuilder();
        GrpcDriverDTO grpcDriverDTO = driverBuilder.buildGrpcDTOByDTO(entityBO.getDriver());
        builder.setTenant(entityBO.getTenant()).setClient(entityBO.getClient()).setDriver(grpcDriverDTO);

        CollectionOptional.ofNullable(entityBO.getDriverAttributes()).ifPresent(value -> {
            List<GrpcDriverAttributeDTO> grpcDriverAttributeDTOList = value.stream()
                    .map(grpcDriverAttributeBuilder::buildGrpcDTOByDTO)
                    .toList();
            builder.addAllDriverAttributes(grpcDriverAttributeDTOList);
        });
        CollectionOptional.ofNullable(entityBO.getPointAttributes()).ifPresent(value -> {
            List<GrpcPointAttributeDTO> grpcPointAttributeDTOList = value.stream()
                    .map(grpcPointAttributeBuilder::buildGrpcDTOByDTO)
                    .toList();
            builder.addAllPointAttributes(grpcPointAttributeDTOList);
        });
        CollectionOptional.ofNullable(entityBO.getCommandAttributes()).ifPresent(value -> {
            List<GrpcCommandAttributeDTO> grpcCommandAttributeDTOList = value.stream()
                    .map(grpcCommandAttributeBuilder::buildGrpcDTOByDTO)
                    .toList();
            builder.addAllCommandAttributes(grpcCommandAttributeDTOList);
        });
        CollectionOptional.ofNullable(entityBO.getEventAttributes()).ifPresent(value -> {
            List<GrpcEventAttributeDTO> grpcEventAttributeDTOList = value.stream()
                    .map(grpcEventAttributeBuilder::buildGrpcDTOByDTO)
                    .toList();
            builder.addAllEventAttributes(grpcEventAttributeDTOList);
        });

        // Initiate driver registration
        GrpcRDriverRegisterDTO rDriverRegisterDTO = driverApiBlockingStub.driverRegister(builder.build());
        if (!rDriverRegisterDTO.getResult().getOk()) {
            throw new RegisterException(rDriverRegisterDTO.getResult().getMessage());
        }

        applyMetadata(rDriverRegisterDTO);
    }

    /**
     * Reloads the current driver metadata from manager without submitting registration
     * properties again.
     *
     * @param driverId registered driver id
     */
    public void refreshMetadata(Long driverId) {
        if (Objects.isNull(driverId) || driverId <= 0) {
            throw new ServiceException("Failed to refresh driver metadata: invalid driver id");
        }

        GrpcDriverQuery query = GrpcDriverQuery.newBuilder()
                .setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverId).build();
        GrpcRDriverRegisterDTO rDriverRegisterDTO = driverApiBlockingStub.getById(query);
        if (!rDriverRegisterDTO.getResult().getOk()) {
            throw new ServiceException(rDriverRegisterDTO.getResult().getMessage());
        }

        applyMetadata(rDriverRegisterDTO);
    }

    private void applyMetadata(GrpcRDriverRegisterDTO rDriverRegisterDTO) {
        DriverBO driverBO = driverBuilder.buildDTOByGrpcDTO(rDriverRegisterDTO.getDriver());
        driverMetadata.setDriver(driverBO);

        driverMetadata.setDeviceIds(new HashSet<>(rDriverRegisterDTO.getDeviceIdsList()));

        List<GrpcDriverAttributeDTO> driverAttributesList = rDriverRegisterDTO.getDriverAttributesList();
        Map<Long, DriverAttributeDTO> driverAttributeIdMap = driverAttributesList.stream()
                .collect(Collectors.toMap(entity -> entity.getBase().getId(),
                        grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, DriverAttributeDTO> driverAttributeNameMap = driverAttributesList.stream()
                .collect(Collectors.toMap(GrpcDriverAttributeDTO::getAttributeCode,
                        grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setDriverAttributeIdMap(driverAttributeIdMap);
        driverMetadata.setDriverAttributeNameMap(driverAttributeNameMap);

        List<GrpcPointAttributeDTO> pointAttributesList = rDriverRegisterDTO.getPointAttributesList();
        Map<Long, PointAttributeDTO> pointAttributeIdMap = pointAttributesList.stream()
                .collect(
                        Collectors.toMap(entity -> entity.getBase().getId(), grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, PointAttributeDTO> pointAttributeNameMap = pointAttributesList.stream()
                .collect(Collectors.toMap(GrpcPointAttributeDTO::getAttributeCode,
                        grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setPointAttributeIdMap(pointAttributeIdMap);
        driverMetadata.setPointAttributeNameMap(pointAttributeNameMap);

        List<GrpcCommandAttributeDTO> commandAttributesList = rDriverRegisterDTO.getCommandAttributesList();
        Map<Long, CommandAttributeDTO> commandAttributeIdMap = commandAttributesList.stream()
                .collect(Collectors.toMap(entity -> entity.getBase().getId(),
                        grpcCommandAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, CommandAttributeDTO> commandAttributeNameMap = commandAttributesList.stream()
                .collect(Collectors.toMap(GrpcCommandAttributeDTO::getAttributeCode,
                        grpcCommandAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setCommandAttributeIdMap(commandAttributeIdMap);
        driverMetadata.setCommandAttributeNameMap(commandAttributeNameMap);

        List<GrpcEventAttributeDTO> eventAttributesList = rDriverRegisterDTO.getEventAttributesList();
        Map<Long, EventAttributeDTO> eventAttributeIdMap = eventAttributesList.stream()
                .collect(Collectors.toMap(entity -> entity.getBase().getId(),
                        grpcEventAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, EventAttributeDTO> eventAttributeNameMap = eventAttributesList.stream()
                .collect(Collectors.toMap(GrpcEventAttributeDTO::getAttributeCode,
                        grpcEventAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setEventAttributeIdMap(eventAttributeIdMap);
        driverMetadata.setEventAttributeNameMap(eventAttributeNameMap);

        driverMetadata.setDriverStatus(EntityStatusEnum.ONLINE);
    }

}
