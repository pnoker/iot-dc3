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
package io.github.pnoker.common.manager.biz;

import io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO;
import io.github.pnoker.common.manager.entity.bo.DriverLeaseGrantBO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Coordinates driver-runtime leases and deterministic device ownership.
 * <p>
 * Every operation is tenant-scoped. Assignment versions and fencing tokens let command senders reject a stale runtime
 * after membership or device inventory changes. Callers must never substitute a driver ID or device ID from another
 * tenant.
 *
 * @author pnoker
 * @since 2026.5.22
 */
public interface DriverLeaseService {

    /**
     * Renews one runtime instance and reconciles device ownership when the active membership or device revision changed.
     *
     * @param tenantId               positive owning tenant ID
     * @param driverId               positive logical driver ID
     * @param node                   stable runtime node identifier used by the ownership algorithm
     * @param client                 runtime client identifier used for diagnostics
     * @param host                   runtime host used for diagnostics
     * @param leaseSeconds           requested lease duration, from 10 through 120 seconds
     * @param knownAssignmentVersion assignment version already observed by the runtime
     * @return renewed lease expiry, current assignment version, and whether the runtime must reload assignments
     */
    Mono<DriverLeaseGrantBO> renew(
            Long tenantId,
            Long driverId,
            String node,
            String client,
            String host,
            int leaseSeconds,
            long knownAssignmentVersion);

    /**
     * Lists the active device assignments owned by one runtime node using an exclusive device-ID cursor.
     *
     * @param tenantId      positive owning tenant ID
     * @param driverId      positive logical driver ID
     * @param node          runtime node whose assignments are requested
     * @param afterDeviceId exclusive cursor; zero starts the first page
     * @param limit         page size from 1 through 2000
     * @return assignments ordered by device ID
     */
    Flux<DeviceLeaseBO> listOwnedLeases(Long tenantId, Long driverId, String node, long afterDeviceId, int limit);

    /**
     * Returns the current fencing generation for one tenant-owned logical driver.
     *
     * @param tenantId positive owning tenant ID
     * @param driverId positive logical driver ID
     * @return current assignment version
     */
    Mono<Long> getAssignmentVersion(Long tenantId, Long driverId);

    /**
     * Resolves the unexpired runtime owner used to route a command to a tenant-owned device.
     *
     * @param tenantId positive owning tenant ID
     * @param deviceId positive device ID
     * @return active owner and fencing token, or {@code null} when the identity is invalid or no active lease exists
     */
    Mono<DeviceLeaseBO> getActiveOwner(Long tenantId, Long deviceId);
}
