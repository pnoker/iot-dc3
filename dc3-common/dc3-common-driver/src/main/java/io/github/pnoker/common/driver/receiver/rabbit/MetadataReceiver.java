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
import reactor.core.publisher.Mono;

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
     * @param message broker-neutral metadata delivery
     * @param ack     poison-message disposition selector
     */
    @Dc3Listener(topic = MqTopic.METADATA, mode = SubscriptionMode.BROADCAST, group = "${dc3.driver.client}", keyPattern = "${dc3.driver.service}")
    public Mono<Void> metadataReceive(MqReceived<MetadataEventDTO> message, Acknowledgment ack) {
        MetadataEventDTO entityDTO = message.payload();
        return Mono.defer(() -> {
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getId())
                    || Objects.isNull(entityDTO.getMetadataType())
                    || Objects.isNull(entityDTO.getOperateType())) {
                log.error("Invalid driver metadata: id={}, type={}, operate={}",
                        Objects.nonNull(entityDTO) ? entityDTO.getId() : null,
                        Objects.nonNull(entityDTO) ? entityDTO.getMetadataType() : null,
                        Objects.nonNull(entityDTO) ? entityDTO.getOperateType() : null);
                ack.reject(false);
                return Mono.empty();
            }

            log.debug("Receive driver metadata: id={}, type={}, operate={}",
                    entityDTO.getId(), entityDTO.getMetadataType(), entityDTO.getOperateType());

            if (MetadataTypeEnum.DEVICE.equals(entityDTO.getMetadataType())) {
                return processDevice(entityDTO);
            }
            if (MetadataTypeEnum.POINT.equals(entityDTO.getMetadataType())) {
                return processPoint(entityDTO);
            }
            if (MetadataTypeEnum.DRIVER.equals(entityDTO.getMetadataType())) {
                return processDriver(entityDTO);
            }
            if (MetadataTypeEnum.COMMAND.equals(entityDTO.getMetadataType())
                    || MetadataTypeEnum.EVENT.equals(entityDTO.getMetadataType())) {
                log.debug("Driver metadata event forwarded, type={}, id={}",
                        entityDTO.getMetadataType(), entityDTO.getId());
                return publishEvent(entityDTO);
            }
            log.error("Driver metadata event rejected, reason=unsupportedType, type={}",
                    entityDTO.getMetadataType());
            ack.reject(false);
            return Mono.empty();
        }).doOnError(error -> log.error("Driver metadata consume failed, metadataType={}, operateType={}, id={}",
                Objects.nonNull(entityDTO) ? entityDTO.getMetadataType() : null,
                Objects.nonNull(entityDTO) ? entityDTO.getOperateType() : null,
                Objects.nonNull(entityDTO) ? entityDTO.getId() : null, error));
    }

    private Mono<Void> processDevice(MetadataEventDTO event) {
        Mono<?> operation;
        if (MetadataOperateTypeEnum.ADD.equals(event.getOperateType())
                || MetadataOperateTypeEnum.UPDATE.equals(event.getOperateType())) {
            log.debug("Device metadata upserted, deviceId={}", event.getId());
            operation = deviceMetadata.refreshCache(event.getId());
        } else if (MetadataOperateTypeEnum.DELETE.equals(event.getOperateType())) {
            operation = Mono.fromRunnable(() -> {
                log.debug("Device metadata deleted, deviceId={}", event.getId());
                driverMetadata.removeDeviceId(event.getId());
                deviceMetadata.removeCache(event.getId());
            });
        } else {
            operation = Mono.empty();
        }
        return operation.then(publishEvent(event));
    }

    private Mono<Void> processPoint(MetadataEventDTO event) {
        Mono<?> operation;
        if (MetadataOperateTypeEnum.ADD.equals(event.getOperateType())
                || MetadataOperateTypeEnum.UPDATE.equals(event.getOperateType())) {
            log.debug("Point metadata upserted, pointId={}", event.getId());
            operation = pointMetadata.refreshCache(event.getId());
        } else if (MetadataOperateTypeEnum.DELETE.equals(event.getOperateType())) {
            operation = Mono.fromRunnable(() -> {
                log.debug("Point metadata deleted, pointId={}", event.getId());
                pointMetadata.removeCache(event.getId());
            });
        } else {
            operation = Mono.empty();
        }
        return operation.then(publishEvent(event));
    }

    private Mono<Void> processDriver(MetadataEventDTO event) {
        Mono<Void> operation;
        if (MetadataOperateTypeEnum.DELETE.equals(event.getOperateType())) {
            operation = Mono.fromRunnable(() -> {
                log.debug("Driver metadata deleted, driverId={}", event.getId());
                driverMetadata.clear();
                deviceMetadata.clearCache();
                pointMetadata.clearCache();
            });
        } else if (MetadataOperateTypeEnum.ADD.equals(event.getOperateType())
                || MetadataOperateTypeEnum.UPDATE.equals(event.getOperateType())) {
            log.debug("Driver metadata refreshed, driverId={}", event.getId());
            operation = driverClient.refreshMetadata(event.getId());
        } else {
            operation = Mono.empty();
        }
        return operation.then(publishEvent(event));
    }

    private Mono<Void> publishEvent(MetadataEventDTO event) {
        return Mono.fromRunnable(() -> metadataEventPublisher.publishEvent(
                new MetadataEvent(this, event.getTenantId(), event.getId(), event.getMetadataType(), event.getOperateType())));
    }

}
