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
import io.github.pnoker.common.driver.grpc.client.DriverClient;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ consumer that keeps local metadata caches in sync with platform metadata
 * change events.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataReceiver {

    private final PointMetadata pointMetadata;

    private final DriverMetadata driverMetadata;

    private final DeviceMetadata deviceMetadata;

    private final DriverClient driverClient;

    private final MetadataEventPublisher metadataEventPublisher;

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
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // Validate metadata event first: the debug log below dereferences entityDTO,
            // so a null payload must be rejected before logging to avoid an NPE that
            // would otherwise fall through to the nack(requeue) path and requeue garbage.
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getId())
                    || Objects.isNull(entityDTO.getMetadataType())
                    || Objects.isNull(entityDTO.getOperateType())) {
                log.error("Invalid driver metadata: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            log.debug("Receive driver metadata: id={}, type={}, operate={}",
                    entityDTO.getId(), entityDTO.getMetadataType(), entityDTO.getOperateType());

            // Handle device metadata events
            if (MetadataTypeEnum.DEVICE.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType())
                        || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.debug("Upsert device: {}", entityDTO.getId());
                    // Add the id first so a refresh that races with a Quartz scan does
                    // not bypass the just-loaded entry; loadCache below either fills
                    // the cache or, on a null upstream, removes the orphan id again.
                    driverMetadata.getDeviceIds().add(entityDTO.getId());
                    deviceMetadata.loadCache(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.debug("Delete device: {}", entityDTO.getId());
                    // Remove the id before invalidating the cache so a Quartz scan
                    // hitting the cache between the two operations does not re-fetch
                    // the doomed device through the loader.
                    driverMetadata.getDeviceIds().remove(entityDTO.getId());
                    deviceMetadata.removeCache(entityDTO.getId());
                }

                // Publish device metadata event
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.DEVICE,
                        entityDTO.getOperateType()));
            }
            // Handle point metadata events
            else if (MetadataTypeEnum.POINT.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType())
                        || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.debug("Upsert point: {}", entityDTO.getId());
                    pointMetadata.loadCache(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.debug("Delete point: {}", entityDTO.getId());
                    pointMetadata.removeCache(entityDTO.getId());
                }

                // Publish point metadata event
                metadataEventPublisher.publishEvent(
                        new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.POINT, entityDTO.getOperateType()));
            } else if (MetadataTypeEnum.DRIVER.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.debug("Delete driver metadata: {}", entityDTO.getId());
                    driverMetadata.clear();
                    deviceMetadata.clearCache();
                    pointMetadata.clearCache();
                } else if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType())
                        || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.debug("Refresh driver metadata: {}", entityDTO.getId());
                    driverClient.refreshMetadata(entityDTO.getId());
                }

                metadataEventPublisher.publishEvent(
                        new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.DRIVER, entityDTO.getOperateType()));
            } else if (MetadataTypeEnum.COMMAND.equals(entityDTO.getMetadataType())
                    || MetadataTypeEnum.EVENT.equals(entityDTO.getMetadataType())) {
                log.debug("Forward {} metadata event: {}", entityDTO.getMetadataType(), entityDTO.getId());
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), entityDTO.getMetadataType(),
                        entityDTO.getOperateType()));
            } else {
                log.error("Unsupported metadata type: {}", entityDTO.getMetadataType());
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Driver metadata consume failed, metadataType={}, operateType={}, id={}, deliveryTag={}, routingKey={}",
                    Objects.nonNull(entityDTO) ? entityDTO.getMetadataType() : null,
                    Objects.nonNull(entityDTO) ? entityDTO.getOperateType() : null,
                    Objects.nonNull(entityDTO) ? entityDTO.getId() : null,
                    deliveryTag, message.getMessageProperties().getReceivedRoutingKey(), e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
