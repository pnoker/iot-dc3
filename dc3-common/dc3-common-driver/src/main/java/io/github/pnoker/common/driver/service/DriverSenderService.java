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

package io.github.pnoker.common.driver.service;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author pnoker
 * @version 2025.6.0
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
