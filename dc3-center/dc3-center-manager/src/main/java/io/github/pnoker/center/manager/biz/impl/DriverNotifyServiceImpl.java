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

package io.github.pnoker.center.manager.biz.impl;

import io.github.pnoker.center.manager.biz.DriverNotifyService;
import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.dto.DriverTransferMetadataDTO;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
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
public class DriverNotifyServiceImpl implements DriverNotifyService {

    @Resource
    private DriverService driverService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void notifyProfile(MetadataCommandTypeEnum command, ProfileBO profileBO) {
        try {
            List<DriverBO> entityDOS = driverService.selectByProfileId(profileBO.getId());
            entityDOS.forEach(driver -> {
                DriverTransferMetadataDTO entityDTO = new DriverTransferMetadataDTO(
                        MetadataTypeEnum.PROFILE,
                        command,
                        JsonUtil.toJsonString(profileBO)
                );
                notifyDriver(driver, entityDTO);
            });
        } catch (Exception e) {
            log.warn("Notify driver {} profile error: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyPoint(MetadataCommandTypeEnum command, PointBO pointBO) {
        try {
            List<DriverBO> entityDOS = driverService.selectByProfileId(pointBO.getProfileId());
            entityDOS.forEach(driver -> {
                DriverTransferMetadataDTO entityDTO = new DriverTransferMetadataDTO(
                        MetadataTypeEnum.POINT,
                        command,
                        JsonUtil.toJsonString(pointBO)
                );
                notifyDriver(driver, entityDTO);
            });
        } catch (Exception e) {
            log.error("Notify driver {} point: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyDevice(MetadataCommandTypeEnum command, DeviceBO deviceBO) {
        try {
            DriverBO entityDO = driverService.selectById(deviceBO.getDriverId());
            DriverTransferMetadataDTO entityDTO = new DriverTransferMetadataDTO(
                    MetadataTypeEnum.DEVICE,
                    command,
                    JsonUtil.toJsonString(deviceBO)
            );
            notifyDriver(entityDO, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} device: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyDriverAttributeConfig(MetadataCommandTypeEnum command, DriverAttributeConfigBO driverAttributeConfigBO) {
        try {
            DriverBO entityDO = driverService.selectByDeviceId(driverAttributeConfigBO.getDeviceId());
            DriverTransferMetadataDTO entityDTO = new DriverTransferMetadataDTO(
                    MetadataTypeEnum.DRIVER_ATTRIBUTE_CONFIG,
                    command,
                    JsonUtil.toJsonString(driverAttributeConfigBO)
            );
            notifyDriver(entityDO, entityDTO);
        } catch (Exception e) {
            log.error("Notify driver {} driverInfo: {}", command, e.getMessage());
        }
    }

    @Override
    public void notifyPointAttributeConfig(MetadataCommandTypeEnum command, PointAttributeConfigBO pointAttributeConfigBO) {
        try {
            DriverBO entityDO = driverService.selectByDeviceId(pointAttributeConfigBO.getDeviceId());
            DriverTransferMetadataDTO entityDTO = new DriverTransferMetadataDTO(
                    MetadataTypeEnum.POINT_ATTRIBUTE_CONFIG,
                    command,
                    JsonUtil.toJsonString(pointAttributeConfigBO)
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
    private void notifyDriver(DriverBO entityDO, DriverTransferMetadataDTO entityDTO) {
        log.info("Notify driver[{}]: {}", entityDO.getServiceName(), entityDTO);
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_METADATA, RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + entityDO.getServiceName(), entityDTO);
    }

}
