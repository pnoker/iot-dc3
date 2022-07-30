/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.driver.mqtt.service.impl;

import io.github.pnoker.driver.mqtt.handler.MqttSendHandler;
import io.github.pnoker.driver.mqtt.service.MqttSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class MqttSendServiceImpl implements MqttSendService {

    @Resource
    private MqttSendHandler mqttSendHandler;

    @Override
    public void sendToMqtt(String data) {
        mqttSendHandler.sendToMqtt(data);
    }

    @Override
    public void sendToMqtt(Integer qos, String data) {
        mqttSendHandler.sendToMqtt(qos, data);
    }

    @Override
    public void sendToMqtt(String topic, String data) {
        mqttSendHandler.sendToMqtt(topic, data);
    }

    @Override
    public void sendToMqtt(String topic, Integer qos, String data) {
        mqttSendHandler.sendToMqtt(topic, qos, data);
    }
}
