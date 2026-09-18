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
    /** Acquire the driver-level membership lock, failing when held. */
    Mono<Void> acquireDriverLock(Long tenantId, Long driverId);

    /** Renew this node's instance lease for the driver. */
    Mono<Void> renewInstance(Long tenantId, Long driverId, String node, String client, String host, Instant leaseUntil);

    /** List nodes holding a live instance lease for the driver. */
    Flux<String> listActiveNodes(Long tenantId, Long driverId);

    /** Page driver device ids after the cursor position. */
    Flux<Long> listDriverDeviceIds(Long tenantId, Long driverId, long afterDeviceId, int limit);

    /** Load the driver lease state row. */
    Mono<DriverLeaseStateDO> getLeaseState(Long tenantId, Long driverId);

    /** Load the driver device revision counter. */
    Mono<Long> getDeviceRevision(Long tenantId, Long driverId);

    /** Advance the assignment version when the membership hash still matches, returning the new version. */
    Mono<Long> advanceAssignmentVersion(Long tenantId, Long driverId, String membershipHash, long deviceRevision);

    /** Drop instance leases expired before the given instant. */
    Mono<Void> deleteExpiredInstances(Long tenantId, Long driverId, Instant expiredBefore);

    /** Reconcile device leases to the supplied assignments. */
    Mono<Void> reconcileDeviceLeases(List<DeviceLeaseDO> leases);

    /** Drop device leases whose device no longer exists. */
    Mono<Void> deleteOrphanedLeases(Long tenantId, Long driverId);

    /** Page device leases owned by the node after the cursor position. */
    Flux<DeviceLeaseDO> listOwnedLeases(Long tenantId, Long driverId, String node, long afterDeviceId, int limit);

    /** Load the active device lease, or null when none. */
    Mono<DeviceLeaseDO> getActiveLease(Long tenantId, Long deviceId);
}
