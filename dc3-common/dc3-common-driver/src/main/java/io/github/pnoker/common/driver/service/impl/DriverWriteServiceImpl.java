/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author pnoker
 * @since 2022.1.0
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
            DeviceBO device = deviceMetadata.getCache(deviceId);
            if (Objects.isNull(device)) {
                throw new ReadPointException("Failed to write point value, device[{}] is null", deviceId);
            }

            if (!device.getPointIds().contains(pointId)) {
                throw new ReadPointException("Failed to write point value, device[{}] not contained point[{}]", deviceId, pointId);
            }

            Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
            Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);

            PointBO point = pointMetadata.getCache(pointId);
            if (Objects.isNull(point)) {
                throw new ReadPointException("Failed to write point value, point[{}] is null" + deviceId);
            }

            driverCustomService.write(driverConfig, pointConfig, device, point, new WValue(value, point.getPointTypeFlag()));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public void write(DeviceCommandDTO commandDTO) {
        DeviceCommandDTO.DeviceWrite deviceWrite = JsonUtil.parseObject(commandDTO.getContent(), DeviceCommandDTO.DeviceWrite.class);
        if (Objects.isNull(deviceWrite)) {
            return;
        }

        log.info("Start command of write: {}", JsonUtil.toJsonString(commandDTO));
        write(deviceWrite.getDeviceId(), deviceWrite.getPointId(), deviceWrite.getValue());
        log.info("End command of write: write");
    }

}
