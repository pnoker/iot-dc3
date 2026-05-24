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
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandReceiver {

    private static final int SCHEMA_VERSION = 1;
    private final DriverCustomService driverCustomService;
    private final DriverSenderService driverSenderService;
    private final CommandFacade commandFacade;
    private final DeviceMetadata deviceMetadata;
    private final CommandDedupCache dedupCache;
    private final DeviceLockManager deviceLockManager;

    @RabbitHandler
    @RabbitListener(queues = "#{commandQueue.name}")
    public void commandReceive(Channel channel, Message message, CommandCallDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean redelivered = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        try {
            log.info("Receive custom command: {}", JsonUtil.toJsonString(entityDTO));

            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.recordId())
                    || Objects.isNull(entityDTO.deviceId()) || Objects.isNull(entityDTO.commandId())) {
                log.error("Invalid custom command: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            String recordId = entityDTO.recordId();
            Long tenantId = entityDTO.tenantId();
            Long deviceId = entityDTO.deviceId();
            Long commandId = entityDTO.commandId();

            // Expire-at pre-check
            if (Objects.nonNull(entityDTO.expireAt()) && Instant.now().isAfter(entityDTO.expireAt())) {
                log.warn("Command already expired: recordId={}, expireAt={}", recordId, entityDTO.expireAt());
                sendResult(recordId, tenantId, PointCommandStatusEnum.EXPIRED.getCode(),
                        null, null, "EXPIRED", "Command expired before execution", channel, deliveryTag);
                return;
            }

            // Dedup check
            if (!dedupCache.tryAcquire(recordId)) {
                log.warn("Duplicate command detected: recordId={}", recordId);
                sendResult(recordId, tenantId, PointCommandStatusEnum.DUPLICATE.getCode(),
                        null, null, "DUPLICATE", "Command already processed", channel, deliveryTag);
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

            sendResult(recordId, tenantId, PointCommandStatusEnum.SUCCESS.getCode(),
                    executionResult.resultValues(), executionResult.configSnapshot(), null, null, channel, deliveryTag);

        } catch (Exception e) {
            if (redelivered) {
                log.error("Custom command failed on redelivery, sending FAILED. deliveryTag={}", deliveryTag, e);
                String recordId = Objects.nonNull(entityDTO) ? entityDTO.recordId() : null;
                Long tenantId = Objects.nonNull(entityDTO) ? entityDTO.tenantId() : null;
                sendResult(recordId, tenantId, PointCommandStatusEnum.FAILED.getCode(),
                        null, null, "DRIVER_ERROR", e.getMessage(), channel, deliveryTag);
            } else {
                log.warn("Custom command failed, requeueing. deliveryTag={}", deliveryTag, e);
                RabbitAckUtil.nack(channel, deliveryTag, true);
            }
        }
    }

    private void sendResult(String recordId, Long tenantId, String status,
                            Map<String, String> resultValues, String configSnapshot,
                            String errorCode, String errorMessage,
                            Channel channel, long deliveryTag) {
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
        } catch (Exception ex) {
            log.error("Failed to send command result, recordId={}", recordId, ex);
        }
        RabbitAckUtil.ack(channel, deliveryTag);
    }

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

    private record CommandExecutionResult(Map<String, String> resultValues, String configSnapshot) {
    }

}
