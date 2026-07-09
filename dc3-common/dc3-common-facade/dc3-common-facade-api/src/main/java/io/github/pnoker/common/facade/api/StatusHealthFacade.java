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

    Map<Long, String> listDeviceStatusesByIds(Long tenantId, Collection<Long> deviceIds);

    Map<Long, String> listDeviceStatusesByProfileId(Long tenantId, Long profileId);

    Map<Long, String> listDriverStatusesByIds(Long tenantId, Collection<Long> driverIds);

    FacadeDriverDeviceStatusSummaryBO getDriverDeviceStatusSummary(Long tenantId, Long driverId);

    FacadeSystemHealthBO systemHealth(Long tenantId);

}
