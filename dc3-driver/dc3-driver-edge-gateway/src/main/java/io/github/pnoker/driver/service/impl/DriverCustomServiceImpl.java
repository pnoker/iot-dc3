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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.driver.entity.bean.RWPointValue;
import io.github.pnoker.common.driver.entity.dto.DeviceDTO;
import io.github.pnoker.common.driver.entity.dto.PointDTO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.bo.AttributeBO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.driver.service.MqttSendService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private DriverSenderService driverSenderService;
    @Resource
    private MqttSendService mqttSendService;

    @Override
    public void initial() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        你可以在此处执行一些特定的初始化逻辑, 驱动在启动的时候会自动执行该方法。
        */
    }

    @Override
    public void schedule() {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
        上传设备状态, 可自行灵活拓展, 不一定非要在schedule()接口中实现, 你可以: 
        - 在read中实现设备状态的判断；
        - 在自定义定时任务中实现设备状态的判断；
        - 根据某种判断机制实现设备状态的判断。

        最后根据 driverSenderService.deviceStatusSender(deviceId,deviceStatus) 接口将设备状态交给SDK管理, 其中设备状态（StatusEnum）:
        - ONLINE:在线
        - OFFLINE:离线
        - MAINTAIN:维护
        - FAULT:故障
         */
        deviceMetadata.getAllDevice().forEach(device -> driverSenderService.deviceStatusSender(device.getId(), DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public String read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceDTO device, PointDTO point) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!

        因为 MQTT 的数据来源是被动接收的, 所以无需实现该 Read 方法
        接收数据处理函数在 io.github.pnoker.driver.mqtt.handler.MqttReceiveHandler.handlerValue
        */
        return DefaultConstant.DEFAULT_NULL_STRING_VALUE;
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceDTO device, PointDTO point, RWPointValue values) {
        /*
        !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
         */
        AttributeBO commandTopicAttribute = pointConfig.get("commandTopic");
        String commandTopic = commandTopicAttribute.getAttributeValue(String.class);
        String value = values.getValue();
        try {
            AttributeBO commandQosAttribute = pointConfig.get("commandQos");
            int commandQos = commandQosAttribute.getAttributeValue(Integer.class);
            mqttSendService.sendToMqtt(commandTopic, commandQos, value);
        } catch (Exception e) {
            mqttSendService.sendToMqtt(commandTopic, value);
        }
        return true;
    }

}
