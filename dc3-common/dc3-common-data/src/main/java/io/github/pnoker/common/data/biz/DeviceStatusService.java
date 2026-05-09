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

import io.github.pnoker.common.data.entity.query.DeviceQuery;

import java.util.Map;

/**
 * Interface for device status-related operations
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface DeviceStatusService {

    /**
     * Paged query of device status, used in conjunction with paged query of devices
     *
     * @param deviceQuery DeviceQuery, including pagination parameters
     * @return Map Long:String, where Long is the device ID and String is the device
     * status
     */
    Map<Long, String> selectByPage(DeviceQuery deviceQuery);

    /**
     * Query device status by profile ID
     *
     * @param tenantId  Tenant ID
     * @param profileId Profile ID
     * @return Map Long:String, where Long is the device ID and String is the device
     * status
     */
    Map<Long, String> selectByProfileId(Long tenantId, Long profileId);

}
