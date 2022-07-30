/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.dto.DeviceEventDto;
import io.github.pnoker.common.dto.DriverEventDto;
import io.github.pnoker.common.model.DeviceEvent;
import io.github.pnoker.common.model.DriverEvent;

import java.util.List;

/**
 * @author pnoker
 */
public interface EventService {

    /**
     * 新增 DriverEvent
     *
     * @param driverEvent DriverEvent
     */
    void addDriverEvent(DriverEvent driverEvent);

    /**
     * 批量新增 DriverEvent
     *
     * @param driverEvents DriverEvent Array
     */
    void addDriverEvents(List<DriverEvent> driverEvents);

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
     * 获取 DriverEvent 带分页、排序
     *
     * @param driverEventDto DriverEventDto
     * @return Page<DriverEvent>
     */
    Page<DriverEvent> driverEvent(DriverEventDto driverEventDto);

    /**
     * 获取 DeviceEvent 带分页、排序
     *
     * @param deviceEventDto DeviceEventDto
     * @return Page<DeviceEvent>
     */
    Page<DeviceEvent> deviceEvent(DeviceEventDto deviceEventDto);

}
