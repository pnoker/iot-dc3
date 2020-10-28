/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.service.mqtt;

import com.alibaba.fastjson.JSON;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.driver.bean.DevicePayLoad;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

import javax.annotation.Resource;

/**
 * mqtt上行数据例子：
 * {
 * deviceId:1,
 * pointId:1,
 * value:"1212"
 * }
 * 可根据实际情况定义 DevicePayLoad 类属性
 *
 * @author pnoker
 */
@Slf4j
@Configuration
public class MqttReceiveHandler {
    @Resource
    private DriverService driverService;

    /**
     * 说明：
     * ConditionalOnProperty(value = "driver.mqtt.default.receive.enable")
     * 根据配置属性driver.mqtt.default.receive.enable选择是否开启 Default Topic 主题的数据接收逻辑
     *
     * @return
     */
    @Bean
    @ServiceActivator(inputChannel = "defaultMqttInputChannel")
    @ConditionalOnProperty(value = "driver.mqtt.default.receive.enable")
    public MessageHandler defaultHandler() {
        return message -> {
            log.info(
                    "defaultTopicReceiver\nheader:{},\npayload:{}",
                    JSON.toJSONString(message.getHeaders(), true),
                    JSON.toJSONString(message.getPayload(), true)
            );
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            log.info(
                    "{}\nheader:{}, payload:{}",
                    message.getHeaders().get("mqtt_receivedTopic"),
                    JSON.toJSONString(message.getHeaders(), true),
                    JSON.toJSONString(message.getPayload(), true)
            );
            DevicePayLoad devicePayLoad = JSON.parseObject(message.getPayload().toString(), DevicePayLoad.class);
            PointValue pointValue = new PointValue(devicePayLoad.getDeviceId(), devicePayLoad.getPointId(), devicePayLoad.getValue(),
                    driverService.convertValue(devicePayLoad.getDeviceId(), devicePayLoad.getPointId(), devicePayLoad.getValue()));
            driverService.pointValueSender(pointValue);
        };
    }
}
