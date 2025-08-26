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
 * @version 2025.6.0
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

    /**
     * Receive and process metadata events from RabbitMQ queue
     *
     * @param channel   RabbitMQ channel
     * @param message   RabbitMQ message
     * @param entityDTO Metadata event data transfer object
     */
    @RabbitHandler
    @RabbitListener(queues = "#{metadataQueue.name}")
    public void metadataReceive(Channel channel, Message message, MetadataEventDTO entityDTO) {
        try {
            // Acknowledge message receipt
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.info("Receive driver metadata: {}", JsonUtil.toJsonString(entityDTO));

            // Validate metadata event
            if (Objects.isNull(entityDTO)
                    || Objects.isNull(entityDTO.getMetadataType())
                    || Objects.isNull(entityDTO.getOperateType())) {
                log.error("Invalid driver metadata: {}", entityDTO);
                return;
            }

            // Handle device metadata events
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

                // Publish device metadata event
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.DEVICE, entityDTO.getOperateType()));
            }
            // Handle point metadata events
            else if (MetadataTypeEnum.POINT.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType()) || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.info("Upsert point: {}", entityDTO.getId());
                    pointMetadata.loadCache(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.info("Delete point: {}", entityDTO.getId());
                    pointMetadata.removeCache(entityDTO.getId());
                }

                // Publish point metadata event
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.POINT, entityDTO.getOperateType()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
