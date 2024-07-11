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

package io.github.pnoker.common.mqtt.service;


import io.github.pnoker.common.mqtt.entity.MqttMessage;

import java.util.List;

/**
 * @author pnoker
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
