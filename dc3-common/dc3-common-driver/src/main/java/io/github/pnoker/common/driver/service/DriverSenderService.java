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

package io.github.pnoker.common.driver.service;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverSenderService {

    /**
     * 发送驱动事件
     *
     * @param entityDTO DriverEventDTO
     */
    void driverEventSender(DriverEventDTO entityDTO);

    /**
     * 发送设备事件
     *
     * @param entityDTO Device Event
     */
    void deviceEventSender(DeviceEventDTO entityDTO);

    /**
     * 发送设备状态事件
     * <p>
     * 设备状态默认15分钟失效
     *
     * @param deviceId 设备ID
     * @param status   StatusEnum
     */
    void deviceStatusSender(Long deviceId, DeviceStatusEnum status);

    /**
     * 发送设备状态事件
     *
     * @param deviceId 设备ID
     * @param status   StatusEnum
     * @param timeOut  失效时间
     * @param timeUnit 失效时间单位 {@link TimeUnit}
     */
    void deviceStatusSender(Long deviceId, DeviceStatusEnum status, int timeOut, TimeUnit timeUnit);

    /**
     * 发送位号值到消息组件
     *
     * @param entityDTO PointValue
     */
    void pointValueSender(PointValue entityDTO);

    /**
     * 批量发送位号值到消息组件
     *
     * @param entityDTOList PointValue Array
     */
    void pointValueSender(List<PointValue> entityDTOList);

}
