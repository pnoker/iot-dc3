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

@InterceptorIgnore(tenantLine = "true")
public interface DriverLeaseMapper {
    void acquireDriverLock(@Param("driverId") Long driverId);

    int upsertInstance(@Param("tenantId") Long tenantId,
                       @Param("driverId") Long driverId,
                       @Param("node") String node,
                       @Param("client") String client,
                       @Param("host") String host,
                       @Param("leaseUntil") Instant leaseUntil);

    List<String> listActiveNodes(@Param("tenantId") Long tenantId,
                                 @Param("driverId") Long driverId);

    List<Long> listDriverDeviceIds(@Param("tenantId") Long tenantId,
                                   @Param("driverId") Long driverId,
                                   @Param("afterDeviceId") Long afterDeviceId,
                                   @Param("limit") Integer limit);

    DriverLeaseStateDO selectLeaseState(@Param("tenantId") Long tenantId,
                                        @Param("driverId") Long driverId);

    Long selectDeviceRevision(@Param("tenantId") Long tenantId,
                              @Param("driverId") Long driverId);

    Long upsertLeaseState(@Param("tenantId") Long tenantId,
                          @Param("driverId") Long driverId,
                          @Param("membershipHash") String membershipHash,
                          @Param("deviceRevision") Long deviceRevision);

    int deleteExpiredInstances(@Param("tenantId") Long tenantId,
                               @Param("driverId") Long driverId,
                               @Param("expiredBefore") Instant expiredBefore);

    int upsertDeviceLeases(@Param("leases") List<DeviceLeaseDO> leases);

    int deleteOrphanedLeases(@Param("tenantId") Long tenantId,
                             @Param("driverId") Long driverId);

    List<DeviceLeaseDO> listOwnedLeases(@Param("tenantId") Long tenantId,
                                        @Param("driverId") Long driverId,
                                        @Param("node") String node,
                                        @Param("afterDeviceId") Long afterDeviceId,
                                        @Param("limit") Integer limit);

    DeviceLeaseDO selectActiveLease(@Param("tenantId") Long tenantId,
                                    @Param("deviceId") Long deviceId);
}
