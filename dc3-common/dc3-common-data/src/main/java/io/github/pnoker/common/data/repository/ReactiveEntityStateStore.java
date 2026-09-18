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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.enums.EntityTypeEnum;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive read port for tenant-scoped entity leases. */
public interface ReactiveEntityStateStore {
    /** Emit the current state flags for the entity ids. */
    Mono<Map<Long, Byte>> listStateFlags(Long tenantId, EntityTypeEnum type, Collection<Long> entityIds);

    /** Count online entities of the type within the tenant. */
    Mono<Long> countOnline(Long tenantId, EntityTypeEnum type);

    /** Upsert the row and emit the stored state. */
    Mono<EntityStateLease> upsert(
            Long id,
            Long tenantId,
            EntityTypeEnum type,
            Long entityId,
            Long parentEntityId,
            byte stateFlag,
            byte initialLastStateFlag,
            Instant heartbeatAt,
            int timeoutSeconds,
            byte timeoutSourceFlag,
            String stateExt);

    /** Attach the alarm to the entity lease when the version still matches. */
    Mono<Boolean> markAlarm(Long tenantId, EntityTypeEnum type, Long entityId, long leaseVersion, Long alarmId);

    /** Claim expired entity leases for renewal under the lease fence. */
    Mono<EntityStateLease> claimExpired(
            Long tenantId, EntityTypeEnum type, Long entityId, long expectedLeaseVersion, int renewSeconds);

    /** Claim expired entity leases for renewal under the lease fence. */
    Flux<EntityStateLease> claimExpired(EntityTypeEnum type, int limit, int renewSeconds);

    /** Snapshot of one entity lease. */
    record EntityStateLease(
            Long id,
            Long tenantId,
            EntityTypeEnum type,
            Long entityId,
            Long parentEntityId,
            byte stateFlag,
            byte lastStateFlag,
            long leaseVersion,
            Instant expireTime,
            int timeoutSeconds,
            Instant lastHeartbeatTime,
            Long lastAlarmId,
            byte timeoutSourceFlag,
            String stateExt) {}
}
