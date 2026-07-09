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
import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.entity.dto.PointCommandPayload;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * RabbitMQ consumer that dispatches point read and write commands to the corresponding
 * services. Performs expire-at pre-check, idempotent deduplication, and sends result
 * receipts back to the data center.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandReceiver {

    private static final int SCHEMA_VERSION = 1;
    private final DriverReadService driverReadService;
    private final DriverWriteService driverWriteService;
    private final DriverSenderService driverSenderService;
    private final CommandDedupCache dedupCache;
    private final DeviceLockManager deviceLockManager;

    @RabbitHandler
    @RabbitListener(queues = "#{pointCommandQueue.name}")
    public void pointCommandReceive(Channel channel, Message message, PointCommandDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean redelivered = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        try {
            log.debug("Receive point command: commandId={}, type={}", entityDTO.commandId(), entityDTO.type());

            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.commandId())
                    || Objects.isNull(entityDTO.tenantId()) || Objects.isNull(entityDTO.type())
                    || Objects.isNull(entityDTO.payload())) {
                log.error("Invalid point command: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            if (isInvalidPayload(entityDTO.payload())) {
                log.error("Invalid point command payload: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            String commandId = entityDTO.commandId();
            Long tenantId = entityDTO.tenantId();

            // Expire-at pre-check
            if (Objects.nonNull(entityDTO.expireAt()) && Instant.now().isAfter(entityDTO.expireAt())) {
                log.warn("Command already expired: commandId={}, expireAt={}", commandId, entityDTO.expireAt());
                sendResult(commandId, tenantId, PointCommandStatusEnum.EXPIRED,
                        null, "EXPIRED", "Command expired before execution", channel, deliveryTag);
                return;
            }

            // Dedup check
            if (!dedupCache.tryAcquire(commandId)) {
                log.warn("Duplicate command detected: commandId={}", commandId);
                sendResult(commandId, tenantId, PointCommandStatusEnum.DUPLICATE,
                        null, "DUPLICATE", "Command already processed", channel, deliveryTag);
                return;
            }

            // Extract deviceId for per-device serialization
            Long lockDeviceId = switch (entityDTO.payload()) {
                case PointCommandPayload.ReadPayload r -> r.deviceId();
                case PointCommandPayload.WritePayload w -> w.deviceId();
            };

            // Dispatch under per-device lock to prevent protocol interleaving
            String responseValue = deviceLockManager.runExclusive(lockDeviceId, () -> {
                String rv = null;
                switch (entityDTO.payload()) {
                    case PointCommandPayload.ReadPayload r -> {
                        driverReadService.read(r.deviceId(), r.pointId());
                    }
                    case PointCommandPayload.WritePayload w -> {
                        boolean ok = driverWriteService.write(w.deviceId(), w.pointId(), w.value());
                        if (ok) {
                            rv = w.value();
                        }
                    }
                }
                return rv;
            });

            if (Objects.isNull(responseValue) && entityDTO.payload()
                    instanceof PointCommandPayload.WritePayload) {
                sendResult(commandId, tenantId, PointCommandStatusEnum.FAILED,
                        null, "WRITE_FAILED", "Device write returned false", channel, deliveryTag);
                return;
            }

            sendResult(commandId, tenantId, PointCommandStatusEnum.SUCCESS,
                    responseValue, null, null, channel, deliveryTag);

        } catch (Exception e) {
            if (redelivered) {
                log.error("Point command failed on redelivery, sending FAILED. deliveryTag={}", deliveryTag, e);
                String commandId = Objects.nonNull(entityDTO) ? entityDTO.commandId() : null;
                Long tenantId = Objects.nonNull(entityDTO) ? entityDTO.tenantId() : null;
                sendResult(commandId, tenantId, PointCommandStatusEnum.FAILED,
                        null, "DRIVER_ERROR", e.getMessage(), channel, deliveryTag);
            } else {
                log.warn("Point command failed, requeueing. deliveryTag={}", deliveryTag, e);
                releaseDedup(entityDTO);
                RabbitAckUtil.nack(channel, deliveryTag, true);
            }
        }
    }

    private boolean isInvalidPayload(PointCommandPayload payload) {
        return switch (payload) {
            case PointCommandPayload.ReadPayload r -> Objects.isNull(r.deviceId()) || Objects.isNull(r.pointId());
            case PointCommandPayload.WritePayload w -> Objects.isNull(w.deviceId()) || Objects.isNull(w.pointId())
                    || Objects.isNull(w.value());
        };
    }

    private void releaseDedup(PointCommandDTO entityDTO) {
        if (Objects.nonNull(entityDTO) && Objects.nonNull(entityDTO.commandId())) {
            dedupCache.release(entityDTO.commandId());
        }
    }

    private void sendResult(String commandId, Long tenantId, PointCommandStatusEnum status,
                            String responseValue, String errorCode, String errorMessage,
                            Channel channel, long deliveryTag) {
        try {
            if (Objects.nonNull(commandId)) {
                PointCommandResultDTO result = PointCommandResultDTO.builder()
                        .commandId(commandId)
                        .tenantId(tenantId)
                        .status(status)
                        .responseValue(responseValue)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .finishedAt(Instant.now())
                        .schemaVersion(SCHEMA_VERSION)
                        .build();
                driverSenderService.pointCommandResultSender(result);
            }
        } catch (Exception e) {
            log.error("Failed to send command result, commandId={}", commandId, e);
        }
        RabbitAckUtil.ack(channel, deliveryTag);
    }

}
