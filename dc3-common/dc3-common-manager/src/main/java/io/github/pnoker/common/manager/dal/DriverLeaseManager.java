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

public interface DriverLeaseManager {
    void acquireDriverLock(Long driverId);

    void renewInstance(Long tenantId, Long driverId, String node, String client,
                       String host, Instant leaseUntil);

    List<String> listActiveNodes(Long tenantId, Long driverId);

    List<Long> listDriverDeviceIds(Long tenantId, Long driverId, Long afterDeviceId, int limit);

    DriverLeaseStateDO getLeaseState(Long tenantId, Long driverId);

    long getDeviceRevision(Long tenantId, Long driverId);

    long advanceAssignmentVersion(Long tenantId, Long driverId, String membershipHash, long deviceRevision);

    void deleteExpiredInstances(Long tenantId, Long driverId, Instant expiredBefore);

    void reconcileDeviceLeases(List<DeviceLeaseDO> leases);

    void deleteOrphanedLeases(Long tenantId, Long driverId);

    List<DeviceLeaseDO> listOwnedLeases(Long tenantId, Long driverId, String node,
                                        Long afterDeviceId, int limit);

    DeviceLeaseDO getActiveLease(Long tenantId, Long deviceId);
}
