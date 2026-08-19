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
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * RabbitMQ consumer that dispatches custom command calls to the driver
 * implementation. Performs expire-at pre-check, idempotent deduplication,
 * and sends result receipts back to the data center.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandReceiver {

    /**
     * Message schema version stamped on outbound command results.
     */
    private static final int SCHEMA_VERSION = 1;
    private final DriverCustomService driverCustomService;
    private final DriverSenderService driverSenderService;
    private final CommandFacade commandFacade;
    private final DeviceMetadata deviceMetadata;
    private final CommandDedupCache dedupCache;
    private final DeviceLockManager deviceLockManager;
    private final DriverMetadata driverMetadata;
    private final DriverProperties driverProperties;

    /**
     * Dispatch a custom command call to the driver: validate the payload, drop
     * expired calls, deduplicate by record id, execute the command under a
     * per-device lock to prevent protocol interleaving, then send the result
     * receipt back to the data center and ack. On failure the call is nacked
     * with requeue unless this is a redelivery, in which case a FAILED result
     * is reported and the message is acked.
     *
     * @param channel   the RabbitMQ channel used for ack/nack
     * @param message   the inbound RabbitMQ message carrying the delivery tag and redelivery flag
     * @param entityDTO the deserialized command call payload
     */
    @Dc3Listener(topic = MqTopic.COMMAND, group = "${dc3.driver.client}", keyPattern = "${dc3.driver.service}.${dc3.driver.node}")
    public void commandReceive(MqReceived<CommandCallDTO> message, Acknowledgment ack) {
        CommandCallDTO entityDTO = message.payload();
        boolean redelivered = message.redelivered();
        try {
            // Validate first: the debug log below dereferences entityDTO, so a null
            // payload must be rejected before logging to avoid an NPE that would
            // otherwise fall through to the nack(requeue) path and requeue garbage.
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.recordId())
                    || Objects.isNull(entityDTO.tenantId())
                    || Objects.isNull(entityDTO.ownerNode()) || Objects.isNull(entityDTO.fencingToken())
                    || Objects.isNull(entityDTO.deviceId()) || Objects.isNull(entityDTO.commandId())) {
                log.error("Custom command rejected, reason=invalidEnvelope, recordId={}, tenantId={}, deviceId={}, commandId={}",
                        Objects.isNull(entityDTO) ? null : entityDTO.recordId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.tenantId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.deviceId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.commandId());
                ack.reject(false);
                return;
            }

            log.debug("Custom command received, recordId={}, deviceId={}, commandId={}",
                    entityDTO.recordId(), entityDTO.deviceId(), entityDTO.commandId());

            String recordId = entityDTO.recordId();
            Long tenantId = entityDTO.tenantId();
            Long deviceId = entityDTO.deviceId();
            Long commandId = entityDTO.commandId();

            if (!Objects.equals(driverProperties.getNode(), entityDTO.ownerNode())
                    || !Objects.equals(driverMetadata.getFencingToken(deviceId), entityDTO.fencingToken())) {
                log.warn("Reject stale-owner custom command, recordId={}, deviceId={}, fencingToken={}",
                        recordId, deviceId, entityDTO.fencingToken());
                sendResult(recordId, tenantId, PointCommandStatusEnum.FAILED,
                        null, null, "STALE_OWNER", "Device ownership lease changed", ack);
                return;
            }

            // Expire-at pre-check
            if (Objects.nonNull(entityDTO.expireAt()) && Instant.now().isAfter(entityDTO.expireAt())) {
                log.warn("Custom command rejected, reason=expired, recordId={}, expireAt={}",
                        recordId, entityDTO.expireAt());
                sendResult(recordId, tenantId, PointCommandStatusEnum.EXPIRED,
                        null, null, "EXPIRED", "Command expired before execution", ack);
                return;
            }

            // Dedup check
            if (!dedupCache.tryAcquire(recordId)) {
                log.warn("Duplicate command detected: recordId={}", recordId);
                sendResult(recordId, tenantId, PointCommandStatusEnum.DUPLICATE,
                        null, null, "DUPLICATE", "Command already processed", ack);
                return;
            }

            // Dispatch under per-device lock to prevent protocol interleaving
            CommandExecutionResult executionResult = deviceLockManager.runExclusive(deviceId, () -> {
                DeviceBO device = deviceMetadata.getCache(deviceId);
                if (Objects.isNull(device)) {
                    throw new IllegalStateException("Device not found in cache: " + deviceId);
                }
                FacadeCommandBO command = commandFacade.getById(tenantId, commandId);
                if (Objects.isNull(command)) {
                    throw new IllegalStateException("Command not found: " + commandId);
                }
                Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
                Map<String, AttributeBO> commandConfig = deviceMetadata.getCommandConfig(deviceId, commandId);
                Map<String, String> resultValues = driverCustomService.execute(driverConfig, commandConfig, device, command,
                        Objects.nonNull(entityDTO.paramValues()) ? entityDTO.paramValues() : Collections.emptyMap());
                return new CommandExecutionResult(resultValues, buildConfigSnapshot(commandConfig));
            });

            sendResult(recordId, tenantId, PointCommandStatusEnum.SUCCESS,
                    executionResult.resultValues(), executionResult.configSnapshot(), null, null, ack);

        } catch (Exception e) {
            if (redelivered) {
                log.error("Custom command failed on redelivery, sending FAILED.", e);
                String recordId = Objects.nonNull(entityDTO) ? entityDTO.recordId() : null;
                Long tenantId = Objects.nonNull(entityDTO) ? entityDTO.tenantId() : null;
                sendResult(recordId, tenantId, PointCommandStatusEnum.FAILED,
                        null, null, "DRIVER_ERROR", e.getMessage(), ack);
            } else {
                log.warn("Custom command failed, requeueing.", e);
                releaseDedup(entityDTO);
                ack.reject(true);
            }
        }
    }

    private void releaseDedup(CommandCallDTO entityDTO) {
        if (Objects.nonNull(entityDTO) && Objects.nonNull(entityDTO.recordId())) {
            dedupCache.release(entityDTO.recordId());
        }
    }

    private void sendResult(String recordId, Long tenantId, PointCommandStatusEnum status,
                            Map<String, String> resultValues, String configSnapshot,
                            String errorCode, String errorMessage,
                            Acknowledgment ack) {
        try {
            if (Objects.nonNull(recordId)) {
                CommandCallResultDTO result = CommandCallResultDTO.builder()
                        .recordId(recordId)
                        .tenantId(tenantId)
                        .status(status)
                        .resultValues(resultValues)
                        .configSnapshot(configSnapshot)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .finishedAt(Instant.now())
                        .schemaVersion(SCHEMA_VERSION)
                        .build();
                driverSenderService.commandResultSender(result);
            }
        } catch (Exception e) {
            log.error("Failed to send command result, recordId={}", recordId, e);
            if (Objects.nonNull(recordId)) {
                dedupCache.release(recordId);
            }
            ack.reject(true);
            return;
        }
        ack.ack();
    }

    /**
     * Build a JSON snapshot of the command attribute config used for a call, for audit
     * trail. Returns null when the config is empty.
     *
     * @param commandConfig the command attribute config map
     * @return the JSON snapshot, or null
     */
    private String buildConfigSnapshot(Map<String, AttributeBO> commandConfig) {
        if (Objects.isNull(commandConfig) || commandConfig.isEmpty()) {
            return null;
        }

        Map<String, Map<String, String>> snapshot = new LinkedHashMap<>();
        commandConfig.forEach((attributeCode, attribute) -> {
            Map<String, String> item = new LinkedHashMap<>();
            if (Objects.nonNull(attribute)) {
                item.put("type", Objects.nonNull(attribute.getType()) ? attribute.getType().getCode() : null);
                item.put("configValue", attribute.getValue());
            }
            snapshot.put(attributeCode, item);
        });
        return JsonUtil.toJsonString(snapshot);
    }

    /**
     * Outcome of a command executed under the per-device lock, pairing the
     * raw result values returned by the driver with a JSON snapshot of the
     * command attribute config that produced them, captured for audit.
     *
     * @param resultValues   map of point name to value produced by the driver
     * @param configSnapshot JSON snapshot of the command config in effect at execution time
     */
    private record CommandExecutionResult(Map<String, String> resultValues, String configSnapshot) {
    }

}
