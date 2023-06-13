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
            List<DriverDO> entityDOS = driverService.selectByProfileId(profile.getId());
            entityDOS.forEach(driver -> {
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
            List<DriverDO> entityDOS = driverService.selectByProfileId(point.getProfileId());
            entityDOS.forEach(driver -> {
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
            DriverDO entityDO = driverService.selectById(device.getDriverId());
            DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                    MetadataTypeEnum.DEVICE,
                    command,
                    JsonUtil.toJsonString(device)
            );
            notifyDriver(entityDO, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} device: {}", command, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum command, DriverAttributeConfig driverAttributeConfig) {
        try {
            DriverDO entityDO = driverService.selectByDeviceId(driverAttributeConfig.getDeviceId());
            DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                    MetadataTypeEnum.DRIVER_ATTRIBUTE_CONFIG,
                    command,
                    JsonUtil.toJsonString(driverAttributeConfig)
            );
            notifyDriver(entityDO, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} driverInfo: {}", command, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDriverPointInfo(MetadataCommandTypeEnum command, PointAttributeConfig pointAttributeConfig) {
        try {
            DriverDO entityDO = driverService.selectByDeviceId(pointAttributeConfig.getDeviceId());
            DriverMetadataDTO entityDTO = new DriverMetadataDTO(
                    MetadataTypeEnum.POINT_ATTRIBUTE_CONFIG,
                    command,
                    JsonUtil.toJsonString(pointAttributeConfig)
            );
            notifyDriver(entityDO, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} pointInfo: {}", command, e.getMessage());
        }
    }

    /**
     * notify driver
     *
     * @param entityDO  Driver
     * @param entityDTO DriverMetadataDTO
     */
    private void notifyDriver(DriverDO entityDO, DriverMetadataDTO entityDTO) {
        log.info("Notify driver[{}]: {}", entityDO.getServiceName(), entityDTO);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_METADATA, RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + entityDO.getServiceName(), entityDTO);
    }

}
