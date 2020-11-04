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
import com.dc3.common.bean.driver.DeviceEventDto;

import java.util.List;

/**
 * @author pnoker
 */
public interface DeviceEventService {

    /**
     * 获取设备状态
     *
     * @param deviceId Device Id
     * @return ONLINE, OFFLINE, MAINTAIN, FAULT
     */
    String deviceStatus(Long deviceId);

    /**
     * 新增 DeviceEvent
     *
     * @param deviceEvent DeviceEvent
     */
    void addDeviceEvent(DeviceEvent deviceEvent);

    /**
     * 批量新增 DeviceEvent
     *
     * @param deviceEvents DeviceEvent Array
     */
    void addDeviceEvents(List<DeviceEvent> deviceEvents);

    /**
     * 获取带分页、排序
     *
     * @param deviceEventDto DeviceEventDto
     * @return Page<DeviceEvent>
     */
    Page<DeviceEvent> list(DeviceEventDto deviceEventDto);

}
