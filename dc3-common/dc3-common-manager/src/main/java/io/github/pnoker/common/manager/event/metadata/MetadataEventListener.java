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
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 元数据事件 Listener
 *
 * @author zhangzi
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
    public void onApplicationEvent(@NotNull MetadataEvent metadataEvent) {
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
