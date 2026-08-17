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

import java.util.List;

public interface DriverLeaseService {
    DriverLeaseGrantBO renew(Long tenantId, Long driverId, String node, String client,
                             String host, int leaseSeconds, long knownAssignmentVersion);

    List<DeviceLeaseBO> listOwnedLeases(Long tenantId, Long driverId, String node,
                                        long afterDeviceId, int limit);

    long getAssignmentVersion(Long tenantId, Long driverId);

    DeviceLeaseBO getActiveOwner(Long tenantId, Long deviceId);
}
