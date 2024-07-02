/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
