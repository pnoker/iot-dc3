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

package io.github.pnoker.common.manager.event.metadata;

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 元数据事件 Listener
 *
 * @author zhangzi
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class MetadataEventListener implements ApplicationListener<MetadataEvent> {

    @Resource
    private DriverService driverService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Async
    @Override
    public void onApplicationEvent(MetadataEvent metadataEvent) {
        log.info("Metadata event listener received: {}", JsonUtil.toJsonString(metadataEvent));
        try {
            Long id = metadataEvent.getId();
            MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
            MetadataEventDTO entityDTO = new MetadataEventDTO(id, metadataType, metadataEvent.getOperateType());
            if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
                DriverBO entityBO = driverService.selectByDeviceId(id);
                notifyDriver(entityBO.getServiceName(), entityDTO);
            } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
                List<DriverBO> entityBOList = driverService.selectByPointId(id);
                entityBOList.forEach(entityBO -> notifyDriver(entityBO.getServiceName(), entityDTO));
            }
        } catch (Exception e) {
            log.error("Metadata event listener error: {}", e.getMessage(), e);
        }
    }

    /**
     * 通知驱动
     *
     * @param service   驱动服务
     * @param entityDTO DriverTransferMetadataDTO
     */
    private void notifyDriver(String service, MetadataEventDTO entityDTO) {
        log.info("Notify driver[{}]: {}", service, JsonUtil.toJsonString(entityDTO));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_METADATA, RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + service, entityDTO);
    }
}
