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
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.entity.dto.PointCommandPayload;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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
    private final Scheduler commandScheduler;

    public PointCommandReceiver(
            DriverReadService driverReadService,
            DriverWriteService driverWriteService,
            DriverSenderService driverSenderService,
            CommandDedupCache dedupCache,
            DeviceLockManager deviceLockManager,
            DriverMetadata driverMetadata,
            DriverProperties driverProperties,
            ThreadPoolExecutor threadPoolExecutor) {
        this.driverReadService = driverReadService;
        this.driverWriteService = driverWriteService;
        this.driverSenderService = driverSenderService;
        this.dedupCache = dedupCache;
        this.deviceLockManager = deviceLockManager;
        this.driverMetadata = driverMetadata;
        this.driverProperties = driverProperties;
        this.commandScheduler = Schedulers.fromExecutor(threadPoolExecutor);
    }

    /**
     * Handles an incoming point command by validating the payload, rejecting duplicates,
     * dispatching the read or write under a per-device lock, and sending a result receipt
     * before acknowledging the delivery (or nack-ing for retry on transient failure).
     *
     * @param message broker-neutral delivery carrying the point command
     * @param ack     poison-message disposition selector
     */
    @Dc3Listener(
            topic = MqTopic.POINT_COMMAND,
            group = "${dc3.driver.client}",
            keyPattern = "${dc3.driver.service}.${dc3.driver.node}")
    public Mono<Void> pointCommandReceive(MqReceived<PointCommandDTO> message, Acknowledgment ack) {
        PointCommandDTO entityDTO = message.payload();
        return Mono.defer(() -> {
            if (Objects.isNull(entityDTO)
                    || Objects.isNull(entityDTO.commandId())
                    || Objects.isNull(entityDTO.tenantId())
                    || Objects.isNull(entityDTO.ownerNode())
                    || Objects.isNull(entityDTO.fencingToken())
                    || Objects.isNull(entityDTO.type())
                    || Objects.isNull(entityDTO.payload())) {
                log.error(
                        "Point command rejected, reason=invalidEnvelope, commandId={}, tenantId={}, type={}",
                        Objects.isNull(entityDTO) ? null : entityDTO.commandId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.tenantId(),
                        Objects.isNull(entityDTO) ? null : entityDTO.type());
                ack.reject(false);
                return Mono.empty();
            }

            log.debug("Point command received, commandId={}, type={}", entityDTO.commandId(), entityDTO.type());
            if (isInvalidPayload(entityDTO.payload())) {
                log.error(
                        "Point command rejected, reason=invalidPayload, commandId={}, tenantId={}, type={}",
                        entityDTO.commandId(),
                        entityDTO.tenantId(),
                        entityDTO.type());
                ack.reject(false);
                return Mono.empty();
            }

            String commandId = entityDTO.commandId();
            return dedupCache
                    .result(commandId, PointCommandResultDTO.class)
                    .map(this::publishResult)
                    .orElseGet(() -> processNewCommand(entityDTO, message.redelivered()));
        });
    }

    private Mono<Void> processNewCommand(PointCommandDTO command, boolean redelivered) {
        String commandId = command.commandId();
        Long tenantId = command.tenantId();
        Long deviceId = deviceId(command.payload());

        if (Objects.nonNull(command.expireAt()) && Instant.now().isAfter(command.expireAt())) {
            log.warn(
                    "Point command rejected, reason=expired, commandId={}, expireAt={}", commandId, command.expireAt());
            return publishResult(result(
                    commandId,
                    tenantId,
                    PointCommandStatusEnum.EXPIRED,
                    null,
                    "EXPIRED",
                    "Command expired before execution"));
        }

        if (!Objects.equals(driverProperties.getNode(), command.ownerNode())
                || !Objects.equals(driverMetadata.getFencingToken(deviceId), command.fencingToken())) {
            log.warn(
                    "Reject stale-owner point command, commandId={}, deviceId={}, fencingToken={}",
                    commandId,
                    deviceId,
                    command.fencingToken());
            return publishResult(result(
                    commandId,
                    tenantId,
                    PointCommandStatusEnum.FAILED,
                    null,
                    "STALE_OWNER",
                    "Device ownership lease changed"));
        }

        if (!dedupCache.tryAcquire(commandId)) {
            log.warn("Point command rejected, reason=duplicate, commandId={}", commandId);
            return publishResult(result(
                    commandId,
                    tenantId,
                    PointCommandStatusEnum.DUPLICATE,
                    null,
                    "DUPLICATE",
                    "Command already processing"));
        }

        Mono<PointCommandResultDTO> execution = Mono.fromCallable(
                        () -> deviceLockManager.runExclusive(deviceId, () -> execute(command)))
                .subscribeOn(commandScheduler)
                .map(response -> result(
                        commandId,
                        tenantId,
                        response.status(),
                        response.value(),
                        response.errorCode(),
                        response.errorMessage()))
                .onErrorResume(error -> handleExecutionFailure(command, redelivered, error));

        return execution.flatMap(commandResult -> {
            dedupCache.complete(commandId, commandResult);
            return publishResult(commandResult);
        });
    }

    private PointExecutionResult execute(PointCommandDTO command) {
        return switch (command.payload()) {
            case PointCommandPayload.ReadPayload read -> {
                driverReadService.read(read.deviceId(), read.pointId());
                yield new PointExecutionResult(PointCommandStatusEnum.SUCCESS, null, null, null);
            }
            case PointCommandPayload.WritePayload write ->
                driverWriteService.write(write.deviceId(), write.pointId(), write.value())
                        ? new PointExecutionResult(PointCommandStatusEnum.SUCCESS, write.value(), null, null)
                        : new PointExecutionResult(
                                PointCommandStatusEnum.FAILED, null, "WRITE_FAILED", "Device write returned false");
        };
    }

    private Mono<PointCommandResultDTO> handleExecutionFailure(
            PointCommandDTO command, boolean redelivered, Throwable error) {
        if (!redelivered) {
            log.warn("Point command failed, requeueing, commandId={}", command.commandId(), error);
            dedupCache.release(command.commandId());
            return Mono.error(error);
        }
        log.error("Point command failed on redelivery, sending FAILED, commandId={}", command.commandId(), error);
        return Mono.just(result(
                command.commandId(),
                command.tenantId(),
                PointCommandStatusEnum.FAILED,
                null,
                "DRIVER_ERROR",
                error.getMessage()));
    }

    private Long deviceId(PointCommandPayload payload) {
        return switch (payload) {
            case PointCommandPayload.ReadPayload read -> read.deviceId();
            case PointCommandPayload.WritePayload write -> write.deviceId();
        };
    }

    private boolean isInvalidPayload(PointCommandPayload payload) {
        return switch (payload) {
            case PointCommandPayload.ReadPayload r -> Objects.isNull(r.deviceId()) || Objects.isNull(r.pointId());
            case PointCommandPayload.WritePayload w ->
                Objects.isNull(w.deviceId()) || Objects.isNull(w.pointId()) || Objects.isNull(w.value());
        };
    }

    private PointCommandResultDTO result(
            String commandId,
            Long tenantId,
            PointCommandStatusEnum status,
            String responseValue,
            String errorCode,
            String errorMessage) {
        return PointCommandResultDTO.builder()
                .commandId(commandId)
                .tenantId(tenantId)
                .status(status)
                .responseValue(responseValue)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .finishedAt(Instant.now())
                .schemaVersion(SCHEMA_VERSION)
                .build();
    }

    private Mono<Void> publishResult(PointCommandResultDTO result) {
        return driverSenderService
                .pointCommandResultSender(result)
                .doOnError(error ->
                        log.error("Failed to publish point-command result, commandId={}", result.commandId(), error));
    }

    private record PointExecutionResult(
            PointCommandStatusEnum status, String value, String errorCode, String errorMessage) {}
}
