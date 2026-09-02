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
import io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO;
import io.github.pnoker.common.manager.entity.bo.DriverLeaseGrantBO;
import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import io.github.pnoker.common.manager.repository.ReactiveDriverLeaseStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive driver membership and fenced device-assignment state machine. */
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

    private final ReactiveDriverLeaseStore store;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<DriverLeaseGrantBO> renew(
            Long tenantId,
            Long driverId,
            String node,
            String client,
            String host,
            int leaseSeconds,
            long knownAssignmentVersion) {
        return Mono.defer(() -> {
            validate(tenantId, driverId, node, client, host, leaseSeconds, knownAssignmentVersion);
            Instant leaseUntil = Instant.now().plusSeconds(leaseSeconds);
            return transactionalOperator.transactional(store.acquireDriverLock(tenantId, driverId)
                    .then(store.renewInstance(tenantId, driverId, node, client, host, leaseUntil))
                    .then(store.deleteExpiredInstances(
                            tenantId, driverId, Instant.now().minusSeconds(EXPIRED_INSTANCE_RETENTION_SECONDS)))
                    .then(Mono.zip(
                            store.listActiveNodes(tenantId, driverId).collectList(),
                            store.getDeviceRevision(tenantId, driverId),
                            store.getLeaseState(tenantId, driverId)
                                    .map(Optional::of)
                                    .defaultIfEmpty(Optional.empty())))
                    .flatMap(tuple -> {
                        List<String> activeNodes = tuple.getT1();
                        if (activeNodes.isEmpty()) {
                            return Mono.error(new ServiceException("No active driver instance after lease renewal"));
                        }
                        String membershipHash = membershipHash(activeNodes);
                        long deviceRevision = tuple.getT2();
                        Optional<DriverLeaseStateDO> state = tuple.getT3();
                        boolean reconcile = state.isEmpty()
                                || !Objects.equals(state.get().getMembershipHash(), membershipHash)
                                || !Objects.equals(state.get().getDeviceRevision(), deviceRevision);
                        Mono<Long> version = reconcile
                                ? reconcileAssignments(tenantId, driverId, activeNodes)
                                        .then(store.advanceAssignmentVersion(
                                                tenantId, driverId, membershipHash, deviceRevision))
                                : Mono.just(state.get().getAssignmentVersion());
                        return version.map(assignmentVersion -> new DriverLeaseGrantBO(
                                leaseUntil.toEpochMilli(),
                                assignmentVersion,
                                knownAssignmentVersion != assignmentVersion));
                    }));
        });
    }

    @Override
    public Flux<DeviceLeaseBO> listOwnedLeases(
            Long tenantId, Long driverId, String node, long afterDeviceId, int limit) {
        if (!valid(tenantId)
                || !valid(driverId)
                || blank(node)
                || afterDeviceId < 0
                || limit < 1
                || limit > MAX_ASSIGNMENT_PAGE_SIZE) {
            return Flux.error(new ServiceException("Invalid driver assignment page request"));
        }
        return store.listOwnedLeases(tenantId, driverId, node, afterDeviceId, limit)
                .map(value -> new DeviceLeaseBO(
                        value.getDriverId(), value.getDeviceId(), value.getOwnerNode(), value.getFencingToken()));
    }

    @Override
    public Mono<Long> getAssignmentVersion(Long tenantId, Long driverId) {
        if (!valid(tenantId) || !valid(driverId)) {
            return Mono.error(new ServiceException("Invalid driver assignment identity"));
        }
        return store.getLeaseState(tenantId, driverId)
                .switchIfEmpty(Mono.error(new ServiceException("Driver assignment state does not exist")))
                .map(DriverLeaseStateDO::getAssignmentVersion)
                .switchIfEmpty(Mono.error(new ServiceException("Driver assignment state is incomplete")));
    }

    @Override
    public Mono<DeviceLeaseBO> getActiveOwner(Long tenantId, Long deviceId) {
        if (!valid(tenantId) || !valid(deviceId)) return Mono.empty();
        return store.getActiveLease(tenantId, deviceId)
                .map(lease -> new DeviceLeaseBO(
                        lease.getDriverId(), lease.getDeviceId(), lease.getOwnerNode(), lease.getFencingToken()));
    }

    private Mono<Void> reconcileAssignments(Long tenantId, Long driverId, List<String> activeNodes) {
        return loadDevicePage(tenantId, driverId, 0L)
                .expand(page -> page.done ? Mono.empty() : loadDevicePage(tenantId, driverId, page.cursor))
                .concatMap(page -> {
                    List<DeviceLeaseDO> assignments = page.deviceIds.stream()
                            .map(deviceId -> new DeviceLeaseDO(
                                    tenantId, driverId, deviceId, selectOwner(deviceId, activeNodes), null))
                            .toList();
                    return store.reconcileDeviceLeases(assignments);
                })
                .then(store.deleteOrphanedLeases(tenantId, driverId));
    }

    private Mono<DevicePage> loadDevicePage(Long tenantId, Long driverId, long afterDeviceId) {
        return store.listDriverDeviceIds(tenantId, driverId, afterDeviceId, RECONCILE_PAGE_SIZE)
                .collectList()
                .map(deviceIds -> {
                    if (deviceIds.isEmpty()) return new DevicePage(List.of(), afterDeviceId, true);
                    long cursor = deviceIds.getLast();
                    if (cursor <= afterDeviceId) {
                        throw new ServiceException("Driver device cursor did not advance");
                    }
                    return new DevicePage(deviceIds, cursor, deviceIds.size() < RECONCILE_PAGE_SIZE);
                });
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

    private String membershipHash(List<String> activeNodes) {
        MessageDigest digest = SHA_256.get();
        digest.reset();
        return HexFormat.of()
                .formatHex(digest.digest(String.join("\u0000", activeNodes).getBytes(StandardCharsets.UTF_8)));
    }

    private int compareUnsigned(byte[] left, byte[] right) {
        for (int index = 0; index < left.length; index++) {
            int comparison = Integer.compare(Byte.toUnsignedInt(left[index]), Byte.toUnsignedInt(right[index]));
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private void validate(
            Long tenantId,
            Long driverId,
            String node,
            String client,
            String host,
            int leaseSeconds,
            long knownAssignmentVersion) {
        if (!valid(tenantId)
                || !valid(driverId)
                || blank(node)
                || blank(client)
                || blank(host)
                || knownAssignmentVersion < 0) {
            throw new ServiceException("Invalid driver lease identity");
        }
        if (leaseSeconds < MIN_LEASE_SECONDS || leaseSeconds > MAX_LEASE_SECONDS) {
            throw new ServiceException(
                    "Driver lease seconds must be between {} and {}", MIN_LEASE_SECONDS, MAX_LEASE_SECONDS);
        }
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record DevicePage(List<Long> deviceIds, long cursor, boolean done) {}
}
