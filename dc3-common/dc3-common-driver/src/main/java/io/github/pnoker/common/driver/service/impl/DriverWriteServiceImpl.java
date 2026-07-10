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
import io.github.pnoker.common.driver.support.ConnectionBackoff;
import io.github.pnoker.common.exception.WritePointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Default {@link DriverWriteService} implementation that resolves metadata, delegates the
 * actual write operation to the custom driver, and echoes the written value only on success.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
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
    public boolean write(Long deviceId, Long pointId, String value) {
        if (!ConnectionBackoff.shouldAttempt(deviceId)) {
            long remaining = ConnectionBackoff.remainingDelay(deviceId);
            log.debug("Backoff active for device {} ({}ms remaining)", deviceId, remaining);
            return false;
        }

        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new WritePointException("Failed to write point value, device[{}] is null", deviceId);
        }

        if (Objects.isNull(pointId) || Objects.isNull(device.getPointIds()) || !device.getPointIds().contains(pointId)) {
            throw new WritePointException("Failed to write point value, device[{}] not contained point[{}]",
                    deviceId, pointId);
        }

        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        if (Objects.isNull(driverConfig) || driverConfig.isEmpty()) {
            throw new WritePointException("Failed to write point value, driver config is empty, deviceId={}", deviceId);
        }
        Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);
        if (Objects.isNull(pointConfig) || pointConfig.isEmpty()) {
            throw new WritePointException("Failed to write point value, point config is empty, deviceId={}, pointId={}",
                    deviceId, pointId);
        }

        PointBO point = pointMetadata.getCache(pointId);
        if (Objects.isNull(point)) {
            throw new WritePointException("Failed to write point value, point is null, deviceId={}, pointId={}",
                    deviceId, pointId);
        }

        try {
            Boolean ok = driverCustomService.write(driverConfig, pointConfig, device, point,
                    new WritePointValue(value, point.getPointTypeFlag()));

            // Only echo the value back to the platform when the device acknowledged the write.
            // Failed writes must not produce fake success signals.
            if (Boolean.TRUE.equals(ok)) {
                driverSenderService.pointValueSender(PointValue.ofRawValue(device, point, value));
            }

            ConnectionBackoff.recordSuccess(deviceId);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            ConnectionBackoff.recordFailure(deviceId);
            throw e;
        }
    }

}
