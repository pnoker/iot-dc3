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

package io.github.pnoker.common.manager.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * SQL mapper for driver-runtime leases and device assignments.
 * <p>
 * Automatic tenant interception is disabled because the advisory driver lock is not tenant-qualified. All remaining
 * statements must bind and filter the explicit {@code tenantId}; omitting that predicate is a tenant-isolation defect.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@InterceptorIgnore(tenantLine = "true")
public interface DriverLeaseMapper {

    /** Acquires the transaction-scoped database lock for a logical driver ID. */
    void acquireDriverLock(@Param("driverId") Long driverId);

    /** Inserts or renews one runtime instance without changing the assignment generation. */
    int upsertInstance(@Param("tenantId") Long tenantId,
                       @Param("driverId") Long driverId,
                       @Param("node") String node,
                       @Param("client") String client,
                       @Param("host") String host,
                       @Param("leaseUntil") Instant leaseUntil);

    /** Lists stable node identifiers whose lease expiry is later than the database clock. */
    List<String> listActiveNodes(@Param("tenantId") Long tenantId,
                                 @Param("driverId") Long driverId);

    /** Pages tenant-owned device IDs in ascending order using an exclusive cursor. */
    List<Long> listDriverDeviceIds(@Param("tenantId") Long tenantId,
                                   @Param("driverId") Long driverId,
                                   @Param("afterDeviceId") Long afterDeviceId,
                                   @Param("limit") Integer limit);

    /** Selects the current membership hash, inventory revision, and assignment version. */
    DriverLeaseStateDO selectLeaseState(@Param("tenantId") Long tenantId,
                                        @Param("driverId") Long driverId);

    /** Selects the device-inventory revision used to detect assignment-invalidating metadata changes. */
    Long selectDeviceRevision(@Param("tenantId") Long tenantId,
                              @Param("driverId") Long driverId);

    /** Upserts reconciliation state and returns the atomically advanced assignment version. */
    int upsertLeaseState(@Param("tenantId") Long tenantId,
                          @Param("driverId") Long driverId,
                          @Param("membershipHash") String membershipHash,
                          @Param("deviceRevision") Long deviceRevision);

    /** Deletes runtime records that expired before the retention cutoff. */
    int deleteExpiredInstances(@Param("tenantId") Long tenantId,
                               @Param("driverId") Long driverId,
                               @Param("expiredBefore") Instant expiredBefore);

    /** Upserts calculated owners and increments fencing tokens when an owner changes. */
    int upsertDeviceLeases(@Param("leases") List<DeviceLeaseDO> leases);

    /** Deletes assignments whose device or owner is absent from the active membership. */
    int deleteOrphanedLeases(@Param("tenantId") Long tenantId,
                             @Param("driverId") Long driverId);

    /** Lists unexpired assignments for one runtime node using an exclusive device-ID cursor. */
    List<DeviceLeaseDO> listOwnedLeases(@Param("tenantId") Long tenantId,
                                        @Param("driverId") Long driverId,
                                        @Param("node") String node,
                                        @Param("afterDeviceId") Long afterDeviceId,
                                        @Param("limit") Integer limit);

    /** Resolves the unexpired owner and fencing token for a tenant-owned device. */
    DeviceLeaseDO selectActiveLease(@Param("tenantId") Long tenantId,
                                    @Param("deviceId") Long deviceId);
}
