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

package io.github.pnoker.common.driver.receiver.rabbit;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.driver.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 接收驱动元数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class MetadataReceiver {

    @Resource
    PointMetadata pointMetadata;
    @Resource
    private DriverMetadata driverMetadata;
    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private MetadataEventPublisher metadataEventPublisher;

    @RabbitHandler
    @RabbitListener(queues = "#{metadataQueue.name}")
    public void metadataReceive(Channel channel, Message message, MetadataEventDTO entityDTO) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.info("Receive driver metadata: {}", JsonUtil.toJsonString(entityDTO));
            if (Objects.isNull(entityDTO)
                    || Objects.isNull(entityDTO.getMetadataType())
                    || Objects.isNull(entityDTO.getOperateType())) {
                log.error("Invalid driver metadata: {}", entityDTO);
                return;
            }

            if (MetadataTypeEnum.DEVICE.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType()) || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.info("Upsert device: {}", entityDTO.getId());
                    deviceMetadata.loadCache(entityDTO.getId());
                    driverMetadata.getDeviceIds().add(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.info("Delete device: {}", entityDTO.getId());
                    deviceMetadata.removeCache(entityDTO.getId());
                    driverMetadata.getDeviceIds().remove(entityDTO.getId());
                }

                // publish device metadata event
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.DEVICE, entityDTO.getOperateType()));
            } else if (MetadataTypeEnum.POINT.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType()) || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.info("Upsert point: {}", entityDTO.getId());
                    pointMetadata.loadCache(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.info("Delete point: {}", entityDTO.getId());
                    pointMetadata.removeCache(entityDTO.getId());
                }

                // publish point metadata event
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.POINT, entityDTO.getOperateType()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
