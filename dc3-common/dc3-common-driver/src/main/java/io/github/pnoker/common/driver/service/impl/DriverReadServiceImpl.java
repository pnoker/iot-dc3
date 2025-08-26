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

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Implementation of the {@link DriverReadService} interface for handling driver read operations.
 * Provides functionality to read point values from a device using metadata, validates data, and sends
 * point values to a message queue.
 */
@Slf4j
@Service
public class DriverReadServiceImpl implements DriverReadService {

    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private PointMetadata pointMetadata;
    @Resource
    private DriverSenderService driverSenderService;
    @Resource
    private DriverCustomService driverCustomService;

    @Override
    public void read(Long deviceId, Long pointId) {
        // Get device from metadata cache
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ReadPointException("Failed to read point value, device[{}] is null", deviceId);
        }

        // Check if device contains the specified point
        if (!device.getPointIds().contains(pointId)) {
            throw new ReadPointException("Failed to read point value, device[{}] not contained point[{}]", deviceId, pointId);
        }

        // Get driver and point configurations
        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);

        // Get point from metadata cache
        PointBO point = pointMetadata.getCache(pointId);
        if (Objects.isNull(point)) {
            throw new ReadPointException("Failed to read point value, point[{}] is null" + deviceId);
        }

        // Read point value using custom driver service
        RValue rValue = driverCustomService.read(driverConfig, pointConfig, device, point);
        if (Objects.isNull(rValue)) {
            throw new ReadPointException("Failed to read point value, point value is null");
        }

        // Send point value to message queue
        driverSenderService.pointValueSender(new PointValue(rValue));
    }

    @Override
    public void read(DeviceCommandDTO commandDTO) {
        // Parse device read command from command DTO
        // Deserialize the command content into DeviceRead object
        DeviceCommandDTO.DeviceRead deviceRead = JsonUtil.parseObject(commandDTO.getContent(), DeviceCommandDTO.DeviceRead.class);
        if (Objects.isNull(deviceRead)) {
            return;
        }

        // Execute read command and log the process
        // Log the start of command execution with command details
        log.info("Start command of read: {}", JsonUtil.toJsonString(commandDTO));
        // Call the read method with device and point IDs
        read(deviceRead.getDeviceId(), deviceRead.getPointId());
        // Log the completion of command execution
        log.info("End command of read");
    }

}
