package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

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
