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

package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.manager.biz.DriverLeaseService;
import io.github.pnoker.common.manager.dal.DriverLeaseManager;
import io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO;
import io.github.pnoker.common.manager.entity.bo.DriverLeaseGrantBO;
import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Serializes driver membership changes, assigns devices with rendezvous hashing, and advances fencing generations when
 * membership or inventory changes.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Service
@RequiredArgsConstructor
public class DriverLeaseServiceImpl implements DriverLeaseService {

    private static final int MIN_LEASE_SECONDS = 10;
    private static final int MAX_LEASE_SECONDS = 120;
    private static final long EXPIRED_INSTANCE_RETENTION_SECONDS = 86_400;
    private static final int RECONCILE_PAGE_SIZE = 5_000;
    private static final int MAX_ASSIGNMENT_PAGE_SIZE = 2_000;

    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    });

    private final DriverLeaseManager driverLeaseManager;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public DriverLeaseGrantBO renew(Long tenantId, Long driverId, String node, String client,
                                    String host, int leaseSeconds, long knownAssignmentVersion) {
        validate(tenantId, driverId, node, client, host, leaseSeconds);
        Instant leaseUntil = Instant.now().plusSeconds(leaseSeconds);

        // Serialize membership and assignment changes per logical driver. This makes
        // fencing-token increments deterministic even when every replica heartbeats at once.
        driverLeaseManager.acquireDriverLock(driverId);
        driverLeaseManager.renewInstance(tenantId, driverId, node, client, host, leaseUntil);
        driverLeaseManager.deleteExpiredInstances(tenantId, driverId,
                Instant.now().minusSeconds(EXPIRED_INSTANCE_RETENTION_SECONDS));

        List<String> activeNodes = driverLeaseManager.listActiveNodes(tenantId, driverId);
        if (activeNodes.isEmpty()) {
            throw new ServiceException("No active driver instance after lease renewal");
        }

        String membershipHash = membershipHash(activeNodes);
        long deviceRevision = driverLeaseManager.getDeviceRevision(tenantId, driverId);
        DriverLeaseStateDO state = driverLeaseManager.getLeaseState(tenantId, driverId);
        boolean reconcile = state == null || !Objects.equals(state.getMembershipHash(), membershipHash)
                || !Objects.equals(state.getDeviceRevision(), deviceRevision);
        long assignmentVersion;
        if (reconcile) {
            reconcileDeviceLeases(tenantId, driverId, activeNodes);
            driverLeaseManager.deleteOrphanedLeases(tenantId, driverId);
            assignmentVersion = driverLeaseManager.advanceAssignmentVersion(
                    tenantId, driverId, membershipHash, deviceRevision);
        } else {
            assignmentVersion = state.getAssignmentVersion();
        }

        boolean assignmentsChanged = knownAssignmentVersion != assignmentVersion;
        return new DriverLeaseGrantBO(leaseUntil.toEpochMilli(), assignmentVersion, assignmentsChanged);
    }

    @Override
    public List<DeviceLeaseBO> listOwnedLeases(Long tenantId, Long driverId, String node,
                                               long afterDeviceId, int limit) {
        if (tenantId == null || tenantId <= 0 || driverId == null || driverId <= 0
                || node == null || node.isBlank() || afterDeviceId < 0
                || limit < 1 || limit > MAX_ASSIGNMENT_PAGE_SIZE) {
            throw new ServiceException("Invalid driver assignment page request");
        }
        return driverLeaseManager.listOwnedLeases(
                        tenantId, driverId, node, afterDeviceId, limit).stream()
                .map(value -> new DeviceLeaseBO(value.getDriverId(), value.getDeviceId(), value.getOwnerNode(),
                        value.getFencingToken()))
                .toList();
    }

    @Override
    public long getAssignmentVersion(Long tenantId, Long driverId) {
        if (tenantId == null || tenantId <= 0 || driverId == null || driverId <= 0) {
            throw new ServiceException("Invalid driver assignment identity");
        }
        DriverLeaseStateDO state = driverLeaseManager.getLeaseState(tenantId, driverId);
        if (state == null || state.getAssignmentVersion() == null) {
            throw new ServiceException("Driver assignment state does not exist");
        }
        return state.getAssignmentVersion();
    }

    @Override
    public DeviceLeaseBO getActiveOwner(Long tenantId, Long deviceId) {
        if (tenantId == null || tenantId <= 0 || deviceId == null || deviceId <= 0) {
            return null;
        }
        DeviceLeaseDO lease = driverLeaseManager.getActiveLease(tenantId, deviceId);
        return lease == null ? null
                : new DeviceLeaseBO(lease.getDriverId(), lease.getDeviceId(), lease.getOwnerNode(),
                lease.getFencingToken());
    }

    private String selectOwner(Long deviceId, List<String> activeNodes) {
        String selected = null;
        byte[] highest = null;
        MessageDigest digest = SHA_256.get();
        for (String node : activeNodes) {
            digest.reset();
            byte[] score = digest.digest((deviceId + "|" + node).getBytes(StandardCharsets.UTF_8));
            if (highest == null || compareUnsigned(score, highest) > 0) {
                highest = score;
                selected = node;
            }
        }
        return selected;
    }

    private void reconcileDeviceLeases(Long tenantId, Long driverId, List<String> activeNodes) {
        long afterDeviceId = 0;
        while (true) {
            List<Long> deviceIds = driverLeaseManager.listDriverDeviceIds(
                    tenantId, driverId, afterDeviceId, RECONCILE_PAGE_SIZE);
            if (deviceIds.isEmpty()) {
                return;
            }
            List<DeviceLeaseDO> assignments = new ArrayList<>(deviceIds.size());
            for (Long deviceId : deviceIds) {
                assignments.add(new DeviceLeaseDO(tenantId, driverId, deviceId,
                        selectOwner(deviceId, activeNodes), null));
            }
            driverLeaseManager.reconcileDeviceLeases(assignments);
            if (deviceIds.size() < RECONCILE_PAGE_SIZE) {
                return;
            }
            afterDeviceId = deviceIds.getLast();
        }
    }

    private String membershipHash(List<String> activeNodes) {
        MessageDigest digest = SHA_256.get();
        digest.reset();
        return HexFormat.of().formatHex(digest.digest(String.join("\u0000", activeNodes)
                .getBytes(StandardCharsets.UTF_8)));
    }

    private int compareUnsigned(byte[] left, byte[] right) {
        for (int i = 0; i < left.length; i++) {
            int comparison = Integer.compare(Byte.toUnsignedInt(left[i]), Byte.toUnsignedInt(right[i]));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private void validate(Long tenantId, Long driverId, String node, String client,
                          String host, int leaseSeconds) {
        if (tenantId == null || tenantId <= 0 || driverId == null || driverId <= 0
                || node == null || node.isBlank() || client == null || client.isBlank()
                || host == null || host.isBlank()) {
            throw new ServiceException("Invalid driver lease identity");
        }
        if (leaseSeconds < MIN_LEASE_SECONDS || leaseSeconds > MAX_LEASE_SECONDS) {
            throw new ServiceException("Driver lease seconds must be between {} and {}",
                    MIN_LEASE_SECONDS, MAX_LEASE_SECONDS);
        }
    }
}
