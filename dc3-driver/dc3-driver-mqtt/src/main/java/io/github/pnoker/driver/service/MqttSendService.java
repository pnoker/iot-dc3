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

package io.github.pnoker.driver.service;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Service for sending messages to MQTT topics. This interface provides methods for
 * publishing messages to MQTT brokers with support for custom topics and Quality of
 * Service (QoS) levels. It is implemented as a Spring Integration messaging gateway.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttSendService {

    /**
     * Send data using the default topic and default QoS
     *
     * @param data string
     */
    void sendToMqtt(String data);

    /**
     * Send data using the default topic and a custom QoS
     *
     * @param qos  Custom QoS
     * @param data string
     */
    void sendToMqtt(@Header(MqttHeaders.QOS) Integer qos, String data);

    /**
     * Send data using a custom topic and the default QoS
     *
     * @param topic Custom topic
     * @param data  string
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String data);

    /**
     * Send data using a custom topic and a custom QoS
     *
     * @param topic Custom topic
     * @param qos   Custom QoS
     * @param data  string
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) Integer qos, String data);

}
