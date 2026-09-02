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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Non-blocking persistence boundary for driver membership and fenced assignments. */
public interface ReactiveDriverLeaseStore {
    Mono<Void> acquireDriverLock(Long tenantId, Long driverId);

    Mono<Void> renewInstance(Long tenantId, Long driverId, String node, String client, String host, Instant leaseUntil);

    Flux<String> listActiveNodes(Long tenantId, Long driverId);

    Flux<Long> listDriverDeviceIds(Long tenantId, Long driverId, long afterDeviceId, int limit);

    Mono<DriverLeaseStateDO> getLeaseState(Long tenantId, Long driverId);

    Mono<Long> getDeviceRevision(Long tenantId, Long driverId);

    Mono<Long> advanceAssignmentVersion(Long tenantId, Long driverId, String membershipHash, long deviceRevision);

    Mono<Void> deleteExpiredInstances(Long tenantId, Long driverId, Instant expiredBefore);

    Mono<Void> reconcileDeviceLeases(List<DeviceLeaseDO> leases);

    Mono<Void> deleteOrphanedLeases(Long tenantId, Long driverId);

    Flux<DeviceLeaseDO> listOwnedLeases(Long tenantId, Long driverId, String node, long afterDeviceId, int limit);

    Mono<DeviceLeaseDO> getActiveLease(Long tenantId, Long deviceId);
}
