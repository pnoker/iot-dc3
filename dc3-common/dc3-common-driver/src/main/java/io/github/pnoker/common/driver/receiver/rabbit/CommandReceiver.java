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
import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.CommandRuntimeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

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
public class CommandReceiver {

    /**
     * Message schema version stamped on outbound command results.
     */
    private static final int SCHEMA_VERSION = 1;
    private final DriverCustomService driverCustomService;
    private final DriverSenderService driverSenderService;
    private final DeviceMetadata deviceMetadata;
    private final CommandDedupCache dedupCache;
    private final DeviceLockManager deviceLockManager;
    private final DriverMetadata driverMetadata;
    private final DriverProperties driverProperties;
    private final Scheduler commandScheduler;

    public CommandReceiver(DriverCustomService driverCustomService, DriverSenderService driverSenderService,
                           DeviceMetadata deviceMetadata, CommandDedupCache dedupCache,
                           DeviceLockManager deviceLockManager, DriverMetadata driverMetadata,
                           DriverProperties driverProperties, ThreadPoolExecutor threadPoolExecutor) {
        this.driverCustomService = driverCustomService;
        this.driverSenderService = driverSenderService;
        this.deviceMetadata = deviceMetadata;
        this.dedupCache = dedupCache;
        this.deviceLockManager = deviceLockManager;
        this.driverMetadata = driverMetadata;
        this.driverProperties = driverProperties;
        this.commandScheduler = Schedulers.fromExecutor(threadPoolExecutor);
    }

    /**
     * Dispatch a custom command call to the driver: validate the payload, drop
     * expired calls, deduplicate by record id, execute the command under a
     * per-device lock to prevent protocol interleaving, then send the result
     * receipt back to the data center and ack. On failure the call is nacked
     * with requeue unless this is a redelivery, in which case a FAILED result
     * is reported and the message is acked.
     *
     * @param message broker-neutral delivery carrying the command call
     * @param ack     poison-message disposition selector
     */
    @Dc3Listener(topic = MqTopic.COMMAND, group = "${dc3.driver.client}", keyPattern = "${dc3.driver.service}.${dc3.driver.node}")
    public Mono<Void> commandReceive(MqReceived<CommandCallDTO> message, Acknowledgment ack) {
        CommandCallDTO entityDTO = message.payload();
        return Mono.defer(() -> {
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
                return Mono.empty();
            }

            log.debug("Custom command received, recordId={}, deviceId={}, commandId={}",
                    entityDTO.recordId(), entityDTO.deviceId(), entityDTO.commandId());

            String recordId = entityDTO.recordId();
            Long tenantId = entityDTO.tenantId();
            Long deviceId = entityDTO.deviceId();
            Long commandId = entityDTO.commandId();
            String dedupKey = "command:" + recordId;

            return dedupCache.result(dedupKey, CommandCallResultDTO.class)
                    .map(this::publishResult)
                    .orElseGet(() -> processNewCommand(entityDTO, dedupKey, tenantId, deviceId, commandId,
                            message.redelivered()));
        });
    }

    private Mono<Void> processNewCommand(CommandCallDTO entityDTO, String dedupKey, Long tenantId, Long deviceId,
                                         Long commandId, boolean redelivered) {
        String recordId = entityDTO.recordId();

        if (!Objects.equals(driverProperties.getNode(), entityDTO.ownerNode())
                || !Objects.equals(driverMetadata.getFencingToken(deviceId), entityDTO.fencingToken())) {
            log.warn("Reject stale-owner custom command, recordId={}, deviceId={}, fencingToken={}",
                    recordId, deviceId, entityDTO.fencingToken());
            return publishResult(result(recordId, tenantId, PointCommandStatusEnum.FAILED,
                    null, null, "STALE_OWNER", "Device ownership lease changed"));
        }

        if (Objects.nonNull(entityDTO.expireAt()) && Instant.now().isAfter(entityDTO.expireAt())) {
            log.warn("Custom command rejected, reason=expired, recordId={}, expireAt={}",
                    recordId, entityDTO.expireAt());
            return publishResult(result(recordId, tenantId, PointCommandStatusEnum.EXPIRED,
                    null, null, "EXPIRED", "Command expired before execution"));
        }

        if (!dedupCache.tryAcquire(dedupKey)) {
            log.warn("Duplicate command detected: recordId={}", recordId);
            return publishResult(result(recordId, tenantId, PointCommandStatusEnum.DUPLICATE,
                    null, null, "DUPLICATE", "Command already processing"));
        }

        Mono<CommandCallResultDTO> execution = Mono.fromSupplier(() -> deviceLockManager.runExclusive(deviceId, () -> {
                DeviceBO device = deviceMetadata.getCache(deviceId);
                if (Objects.isNull(device)) {
                    throw new IllegalStateException("Device not found in cache: " + deviceId);
                }
                CommandRuntimeBO command = Objects.isNull(device.getCommandRuntimeIdMap())
                        ? null : device.getCommandRuntimeIdMap().get(commandId);
                if (Objects.isNull(command)) {
                    throw new IllegalStateException("Command not found in device metadata: " + commandId);
                }
                Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
                Map<String, AttributeBO> commandConfig = deviceMetadata.getCommandConfig(deviceId, commandId);
                Map<String, String> resultValues = driverCustomService.execute(driverConfig, commandConfig, device, command,
                        Objects.nonNull(entityDTO.paramValues()) ? entityDTO.paramValues() : Collections.emptyMap());
                return new CommandExecutionResult(resultValues, buildConfigSnapshot(commandConfig));
                })).subscribeOn(commandScheduler)
                .map(commandExecution -> result(recordId, tenantId, PointCommandStatusEnum.SUCCESS,
                        commandExecution.resultValues(), commandExecution.configSnapshot(), null, null))
                .onErrorResume(error -> handleExecutionFailure(entityDTO, dedupKey, redelivered, error));

        return execution.flatMap(commandResult -> {
            dedupCache.complete(dedupKey, commandResult);
            return publishResult(commandResult);
        });
    }

    private Mono<CommandCallResultDTO> handleExecutionFailure(CommandCallDTO command, String dedupKey,
                                                               boolean redelivered, Throwable error) {
        if (!redelivered) {
            log.warn("Custom command failed, requeueing, recordId={}", command.recordId(), error);
            dedupCache.release(dedupKey);
            return Mono.error(error);
        }
        log.error("Custom command failed on redelivery, sending FAILED, recordId={}", command.recordId(), error);
        return Mono.just(result(command.recordId(), command.tenantId(), PointCommandStatusEnum.FAILED,
                null, null, "DRIVER_ERROR", error.getMessage()));
    }

    private Mono<Void> publishResult(CommandCallResultDTO result) {
        return driverSenderService.commandResultSender(result)
                .doOnError(error -> log.error("Failed to publish command result, recordId={}",
                        result.recordId(), error));
    }

    private CommandCallResultDTO result(String recordId, Long tenantId, PointCommandStatusEnum status,
                                        Map<String, String> resultValues, String configSnapshot,
                                        String errorCode, String errorMessage) {
        return CommandCallResultDTO.builder()
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
