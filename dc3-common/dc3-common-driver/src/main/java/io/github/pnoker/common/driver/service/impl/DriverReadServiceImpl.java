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
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.exception.ReadPointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Default {@link DriverReadService} implementation that resolves metadata, delegates the
 * actual read operation to the custom driver, and publishes the resulting point value.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverReadServiceImpl implements DriverReadService {

    private final DeviceMetadata deviceMetadata;

    private final PointMetadata pointMetadata;

    private final DriverSenderService driverSenderService;

    private final DriverCustomService driverCustomService;

    @Override
    public void read(Long deviceId, Long pointId) {
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ReadPointException("Failed to read point value, device[{}] is null", deviceId);
        }

        if (Objects.isNull(pointId) || Objects.isNull(device.getPointIds()) || !device.getPointIds().contains(pointId)) {
            throw new ReadPointException("Failed to read point value, device[{}] not contained point[{}]", deviceId,
                    pointId);
        }

        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        if (Objects.isNull(driverConfig) || driverConfig.isEmpty()) {
            throw new ReadPointException("Failed to read point value, driver config is empty, deviceId={}", deviceId);
        }
        Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);
        if (Objects.isNull(pointConfig) || pointConfig.isEmpty()) {
            throw new ReadPointException("Failed to read point value, point config is empty, deviceId={}, pointId={}",
                    deviceId, pointId);
        }

        PointBO point = pointMetadata.getCache(pointId);
        if (Objects.isNull(point)) {
            throw new ReadPointException("Failed to read point value, point is null, deviceId={}, pointId={}",
                    deviceId, pointId);
        }

        ReadPointValue readPointValue = driverCustomService.read(driverConfig, pointConfig, device, point);
        if (Objects.isNull(readPointValue)) {
            throw new ReadPointException("Failed to read point value, point value is null");
        }

        driverSenderService.pointValueSender(new PointValue(readPointValue));
    }

}
