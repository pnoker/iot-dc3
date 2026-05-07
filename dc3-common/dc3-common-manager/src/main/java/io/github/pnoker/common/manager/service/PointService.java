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
import io.github.pnoker.common.manager.entity.bo.*;
import io.github.pnoker.common.manager.entity.model.PointDataVolumeRunDO;
import io.github.pnoker.common.manager.entity.query.PointQuery;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Point Interface
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface PointService extends BaseService<PointBO, PointQuery> {

    /**
     * ID
     *
     * @param profileId Point ID
     * @return Point
     */
    List<PointBO> selectByProfileId(Long profileId);

    /**
     * Device ID
     *
     * @param deviceId Device ID
     * @return Point
     */
    List<PointBO> selectByDeviceId(Long deviceId);

    /**
     * ID
     *
     * @param profileIds ID
     * @return Point
     */
    List<PointBO> selectByProfileIds(List<Long> profileIds);

    /**
     * Device ID
     *
     * @param ids Point ID
     * @return Point
     */
    List<PointBO> selectByIds(Set<Long> ids);

    /**
     * @param pointIds Point ID
     * @return Map Long:Unit String
     */
    Map<Long, String> unit(Set<Long> pointIds);

    /**
     * @param pointId id
     * @return {@link Set}<{@link Long}>
     */
    DeviceByPointBO selectPointStatisticsWithDevice(Long pointId);

    /**
     *
     * id
     *
     * @param pointId   id
     * @param deviceIds id
     * @return {@link List}<{@link List}<{@link PointDataVolumeRunDO}>>
     */
    List<PointDataVolumeRunBO> selectPointStatisticsByDeviceId(Long pointId, Set<Long> deviceIds);

    /**
     *
     * id
     *
     * @param pointId id
     * @return {@link List}<{@link List}<{@link PointDataVolumeRunDO}>>
     */
    PointDataVolumeRunDO selectPointStatisticsByPointId(Long pointId);

    /**
     * @param deviceId
     * @return
     */
    Long selectPointByDeviceId(Long deviceId);

    /**
     * @param deviceId
     * @return
     */
    PointConfigByDeviceBO selectPointConfigByDeviceId(Long deviceId);

    /**
     * @param deviceId
     * @param pointIds
     * @return
     */
    List<DeviceDataVolumeRunBO> selectDeviceStatisticsByPointId(Long deviceId, Set<Long> pointIds);

    /**
     * @param driverId
     * @return
     */
    PointDataVolumeRunDO selectPointDataByDriverId(Long driverId);

    /**
     * @param driverId
     * @return
     */
    Long selectPointByDriverId(Long driverId);

    /**
     * 7 days
     *
     * @param driverId
     * @return
     */
    PointDataStatisticsByDriverIdBO selectPointDataStatisticsByDriverId(Long driverId);

}
