package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.enums.EntityTypeEnum;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.time.Instant;
import java.util.Map;

/** Reactive read port for tenant-scoped entity leases. */
public interface ReactiveEntityStateStore {
    Mono<Map<Long, Byte>> listStateFlags(Long tenantId, EntityTypeEnum type, Collection<Long> entityIds);

    Mono<Long> countOnline(Long tenantId, EntityTypeEnum type);

    Mono<EntityStateLease> upsert(Long id, Long tenantId, EntityTypeEnum type, Long entityId,
                                  Long parentEntityId, byte stateFlag, byte initialLastStateFlag,
                                  Instant heartbeatAt, int timeoutSeconds, byte timeoutSourceFlag,
                                  String stateExt);

    Mono<Boolean> markAlarm(Long tenantId, EntityTypeEnum type, Long entityId, long leaseVersion, Long alarmId);

    Mono<EntityStateLease> claimExpired(Long tenantId, EntityTypeEnum type, Long entityId,
                                        long expectedLeaseVersion, int renewSeconds);

    Flux<EntityStateLease> claimExpired(EntityTypeEnum type, int limit, int renewSeconds);

    record EntityStateLease(Long id, Long tenantId, EntityTypeEnum type, Long entityId, Long parentEntityId,
                            byte stateFlag, byte lastStateFlag, long leaseVersion, Instant expireTime,
                            int timeoutSeconds, Instant lastHeartbeatTime, Long lastAlarmId, byte timeoutSourceFlag,
                            String stateExt) {
    }
}
