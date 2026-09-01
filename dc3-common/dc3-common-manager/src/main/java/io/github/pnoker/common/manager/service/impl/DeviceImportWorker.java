package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.operation.DeviceImportJob;
import io.github.pnoker.common.manager.entity.operation.DeviceImportManifest;
import io.github.pnoker.common.manager.entity.operation.DeviceImportRow;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveDeviceImportJobStore;
import io.github.pnoker.common.manager.repository.ReactiveDeviceStore;
import io.github.pnoker.common.manager.repository.ReactiveDriverAttributeConfigStore;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeConfigStore;
import io.github.pnoker.common.manager.service.DeviceImportSchemaService;
import io.github.pnoker.common.manager.service.DeviceImportWorkbookCodec;
import io.github.pnoker.common.manager.support.ManagerFileScheduler;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.operation.OperationRepository;
import io.github.pnoker.db.r2dbc.core.operation.OperationState;
import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceImportWorker {

    private static final Duration LEASE = Duration.ofHours(1);
    private static final Duration RECOVERY_SCAN_INTERVAL = Duration.ofMinutes(1);

    private final ReactiveDeviceImportJobStore jobStore;
    private final OperationRepository operationRepository;
    private final DeviceImportSchemaService schemaService;
    private final DeviceImportWorkbookCodec workbookCodec;
    private final ManagerFileScheduler fileScheduler;
    private final ReactiveDeviceStore deviceStore;
    private final ReactiveDriverAttributeConfigStore driverConfigStore;
    private final ReactivePointAttributeConfigStore pointConfigStore;
    private final MetadataEventPublisher metadataEventPublisher;
    private final TransactionalOperator transactionalOperator;
    private final ObjectMapper objectMapper;

    private final Sinks.Many<UUID> queue = Sinks.many().unicast().onBackpressureBuffer();
    private final String workerId = UUID.randomUUID().toString();
    private volatile Disposable subscription;
    private volatile Disposable recoverySubscription;

    @PostConstruct
    void start() {
        subscription = queue.asFlux().concatMap(this::processSafely).subscribe(
                ignored -> { }, error -> log.error("Device import worker stopped unexpectedly", error));
        recoverySubscription = Flux.interval(Duration.ZERO, RECOVERY_SCAN_INTERVAL)
                .concatMap(ignored -> jobStore.listRecoverable(Instant.now()).doOnNext(this::enqueue).then()
                        .onErrorResume(error -> {
                            log.error("Device import recovery scan failed", error);
                            return Mono.empty();
                        }))
                .subscribe();
    }

    @PreDestroy
    void stop() {
        if (subscription != null) subscription.dispose();
        if (recoverySubscription != null) recoverySubscription.dispose();
    }

    void enqueue(UUID operationId) {
        Sinks.EmitResult result = queue.tryEmitNext(operationId);
        if (result.isFailure()) log.warn("Device import operation could not be queued, operationId={}, result={}",
                operationId, result);
    }

    Mono<Void> processSafely(UUID operationId) {
        Instant now = Instant.now();
        return jobStore.claim(operationId, workerId, now, now.plus(LEASE))
                .flatMap(job -> process(job).onErrorResume(error -> fail(job, error)))
                .onErrorResume(error -> {
                    log.error("Device import job claim failed, operationId={}", operationId, error);
                    return Mono.empty();
                });
    }

    private Mono<Void> process(DeviceImportJob job) {
        TenantScope scope = new TenantScope(job.tenantId());
        return operationRepository.findById(scope, job.operationId())
                .flatMap(state -> {
                    if (state.status() == OperationState.Status.PENDING) {
                        OperationState running = state.transition(OperationState.Status.RUNNING, 5, Instant.now());
                        return operationRepository.transition(scope, job.operationId(), state.status(), running);
                    }
                    if (state.status() == OperationState.Status.RUNNING) return Mono.just(state);
                    return jobStore.delete(job.operationId(), job.tenantId()).then(Mono.empty());
                })
                .flatMap(running -> schemaService.load(job.tenantId(), job.driverId(), job.profileId())
                        .flatMap(manifest -> fileScheduler.call(() -> workbookCodec.parse(job.content(), manifest))
                                .map(rows -> new ParsedImport(manifest, rows))))
                .flatMap(parsed -> commit(job, parsed));
    }

    private Mono<Void> commit(DeviceImportJob job, ParsedImport parsed) {
        TenantScope scope = new TenantScope(job.tenantId());
        Mono<List<DeviceBO>> write = Flux.fromIterable(parsed.rows())
                .concatMap(row -> insertRow(job, parsed.manifest(), row))
                .collectList()
                .flatMap(devices -> operationRepository.findById(scope, job.operationId())
                        .switchIfEmpty(Mono.error(new IllegalStateException("Device import operation does not exist")))
                        .flatMap(current -> {
                            if (current.status() != OperationState.Status.RUNNING) {
                                return Mono.error(new IllegalStateException("Device import operation is not running"));
                            }
                            String result = json(Map.of("imported", devices.size(), "deviceIds",
                                    devices.stream().map(DeviceBO::getId).toList()));
                            OperationState succeeded = current.transition(OperationState.Status.SUCCEEDED, 100,
                                    result, null, Instant.now());
                            return operationRepository.transition(scope, job.operationId(), current.status(), succeeded)
                                    .then(jobStore.delete(job.operationId(), job.tenantId()))
                                    .thenReturn(devices);
                        }));
        return transactionalOperator.transactional(write)
                .doOnSuccess(devices -> devices.forEach(this::publishMetadata))
                .then();
    }

    private void publishMetadata(DeviceBO device) {
        try {
            metadataEventPublisher.publishEvent(new MetadataEvent(this, device.getId(), MetadataTypeEnum.DEVICE,
                    MetadataOperateTypeEnum.ADD));
        } catch (RuntimeException error) {
            log.error("Device import committed but metadata notification failed, deviceId={}", device.getId(), error);
        }
    }

    private Mono<DeviceBO> insertRow(DeviceImportJob job, DeviceImportManifest manifest, DeviceImportRow row) {
        DeviceBO device = new DeviceBO();
        device.setDeviceName(row.deviceName());
        device.setDeviceCode(CodeUtil.getCode());
        device.setDriverId(job.driverId());
        device.setProfileId(job.profileId());
        device.setDeviceExt(new io.github.pnoker.common.entity.ext.DeviceExt());
        device.setTenantId(job.tenantId());
        device.setRemark(row.remark());
        device.setCreatorId(job.operatorId());
        device.setCreatorName(job.operatorName());
        device.setOperatorId(job.operatorId());
        device.setOperatorName(job.operatorName());
        return deviceStore.insert(device)
                .flatMap(saved -> insertConfigs(job, manifest, row, saved).thenReturn(saved));
    }

    private Mono<Void> insertConfigs(DeviceImportJob job, DeviceImportManifest manifest, DeviceImportRow row,
                                     DeviceBO device) {
        Flux<DriverAttributeConfigBO> driverConfigs = Flux.range(0, manifest.driverAttributes().size())
                .map(index -> {
                    DriverAttributeConfigBO config = new DriverAttributeConfigBO();
                    config.setAttributeId(manifest.driverAttributes().get(index).id());
                    config.setConfigValue(row.driverAttributeValues().get(index));
                    config.setDeviceId(device.getId());
                    config.setConfigExt(new JsonExt());
                    config.setTenantId(job.tenantId());
                    audit(config, job, row.remark());
                    return config;
                });
        List<PointConfigValue> pointValues = new java.util.ArrayList<>();
        int valueIndex = 0;
        for (DeviceImportManifest.PointColumn point : manifest.points()) {
            for (DeviceImportManifest.AttributeColumn attribute : point.attributes()) {
                pointValues.add(new PointConfigValue(point.id(), attribute.id(),
                        row.pointAttributeValues().get(valueIndex++)));
            }
        }
        Flux<PointAttributeConfigBO> pointConfigs = Flux.fromIterable(pointValues).map(value -> {
            PointAttributeConfigBO config = new PointAttributeConfigBO();
            config.setAttributeId(value.attributeId());
            config.setConfigValue(value.value());
            config.setDeviceId(device.getId());
            config.setPointId(value.pointId());
            config.setConfigExt(new JsonExt());
            config.setTenantId(job.tenantId());
            audit(config, job, row.remark());
            return config;
        });
        return driverConfigs.concatMap(driverConfigStore::insert).then()
                .thenMany(pointConfigs.concatMap(pointConfigStore::insert)).then();
    }

    private void audit(io.github.pnoker.common.entity.base.BaseBO config, DeviceImportJob job, String remark) {
        config.setRemark(remark);
        config.setCreatorId(job.operatorId());
        config.setCreatorName(job.operatorName());
        config.setOperatorId(job.operatorId());
        config.setOperatorName(job.operatorName());
    }

    private Mono<Void> fail(DeviceImportJob job, Throwable error) {
        UUID operationId = job.operationId();
        return operationRepository.findById(new TenantScope(job.tenantId()), operationId)
                        .flatMap(current -> {
                            if (current.status() != OperationState.Status.PENDING
                                    && current.status() != OperationState.Status.RUNNING) return Mono.empty();
                            OperationState failed = current.transition(OperationState.Status.FAILED,
                                    current.progress(), null, json(Map.of(
                                            "type", "about:blank",
                                            "title", "Device import failed",
                                            "status", 422,
                                            "code", "DEVICE_IMPORT_FAILED",
                                            "detail", safeMessage(error))), Instant.now());
                            return operationRepository.transition(new TenantScope(job.tenantId()), operationId,
                                            current.status(), failed)
                                    .then(jobStore.delete(operationId, job.tenantId()));
                        }).as(transactionalOperator::transactional)
                .doOnSuccess(ignored -> log.warn("Device import failed, operationId={}", operationId, error))
                .then();
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("Operation result cannot be serialized", error);
        }
    }

    private record ParsedImport(DeviceImportManifest manifest, List<DeviceImportRow> rows) {
    }

    private record PointConfigValue(Long pointId, Long attributeId, String value) {
    }
}
