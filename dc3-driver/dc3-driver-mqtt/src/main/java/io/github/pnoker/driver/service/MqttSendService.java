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

package io.github.pnoker.driver.service;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/**
 * @author pnoker
 * @version 2025.6.1
 * @since 2022.1.0
 */
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttSendService {
    /**
     * 使用 Default Topic 和 Default Qos 发送数据
     *
     * @param data string
     */
    void sendToMqtt(String data);

    /**
     * 使用 Default Topic 和 自定义 Qos 发送数据
     *
     * @param qos  自定义 Qos
     * @param data string
     */
    void sendToMqtt(@Header(MqttHeaders.QOS) Integer qos, String data);

    /**
     * 使用 自定义 Topic 和 Default Qos 发送数据
     *
     * @param topic 自定义 Topic
     * @param data  string
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String data);

    /**
     * 使用 自定义 Topic 和 自定义 Qos 发送数据
     *
     * @param topic 自定义 Topic
     * @param qos   自定义 Qos
     * @param data  string
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) Integer qos, String data);
}