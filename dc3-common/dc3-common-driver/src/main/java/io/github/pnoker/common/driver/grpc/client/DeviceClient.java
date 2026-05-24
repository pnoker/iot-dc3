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

import io.github.pnoker.api.common.GrpcCommandAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcDriverAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcEventAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointAttributeConfigDTO;
import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcPageDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcPageDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcRDeviceAttachDTO;
import io.github.pnoker.api.common.driver.GrpcRDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcRPageDeviceDTO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.builder.DeviceBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcCommandAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcEventAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.dto.CommandAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.optional.CollectionOptional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * gRPC client used to query device metadata and device-specific attribute configuration
 * from the manager center.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceClient {

    private final DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    private final DriverMetadata driverMetadata;

    private final DeviceBuilder deviceBuilder;

    private final GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;

    private final GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;

    private final GrpcCommandAttributeConfigBuilder grpcCommandAttributeConfigBuilder;

    private final GrpcEventAttributeConfigBuilder grpcEventAttributeConfigBuilder;

    public List<DeviceBO> list() {
        long current = 1;
        GrpcRPageDeviceDTO rPageDeviceDTO = getGrpcRPageDeviceDTO(current);
        GrpcPageDeviceDTO pageDTO = rPageDeviceDTO.getData();
        List<GrpcRDeviceAttachDTO> dataList = pageDTO.getDataList();
        List<DeviceBO> deviceBOList = dataList.stream().map(this::buildDTOByGrpcAttachDTO).toList();
        List<DeviceBO> allDeviceBOList = new ArrayList<>(deviceBOList);

        long pages = pageDTO.getPage().getPages();
        while (current < pages) {
            current++;
            GrpcRPageDeviceDTO tPageDeviceDTO = getGrpcRPageDeviceDTO(current);
            GrpcPageDeviceDTO tPageDTO = tPageDeviceDTO.getData();
            List<GrpcRDeviceAttachDTO> tDataList = tPageDTO.getDataList();
            List<DeviceBO> tDeviceBOList = tDataList.stream().map(this::buildDTOByGrpcAttachDTO).toList();
            allDeviceBOList.addAll(tDeviceBOList);
            pages = tPageDTO.getPage().getPages();
        }
        return allDeviceBOList;
    }

    /**
     * Device ID
     *
     * @param id Device ID
     * @return DeviceBO
     */
    public DeviceBO getById(Long id) {
        GrpcDeviceQuery.Builder query = GrpcDeviceQuery.newBuilder();
        query.setDriverId(driverMetadata.getDriver().getId()).setDeviceId(id);
        GrpcRDeviceDTO rDeviceDTO = deviceApiBlockingStub.getById(query.build());
        if (!rDeviceDTO.getResult().getOk()) {
            log.error("Device doesn't exist: {}", id);
            return null;
        }

        GrpcRDeviceAttachDTO rDeviceAttachDTO = rDeviceDTO.getData();
        return buildDTOByGrpcAttachDTO(rDeviceAttachDTO);
    }

    private GrpcRPageDeviceDTO getGrpcRPageDeviceDTO(long current) {
        GrpcPageDeviceQuery.Builder query = GrpcPageDeviceQuery.newBuilder();
        GrpcPage.Builder page = GrpcPage.newBuilder();
        page.setCurrent(current);
        query.setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverMetadata.getDriver().getId())
                .setPage(page);
        GrpcRPageDeviceDTO rPageDeviceDTO = deviceApiBlockingStub.listByPage(query.build());
        if (!rPageDeviceDTO.getResult().getOk()) {
            throw new ServiceException("Failed to fetch device list");
        }
        return rPageDeviceDTO;
    }

    private DeviceBO buildDTOByGrpcAttachDTO(GrpcRDeviceAttachDTO rDeviceAttachDTO) {
        DeviceBO deviceBO = deviceBuilder.buildDTOByGrpcDTO(rDeviceAttachDTO.getDevice());
        deviceBO.setPointIds(new HashSet<>(rDeviceAttachDTO.getPointIdsList()));

        CollectionOptional.ofNullable(rDeviceAttachDTO.getDriverConfigsList()).ifPresent(value -> {
            Map<Long, DriverAttributeConfigDTO> driverAttributeConfigMap = value.stream()
                    .collect(Collectors.toMap(GrpcDriverAttributeConfigDTO::getAttributeId,
                            grpcDriverAttributeConfigBuilder::buildDTOByGrpcDTO));
            deviceBO.setDriverAttributeConfigIdMap(driverAttributeConfigMap);
        });

        CollectionOptional.ofNullable(rDeviceAttachDTO.getPointConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(GrpcPointAttributeConfigDTO::getPointId,
                            Collectors.toMap(GrpcPointAttributeConfigDTO::getAttributeId,
                                    grpcPointAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setPointAttributeConfigIdMap(pointAttributeConfigMap);
        });

        CollectionOptional.ofNullable(rDeviceAttachDTO.getCommandConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, CommandAttributeConfigDTO>> commandAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(GrpcCommandAttributeConfigDTO::getCommandId,
                            Collectors.toMap(GrpcCommandAttributeConfigDTO::getAttributeId,
                                    grpcCommandAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setCommandAttributeConfigIdMap(commandAttributeConfigMap);
        });

        CollectionOptional.ofNullable(rDeviceAttachDTO.getEventConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, EventAttributeConfigDTO>> eventAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(GrpcEventAttributeConfigDTO::getEventId,
                            Collectors.toMap(GrpcEventAttributeConfigDTO::getAttributeId,
                                    grpcEventAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setEventAttributeConfigIdMap(eventAttributeConfigMap);
        });

        return deviceBO;
    }

}
