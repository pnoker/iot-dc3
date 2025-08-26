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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Implementation of the DriverWriteService interface, providing functionalities to handle
 * device point value write operations. This service interacts with metadata caches
 * and utilizes custom driver services for executing write commands.
 */
@Slf4j
@Service
public class DriverWriteServiceImpl implements DriverWriteService {

    @Resource
    private DriverMetadata driverMetadata;
    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private PointMetadata pointMetadata;
    @Resource
    private DriverCustomService driverCustomService;

    @Override
    public void write(Long deviceId, Long pointId, String value) {
        try {
            // Get device from metadata cache
            DeviceBO device = deviceMetadata.getCache(deviceId);
            if (Objects.isNull(device)) {
                throw new ReadPointException("Failed to write point value, device[{}] is null", deviceId);
            }

            // Check if device contains the specified point
            if (!device.getPointIds().contains(pointId)) {
                throw new ReadPointException("Failed to write point value, device[{}] not contained point[{}]", deviceId, pointId);
            }

            // Get driver and point configurations
            Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
            Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);

            // Get point from metadata cache
            PointBO point = pointMetadata.getCache(pointId);
            if (Objects.isNull(point)) {
                throw new ReadPointException("Failed to write point value, point[{}] is null" + deviceId);
            }

            // Write value to device through custom driver service
            driverCustomService.write(driverConfig, pointConfig, device, point, new WValue(value, point.getPointTypeFlag()));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public void write(DeviceCommandDTO commandDTO) {
        // Parse device write command from DTO content
        DeviceCommandDTO.DeviceWrite deviceWrite = JsonUtil.parseObject(commandDTO.getContent(), DeviceCommandDTO.DeviceWrite.class);
        if (Objects.isNull(deviceWrite)) {
            return;
        }

        // Log command execution start
        log.info("Start command of write: {}", JsonUtil.toJsonString(commandDTO));
        // Execute write operation
        write(deviceWrite.getDeviceId(), deviceWrite.getPointId(), deviceWrite.getValue());
        // Log command execution end
        log.info("End command of write: write");
    }

}
