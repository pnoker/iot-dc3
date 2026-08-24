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

package io.github.pnoker.common.manager.dal.impl;

import io.github.pnoker.common.manager.dal.DriverLeaseManager;
import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import io.github.pnoker.common.manager.mapper.DriverLeaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.List;

/**
 * MyBatis-backed {@link DriverLeaseManager} implementation with bounded assignment writes.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Service
@RequiredArgsConstructor
public class DriverLeaseManagerImpl implements DriverLeaseManager {

    private static final int WRITE_BATCH_SIZE = 1000;

    private final DriverLeaseMapper driverLeaseMapper;

    @Override
    public void acquireDriverLock(Long driverId) {
        driverLeaseMapper.acquireDriverLock(driverId);
    }

    @Override
    public void renewInstance(Long tenantId, Long driverId, String node, String client,
                              String host, Instant leaseUntil) {
        driverLeaseMapper.upsertInstance(tenantId, driverId, node, client, host, leaseUntil);
    }

    @Override
    public List<String> listActiveNodes(Long tenantId, Long driverId) {
        return driverLeaseMapper.listActiveNodes(tenantId, driverId);
    }

    @Override
    public List<Long> listDriverDeviceIds(Long tenantId, Long driverId, Long afterDeviceId, int limit) {
        return driverLeaseMapper.listDriverDeviceIds(tenantId, driverId, afterDeviceId, limit);
    }

    @Override
    public DriverLeaseStateDO getLeaseState(Long tenantId, Long driverId) {
        return driverLeaseMapper.selectLeaseState(tenantId, driverId);
    }

    @Override
    public long getDeviceRevision(Long tenantId, Long driverId) {
        Long revision = driverLeaseMapper.selectDeviceRevision(tenantId, driverId);
        return revision == null ? 0L : revision;
    }

    @Override
    public long advanceAssignmentVersion(Long tenantId, Long driverId, String membershipHash, long deviceRevision) {
        // MySQL has no INSERT ... RETURNING; the portable shape re-selects the
        // version in the same transaction.
        driverLeaseMapper.upsertLeaseState(tenantId, driverId, membershipHash, deviceRevision);
        DriverLeaseStateDO state = driverLeaseMapper.selectLeaseState(tenantId, driverId);
        return Objects.isNull(state) ? 0L : state.getAssignmentVersion();
    }

    @Override
    public void deleteExpiredInstances(Long tenantId, Long driverId, Instant expiredBefore) {
        driverLeaseMapper.deleteExpiredInstances(tenantId, driverId, expiredBefore);
    }

    @Override
    public void reconcileDeviceLeases(List<DeviceLeaseDO> leases) {
        for (int from = 0; from < leases.size(); from += WRITE_BATCH_SIZE) {
            int to = Math.min(from + WRITE_BATCH_SIZE, leases.size());
            driverLeaseMapper.upsertDeviceLeases(leases.subList(from, to));
        }
    }

    @Override
    public void deleteOrphanedLeases(Long tenantId, Long driverId) {
        driverLeaseMapper.deleteOrphanedLeases(tenantId, driverId);
    }

    @Override
    public List<DeviceLeaseDO> listOwnedLeases(Long tenantId, Long driverId, String node,
                                               Long afterDeviceId, int limit) {
        return driverLeaseMapper.listOwnedLeases(tenantId, driverId, node, afterDeviceId, limit);
    }

    @Override
    public DeviceLeaseDO getActiveLease(Long tenantId, Long deviceId) {
        return driverLeaseMapper.selectActiveLease(tenantId, deviceId);
    }
}
