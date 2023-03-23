/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.service.impl;

import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.dto.DriverMetadataDTO;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.model.*;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * NotifyService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    @Resource
    private DriverService driverService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverProfile(MetadataCommandTypeEnum command, Profile profile) {
        try {
            List<Driver> drivers = driverService.selectByProfileId(profile.getId());
            drivers.forEach(driver -> {
                DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                        MetadataTypeEnum.PROFILE,
                        command,
                        JsonUtil.toJsonString(profile)
                );
                notifyDriver(driver, entityDTO);
            });
        } catch (Exception e) {
            log.warn("Notify driver {} profile error: {}", command, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverPoint(MetadataCommandTypeEnum command, Point point) {
        try {
            List<Driver> drivers = driverService.selectByProfileId(point.getProfileId());
            drivers.forEach(driver -> {
                DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                        MetadataTypeEnum.POINT,
                        command,
                        JsonUtil.toJsonString(point)
                );
                notifyDriver(driver, entityDTO);
            });
        } catch (Exception e) {
            log.error("Notify driver {} point: {}", command, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverDevice(MetadataCommandTypeEnum command, Device device) {
        try {
            Driver driver = driverService.selectById(device.getDriverId());
            DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                    MetadataTypeEnum.DEVICE,
                    command,
                    JsonUtil.toJsonString(device)
            );
            notifyDriver(driver, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} device: {}", command, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverDriverInfo(MetadataCommandTypeEnum command, DriverInfo driverInfo) {
        try {
            Driver driver = driverService.selectByDeviceId(driverInfo.getDeviceId());
            DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                    MetadataTypeEnum.DRIVER_INFO,
                    command,
                    JsonUtil.toJsonString(driverInfo)
            );
            notifyDriver(driver, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} driverInfo: {}", command, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverPointInfo(MetadataCommandTypeEnum command, PointInfo pointInfo) {
        try {
            Driver driver = driverService.selectByDeviceId(pointInfo.getDeviceId());
            DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                    MetadataTypeEnum.POINT_INFO,
                    command,
                    JsonUtil.toJsonString(pointInfo)
            );
            notifyDriver(driver, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} pointInfo: {}", command, e.getMessage());
        }
    }

    /**
     * notify driver
     *
     * @param driver    Driver
     * @param entityDTO DriverMetadataDTO
     */
    private void notifyDriver(Driver driver, DriverMetadataDTO entityDTO) {
        log.info("Notify driver[{}]: {}", driver.getServiceName(), entityDTO);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_METADATA, RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driver.getServiceName(), entityDTO);
    }

}
