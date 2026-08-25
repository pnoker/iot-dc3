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

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.SubscriptionMode;
import io.github.pnoker.common.driver.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.driver.grpc.client.DriverClient;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ consumer that keeps local metadata caches in sync with platform metadata
 * change events.
 *
 * @author pnoker
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
    @Dc3Listener(topic = MqTopic.METADATA, mode = SubscriptionMode.BROADCAST, group = "${dc3.driver.client}", keyPattern = "${dc3.driver.service}")
    public void metadataReceive(MqReceived<MetadataEventDTO> message, Acknowledgment ack) {
        MetadataEventDTO entityDTO = message.payload();
        try {
            // Validate metadata event first: the debug log below dereferences entityDTO,
            // so a null payload must be rejected before logging to avoid an NPE that
            // would otherwise fall through to the nack(requeue) path and requeue garbage.
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getId())
                    || Objects.isNull(entityDTO.getMetadataType())
                    || Objects.isNull(entityDTO.getOperateType())) {
                log.error("Invalid driver metadata: id={}, type={}, operate={}",
                        Objects.nonNull(entityDTO) ? entityDTO.getId() : null,
                        Objects.nonNull(entityDTO) ? entityDTO.getMetadataType() : null,
                        Objects.nonNull(entityDTO) ? entityDTO.getOperateType() : null);
                ack.reject(false);
                return;
            }

            log.debug("Receive driver metadata: id={}, type={}, operate={}",
                    entityDTO.getId(), entityDTO.getMetadataType(), entityDTO.getOperateType());

            // Handle device metadata events
            if (MetadataTypeEnum.DEVICE.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType())
                        || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.debug("Device metadata upserted, deviceId={}", entityDTO.getId());
                    // Metadata events invalidate/load data only. Ownership is assigned by
                    // the Manager lease service and is never inferred from an ADD event.
                    deviceMetadata.loadCache(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.debug("Device metadata deleted, deviceId={}", entityDTO.getId());
                    // Remove the id before invalidating the cache so a Quartz scan
                    // hitting the cache between the two operations does not re-fetch
                    // the doomed device through the loader.
                    driverMetadata.removeDeviceId(entityDTO.getId());
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
                    log.debug("Point metadata upserted, pointId={}", entityDTO.getId());
                    pointMetadata.loadCache(entityDTO.getId());
                } else if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.debug("Point metadata deleted, pointId={}", entityDTO.getId());
                    pointMetadata.removeCache(entityDTO.getId());
                }

                // Publish point metadata event
                metadataEventPublisher.publishEvent(
                        new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.POINT, entityDTO.getOperateType()));
            } else if (MetadataTypeEnum.DRIVER.equals(entityDTO.getMetadataType())) {
                if (MetadataOperateTypeEnum.DELETE.equals(entityDTO.getOperateType())) {
                    log.debug("Driver metadata deleted, driverId={}", entityDTO.getId());
                    driverMetadata.clear();
                    deviceMetadata.clearCache();
                    pointMetadata.clearCache();
                } else if (MetadataOperateTypeEnum.ADD.equals(entityDTO.getOperateType())
                        || MetadataOperateTypeEnum.UPDATE.equals(entityDTO.getOperateType())) {
                    log.debug("Driver metadata refreshed, driverId={}", entityDTO.getId());
                    driverClient.refreshMetadata(entityDTO.getId());
                }

                metadataEventPublisher.publishEvent(
                        new MetadataEvent(this, entityDTO.getId(), MetadataTypeEnum.DRIVER, entityDTO.getOperateType()));
            } else if (MetadataTypeEnum.COMMAND.equals(entityDTO.getMetadataType())
                    || MetadataTypeEnum.EVENT.equals(entityDTO.getMetadataType())) {
                log.debug("Driver metadata event forwarded, type={}, id={}",
                        entityDTO.getMetadataType(), entityDTO.getId());
                metadataEventPublisher.publishEvent(new MetadataEvent(this, entityDTO.getId(), entityDTO.getMetadataType(),
                        entityDTO.getOperateType()));
            } else {
                log.error("Driver metadata event rejected, reason=unsupportedType, type={}",
                        entityDTO.getMetadataType());
                ack.reject(false);
                return;
            }
            ack.ack();
        } catch (Exception e) {
            log.error("Driver metadata consume failed, metadataType={}, operateType={}, id={}",
                    Objects.nonNull(entityDTO) ? entityDTO.getMetadataType() : null,
                    Objects.nonNull(entityDTO) ? entityDTO.getOperateType() : null,
                    Objects.nonNull(entityDTO) ? entityDTO.getId() : null, e);
            ack.reject(true);
        }
    }

}
