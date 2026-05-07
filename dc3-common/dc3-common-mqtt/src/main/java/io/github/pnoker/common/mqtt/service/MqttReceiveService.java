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
 * MQTT Receive Service Interface
 * <p>
 * Service interface for handling received MQTT messages in IoT DC3 platform. Provides
 * methods for single and batch message processing with data parsing and publishing
 * capabilities.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface MqttReceiveService {

    /**
     * Must implement single-point processing logic
     * <p>
     * Parse incoming data into io.github.pnoker.common.bean.point.PointValue then call
     * driverService.pointValueSender(pointValue) to publish data Tip: Refer to the
     * dc3-driver-listening-virtual driver for examples
     *
     * @param mqttMessage MqttMessage
     */
    void receiveValue(MqttMessage mqttMessage);

    /**
     * Must implement batch processing logic
     * <p>
     * Parse incoming data into io.github.pnoker.common.bean.point.PointValue then call
     * driverService.pointValueSender(pointValue) to publish data Tip: Refer to the
     * dc3-driver-listening-virtual driver for examples
     *
     * @param mqttMessageList String Array List
     */
    void receiveValues(List<MqttMessage> mqttMessageList);

}
