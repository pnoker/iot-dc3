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
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Default {@link DriverWriteService} implementation that resolves metadata, delegates the
 * actual write operation to the custom driver, and handles command payload execution.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverWriteServiceImpl implements DriverWriteService {

    private final DriverMetadata driverMetadata;

    private final DeviceMetadata deviceMetadata;

    private final PointMetadata pointMetadata;

    private final DriverCustomService driverCustomService;

    private final DriverSenderService driverSenderService;

    @Override
    public void write(Long deviceId, Long pointId, String value) {
        // Get device from metadata cache
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ReadPointException("Failed to write point value, device[{}] is null", deviceId);
        }

        // Check if device contains the specified point
        if (!device.getPointIds().contains(pointId)) {
            throw new ReadPointException("Failed to write point value, device[{}] not contained point[{}]",
                    deviceId, pointId);
        }

        // Get driver and point configurations
        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);

        // Get point from metadata cache
        PointBO point = pointMetadata.getCache(pointId);
        if (Objects.isNull(point)) {
            throw new ReadPointException("Failed to write point value, point is null, deviceId={}, pointId={}",
                    deviceId, pointId);
        }

        // Write value to device through custom driver service. Any exception thrown by
        // the custom driver implementation propagates to PointCommandReceiver where
        // ack/nack policy is decided — wrapping it here would erase the cause and make
        // a transient I/O failure look identical to a programming bug.
        driverCustomService.write(driverConfig, pointConfig, device, point,
                new WritePointValue(value, point.getPointTypeFlag()));

        // Echo the just-written value back to the platform as a synthetic point sample
        // so dashboards see the new state without waiting for the next read scan. The
        // value is the one the device acknowledged (echoed by the SDK from the request),
        // not a re-read — drivers that need read-after-write semantics should layer that
        // inside their own DriverProtocol.write implementation.
        driverSenderService.pointValueSender(new PointValue(new ReadPointValue(device, point, value)));
    }

    @Override
    public void write(PointCommandDTO commandDTO) {
        // Parse device write command from DTO content
        PointCommandDTO.PointWrite deviceWrite = JsonUtil.parseObject(commandDTO.getContent(),
                PointCommandDTO.PointWrite.class);
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
