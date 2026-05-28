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

package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.data.entity.query.DriverQuery;

import java.util.Map;

/**
 * Business service for driver status operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverStatusService {

    /**
     * Paged query of driver status, used in conjunction with paged query of drivers
     *
     * @param driverQuery DriverQuery, including pagination parameters
     * @return Map Long:String, where Long is the driver ID and String is the driver
     * status
     */
    Map<Long, String> getStatusByPage(DriverQuery driverQuery);

    /**
     * Count of currently online devices under the driver.
     *
     * @param tenantId Tenant ID
     * @param driverId Driver ID
     * @return Number of online devices
     */
    Long getDeviceOnlineByDriverId(Long tenantId, Long driverId);

    /**
     * Count of currently offline devices under the driver.
     *
     * @param tenantId Tenant ID
     * @param driverId Driver ID
     * @return Number of offline devices
     */
    Long getDeviceOfflineByDriverId(Long tenantId, Long driverId);

}
