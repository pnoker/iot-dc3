package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.operation.DeviceImportJob;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface ReactiveDeviceImportJobStore {

    Mono<Void> insert(DeviceImportJob job);

    Mono<DeviceImportJob> claim(UUID operationId, String workerId, Instant now, Instant claimedUntil);

    Mono<Boolean> renew(UUID operationId, String workerId, Instant claimedUntil);

    Flux<UUID> listRecoverable(Instant now);

    Mono<Void> delete(UUID operationId, Long tenantId);
}
