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

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Business service for point operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface PointService extends BaseService<PointBO, PointQuery> {

    /**
     * Profile ID TenantId
     *
     * @param profileId Point ID
     * @param tenantId  Tenant ID
     * @return Point
     */
    List<PointBO> listByProfileId(Long profileId, Long tenantId);

    /**
     * Device ID TenantId
     *
     * @param deviceId Device ID
     * @param tenantId Tenant ID
     * @return Point
     */
    List<PointBO> listByDeviceId(Long deviceId, Long tenantId);

    /**
     * ID
     *
     * @param profileIds ID
     * @return Point
     */
    List<PointBO> listByProfileIds(List<Long> profileIds);

    /**
     * Device ID
     *
     * @param ids Point ID
     * @return Point
     */
    List<PointBO> listByIds(Set<Long> ids);

    /**
     * @param pointIds Point ID
     * @return Map Long:Unit String
     */
    Map<Long, String> unit(Set<Long> pointIds);

    /**
     * @param pointId id
     * @return {@link Set}<{@link Long}>
     */
    DeviceByPointBO getPointStatisticsWithDevice(Long pointId);

    /**
     * @param deviceId
     * @return
     */
    Long getPointByDeviceId(Long deviceId);

    /**
     * @param deviceId
     * @return
     */
    PointConfigByDeviceBO getPointConfigByDeviceId(Long deviceId);

}
