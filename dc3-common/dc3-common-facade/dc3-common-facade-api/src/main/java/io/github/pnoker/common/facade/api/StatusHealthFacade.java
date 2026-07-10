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

package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;

import java.util.Collection;
import java.util.Map;

/**
 * Protocol-neutral status and health facade.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2026.5.14
 */
public interface StatusHealthFacade {

    /**
     * Resolve the status code for each device id, scoped to a tenant.
     *
     * @param tenantId  tenant scope
     * @param deviceIds device ids to resolve
     * @return map from device id to its status code
     */
    Map<Long, String> listDeviceStatusesByIds(Long tenantId, Collection<Long> deviceIds);

    /**
     * Resolve the status code for each device sharing a profile, scoped to a tenant.
     *
     * @param tenantId  tenant scope
     * @param profileId profile whose devices to resolve
     * @return map from device id to its status code
     */
    Map<Long, String> listDeviceStatusesByProfileId(Long tenantId, Long profileId);

    /**
     * Resolve the status code for each driver id, scoped to a tenant.
     *
     * @param tenantId  tenant scope
     * @param driverIds driver ids to resolve
     * @return map from driver id to its status code
     */
    Map<Long, String> listDriverStatusesByIds(Long tenantId, Collection<Long> driverIds);

    /**
     * Summarize the status of a driver and its devices, scoped to a tenant.
     *
     * @param tenantId tenant scope
     * @param driverId driver id
     * @return the driver/device status summary
     */
    FacadeDriverDeviceStatusSummaryBO getDriverDeviceStatusSummary(Long tenantId, Long driverId);

    /**
     * Snapshot the system health (centers, infrastructure, fleet) for a tenant.
     *
     * @param tenantId tenant scope
     * @return the system health snapshot
     */
    FacadeSystemHealthBO systemHealth(Long tenantId);

}
