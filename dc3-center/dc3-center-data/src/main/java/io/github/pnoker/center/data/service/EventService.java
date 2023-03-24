/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.data.entity.vo.query.DeviceEventPageQuery;
import io.github.pnoker.center.data.entity.vo.query.DriverEventPageQuery;
import io.github.pnoker.common.entity.DeviceEvent;
import io.github.pnoker.common.entity.DriverEvent;

import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
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
     * @param driverEventPageQuery 驱动事件和分页参数
     * @return Page Of DriverEvent
     */
    Page<DriverEvent> driverEvent(DriverEventPageQuery driverEventPageQuery);

    /**
     * 获取 DeviceEvent 带分页、排序
     *
     * @param deviceEventPageQuery 设备事件和分页参数
     * @return Page Of DeviceEvent
     */
    Page<DeviceEvent> deviceEvent(DeviceEventPageQuery deviceEventPageQuery);

}
