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

package io.github.pnoker.common.mqtt.service;


import io.github.pnoker.common.mqtt.entity.MqttMessage;

import java.util.List;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface MqttReceiveService {

    /**
     * 务必实现, 单点逻辑
     * <p>
     * 将解析之后的数据封装 io.github.pnoker.common.bean.point.PointValue
     * 然后调用 driverService.pointValueSender(pointValue) 进行数据推送
     * Tip:  可参考 dc3-driver-listening-virtual 驱动
     *
     * @param mqttMessage MqttMessage
     */
    void receiveValue(MqttMessage mqttMessage);

    /**
     * 务必实现, 批量逻辑
     * <p>
     * 将解析之后的数据封装 io.github.pnoker.common.bean.point.PointValue
     * 然后调用 driverService.pointValueSender(pointValue) 进行数据推送
     * Tip:  可参考 dc3-driver-listening-virtual 驱动
     *
     * @param mqttMessageList String Array List
     */
    void receiveValues(List<MqttMessage> mqttMessageList);
}
