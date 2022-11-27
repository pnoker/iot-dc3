/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.sdk.service;

import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.model.DeviceEvent;
import io.github.pnoker.common.model.DriverEvent;

import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverService {

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
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @param type     Event Type, STATUS、LIMIT
     * @param content  Event Content
     */
    void deviceEventSender(String deviceId, String pointId, String type, String content);

    /**
     * 发送设备状态事件
     *
     * @param deviceId Device ID
     * @param status   StatusEnum
     */
    void deviceStatusSender(String deviceId, StatusEnum status);

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
