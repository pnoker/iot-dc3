/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.service;

import com.dc3.common.model.DeviceEvent;
import com.dc3.common.model.DriverEvent;
import com.dc3.common.model.PointValue;

import java.util.List;

/**
 * @author pnoker
 */
public interface DriverService {

    /**
     * 将位号原始值进行处理和转换
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param rawValue Raw Value
     * @return PointValue
     */
    String convertValue(Long deviceId, Long pointId, String rawValue);

    /**
     * 发送驱动事件
     *
     * @param driverEvent Driver Event
     */
    void driverEventSender(DriverEvent driverEvent);

    /**
     * 发送设备事件
     *
     * @param deviceEvent Device Event
     */
    void deviceEventSender(DeviceEvent deviceEvent);

    /**
     * 发送设备事件
     *
     * @param deviceId Device Id
     * @param type     Event Type, STATUS、LIMIT
     * @param content  Event Content
     */
    void deviceEventSender(Long deviceId, String type, String content);

    /**
     * 发送设备事件
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param type     Event Type, STATUS、LIMIT
     * @param content  Event Content
     */
    void deviceEventSender(Long deviceId, Long pointId, String type, String content);

    /**
     * 发送位号值到消息组件
     *
     * @param pointValue PointValue
     */
    void pointValueSender(PointValue pointValue);

    /**
     * 批量发送位号值到消息组件
     *
     * @param pointValues PointValue Array
     */
    void pointValueSender(List<PointValue> pointValues);

    /**
     * Close ApplicationContext
     *
     * @param template Template
     * @param params   Object Params
     */
    void close(CharSequence template, Object... params);

}
