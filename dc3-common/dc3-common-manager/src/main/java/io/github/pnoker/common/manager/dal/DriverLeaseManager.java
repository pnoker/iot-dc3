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

package io.github.pnoker.common.manager.dal;

import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;

import java.time.Instant;
import java.util.List;

/**
 * Persistence boundary for driver-runtime membership, assignment generations, and device fencing tokens.
 * <p>
 * The mapper intentionally bypasses automatic tenant interception because the driver lock is global to a logical
 * driver ID. Every tenant-owned query therefore carries an explicit tenant ID and must preserve that predicate.
 *
 * @author pnoker
 * @since 2026.5.22
 */
public interface DriverLeaseManager {

    /**
     * Serializes membership and assignment changes for one logical driver.
     */
    void acquireDriverLock(Long driverId);

    /**
     * Creates or renews one tenant-owned runtime instance through the supplied expiry instant.
     */
    void renewInstance(Long tenantId, Long driverId, String node, String client,
                       String host, Instant leaseUntil);

    /**
     * Returns the stable, sorted identifiers of all unexpired runtime nodes for one logical driver.
     */
    List<String> listActiveNodes(Long tenantId, Long driverId);

    /**
     * Lists tenant-owned device IDs using an exclusive cursor and deterministic ascending order.
     */
    List<Long> listDriverDeviceIds(Long tenantId, Long driverId, Long afterDeviceId, int limit);

    /**
     * Returns the current assignment state, or {@code null} before the first reconciliation.
     */
    DriverLeaseStateDO getLeaseState(Long tenantId, Long driverId);

    /**
     * Returns the current device-inventory revision, or zero when no revision exists.
     */
    long getDeviceRevision(Long tenantId, Long driverId);

    /**
     * Persists membership state and atomically advances the assignment fencing generation.
     */
    long advanceAssignmentVersion(Long tenantId, Long driverId, String membershipHash, long deviceRevision);

    /**
     * Removes runtime-instance records whose leases expired before the retention cutoff.
     */
    void deleteExpiredInstances(Long tenantId, Long driverId, Instant expiredBefore);

    /**
     * Upserts the complete calculated assignment set in bounded batches.
     */
    void reconcileDeviceLeases(List<DeviceLeaseDO> leases);

    /**
     * Removes assignments for devices or owners that no longer belong to the active driver membership.
     */
    void deleteOrphanedLeases(Long tenantId, Long driverId);

    /**
     * Lists unexpired assignments owned by one runtime node using an exclusive device-ID cursor.
     */
    List<DeviceLeaseDO> listOwnedLeases(Long tenantId, Long driverId, String node,
                                        Long afterDeviceId, int limit);

    /**
     * Returns the unexpired assignment for one tenant-owned device, or {@code null} when none exists.
     */
    DeviceLeaseDO getActiveLease(Long tenantId, Long deviceId);
}
