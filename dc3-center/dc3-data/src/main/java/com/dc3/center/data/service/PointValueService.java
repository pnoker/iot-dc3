/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.common.bean.driver.DeviceEvent;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;

import java.util.List;

/**
 * @author pnoker
 */
public interface PointValueService {

    /**
     * 数据纠正
     *
     * @param serviceName Driver Service Name
     * @return Boolean
     */
    Boolean correct(String serviceName);

    /**
     * 获取设备状态
     *
     * @param deviceId Device Id
     * @return ONLINE, OFFLINE, MAINTAIN, FAULT
     */
    String status(Long deviceId);

    /**
     * 获取实时数据
     *
     * @param deviceId Device Id
     * @return PointValue Array
     */
    List<PointValue> realtime(Long deviceId);

    /**
     * 获取实时数据
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointValue
     */
    PointValue realtime(Long deviceId, Long pointId);

    /**
     * 获取最新的一个位号数据
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointValue
     */
    PointValue latest(Long deviceId, Long pointId);

    /**
     * 新增 DeviceEvent
     *
     * @param deviceEvent DeviceEvent
     */
    void addDeviceEvent(DeviceEvent deviceEvent);

    /**
     * 新增 PointValue
     *
     * @param pointValue PointValue
     */
    void addPointValue(PointValue pointValue);

    /**
     * 批量新增 PointValue
     *
     * @param pointValues PointValue Array
     */
    void addPointValues(List<PointValue> pointValues);

    /**
     * 获取带分页、排序
     *
     * @param pointValueDto PointValueDto
     * @return Page<PointValue>
     */
    Page<PointValue> list(PointValueDto pointValueDto);

}
