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
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface PointService extends BaseService<PointBO, PointQuery> {

    /**
     * 根据 模版ID 查询位号
     *
     * @param profileId 位号ID
     * @return Point 集合
     */
    List<PointBO> selectByProfileId(Long profileId);

    /**
     * 根据 设备ID 查询位号
     *
     * @param deviceId 设备ID
     * @return Point 集合
     */
    List<PointBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 模板ID 集查询位号
     *
     * @param profileIds 模版ID集
     * @return Point 集合
     */
    List<PointBO> selectByProfileIds(List<Long> profileIds);

    /**
     * 根据 设备ID集合 查询设备
     *
     * @param ids 位号ID集
     * @return Point 集合
     */
    List<PointBO> selectByIds(Set<Long> ids);

    /**
     * 查询 位号单位
     *
     * @param pointIds 位号ID集
     * @return Map Long:Unit String
     */
    Map<Long, String> unit(Set<Long> pointIds);

    /**
     * 位号被多少设备引用
     * 选择点位统计设备信息
     *
     * @param pointId 点位id
     * @return {@link Set}<{@link Long}>
     */
    DeviceByPointBO selectPointStatisticsWithDevice(Long pointId);

    /**
     * 位号在不同设备下的数据量
     * 按设备id统计位号数量
     *
     * @param pointId   点位id
     * @param deviceIds 设备id
     * @return {@link List}<{@link List}<{@link PointDataVolumeRunDO}>>
     */
    List<PointDataVolumeRunBO> selectPointStatisticsByDeviceId(Long pointId, Set<Long> deviceIds);


    /**
     * 当前位号下数据量
     * 按位号id统计位号数据量
     *
     * @param pointId 点位id
     * @return {@link List}<{@link List}<{@link PointDataVolumeRunDO}>>
     */
    PointDataVolumeRunDO selectPointStatisticsByPointId(Long pointId);

    /**
     * 设备下位号数量
     *
     * @param deviceId
     * @return
     */
    Long selectPointByDeviceId(Long deviceId);

    /**
     * 设备下位号数量 已配置
     *
     * @param deviceId
     * @return
     */
    PointConfigByDeviceBO selectPointConfigByDeviceId(Long deviceId);

    /**
     * 设备在不同位号下的数据量
     *
     * @param deviceId
     * @param pointIds
     * @return
     */
    List<DeviceDataVolumeRunBO> selectDeviceStatisticsByPointId(Long deviceId, Set<Long> pointIds);

    /**
     * 驱动下位号数量
     *
     * @param driverId
     * @return
     */
    PointDataVolumeRunDO selectPointDataByDriverId(Long driverId);

    /**
     * 驱动下位号数量
     *
     * @param driverId
     * @return
     */
    Long selectPointByDriverId(Long driverId);

    /**
     * 统计7天驱动下位号数据量
     *
     * @param driverId
     * @return
     */
    PointDataStatisticsByDriverIdBO selectPointDataStatisticsByDriverId(Long driverId);
}
