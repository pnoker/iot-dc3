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

import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.entity.dto.PointCommandPayload;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * RabbitMQ consumer that dispatches point read and write commands to the corresponding
 * services. Performs expire-at pre-check, idempotent deduplication, and sends result
 * receipts back to the data center.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandReceiver {

    /**
     * Message schema version stamped on outgoing command results.
     */
    private static final int SCHEMA_VERSION = 1;
    private final DriverReadService driverReadService;
    private final DriverWriteService driverWriteService;
    private final DriverSenderService driverSenderService;
    private final CommandDedupCache dedupCache;
    private final DeviceLockManager deviceLockManager;
    private final DriverMetadata driverMetadata;
    private final DriverProperties driverProperties;

    /**
     * Handles an incoming point command by validating the payload, rejecting duplicates,
     * dispatching the read or write under a per-device lock, and sending a result receipt
     * before acknowledging the delivery (or nack-ing for retry on transient failure).
     *
     * @param channel   RabbitMQ channel used to ack, nack, or reject the delivery
     * @param message   Spring AMQP message carrying the delivery tag and redelivery flag
     * @param entityDTO deserialized point command payload (type, command id, tenant, value)
     */
    @Dc3Listener(topic = MqTopic.POINT_COMMAND, group = "${dc3.driver.client}", keyPattern = "${dc3.driver.service}.${dc3.driver.node}")
    public void pointCommandReceive(MqReceived<PointCommandDTO> message, Acknowledgment ack) {
        PointCommandDTO entityDTO = message.payload();
        boolean redelivered = message.redelivered();
        try {
            // Validate first: the debug log below dereferences entityDTO, so a null
            // payload must be rejected before logging to avoid an NPE that would
            // otherwise fall through to the nack(requeue) path and requeue garbage.
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.commandId())
                    || Objects.isNull(entityDTO.tenantId()) || Objects.isNull(entityDTO.ownerNode())
                    || Objects.isNull(entityDTO.fencingToken()) || Objects.isNull(entityDTO.type())
                    || Objects.isNull(entityDTO.payload())) {
                log.error("Point command rejected, reason=invalidEnvelope, commandId={}, tenantId={}, type={}",
                        Objects.isNull(entityDTO) ? null : entityDTO.commandId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.tenantId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.type());
                ack.reject(false);
                return;
            }

            log.debug("Point command received, commandId={}, type={}", entityDTO.commandId(), entityDTO.type());
            if (isInvalidPayload(entityDTO.payload())) {
                log.error("Point command rejected, reason=invalidPayload, commandId={}, tenantId={}, type={}",
                        entityDTO.commandId(), entityDTO.tenantId(), entityDTO.type());
                ack.reject(false);
                return;
            }

            String commandId = entityDTO.commandId();
            Long tenantId = entityDTO.tenantId();

            // Expire-at pre-check
            if (Objects.nonNull(entityDTO.expireAt()) && Instant.now().isAfter(entityDTO.expireAt())) {
                log.warn("Point command rejected, reason=expired, commandId={}, expireAt={}",
                        commandId, entityDTO.expireAt());
                sendResult(commandId, tenantId, PointCommandStatusEnum.EXPIRED,
                        null, "EXPIRED", "Command expired before execution", ack);
                return;
            }

            // Dedup check
            if (!dedupCache.tryAcquire(commandId)) {
                log.warn("Point command rejected, reason=duplicate, commandId={}", commandId);
                sendResult(commandId, tenantId, PointCommandStatusEnum.DUPLICATE,
                        null, "DUPLICATE", "Command already processed", ack);
                return;
            }

            // Extract deviceId for per-device serialization
            Long lockDeviceId = switch (entityDTO.payload()) {
                case PointCommandPayload.ReadPayload r -> r.deviceId();
                case PointCommandPayload.WritePayload w -> w.deviceId();
            };

            if (!Objects.equals(driverProperties.getNode(), entityDTO.ownerNode())
                    || !Objects.equals(driverMetadata.getFencingToken(lockDeviceId), entityDTO.fencingToken())) {
                log.warn("Reject stale-owner point command, commandId={}, deviceId={}, fencingToken={}",
                        commandId, lockDeviceId, entityDTO.fencingToken());
                sendResult(commandId, tenantId, PointCommandStatusEnum.FAILED,
                        null, "STALE_OWNER", "Device ownership lease changed", ack);
                return;
            }

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
                        null, "WRITE_FAILED", "Device write returned false", ack);
                return;
            }

            sendResult(commandId, tenantId, PointCommandStatusEnum.SUCCESS,
                    responseValue, null, null, ack);

        } catch (Exception e) {
            if (redelivered) {
                log.error("Point command failed on redelivery, sending FAILED.", e);
                String commandId = Objects.nonNull(entityDTO) ? entityDTO.commandId() : null;
                Long tenantId = Objects.nonNull(entityDTO) ? entityDTO.tenantId() : null;
                sendResult(commandId, tenantId, PointCommandStatusEnum.FAILED,
                        null, "DRIVER_ERROR", e.getMessage(), ack);
            } else {
                log.warn("Point command failed, requeueing.", e);
                releaseDedup(entityDTO);
                ack.reject(true);
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
                            Acknowledgment ack) {
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
            if (Objects.nonNull(commandId)) {
                dedupCache.release(commandId);
            }
            ack.reject(true);
            return;
        }
        ack.ack();
    }

}
