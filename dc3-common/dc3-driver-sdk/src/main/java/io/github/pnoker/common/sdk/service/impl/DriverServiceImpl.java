/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.sdk.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.EventConstant;
import io.github.pnoker.common.constant.RabbitConstant;
import io.github.pnoker.common.enums.PointValueTypeEnum;
import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.model.DeviceEvent;
import io.github.pnoker.common.model.DriverEvent;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.sdk.service.DriverService;
import io.github.pnoker.common.utils.ConvertUtil;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void driverEventSender(DriverEvent driverEvent) {
        if (ObjectUtil.isNotNull(driverEvent)) {
            log.debug("Send driver event: {}", JsonUtil.toJsonString(driverEvent));
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_EVENT,
                    RabbitConstant.ROUTING_DRIVER_EVENT_PREFIX + serviceName,
                    driverEvent
            );
        }
    }

    @Override
    public void deviceEventSender(DeviceEvent deviceEvent) {
        if (ObjectUtil.isNotNull(deviceEvent)) {
            log.debug("Send device event: {}", JsonUtil.toJsonString(deviceEvent));
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_EVENT,
                    RabbitConstant.ROUTING_DEVICE_EVENT_PREFIX + serviceName,
                    deviceEvent
            );
        }
    }

    @Override
    public void deviceEventSender(String deviceId, String pointId, String type, String content) {
        deviceEventSender(new DeviceEvent(deviceId, pointId, type, content));
    }

    @Override
    public void deviceStatusSender(String deviceId, StatusEnum status) {
        deviceEventSender(new DeviceEvent(deviceId, EventConstant.Device.STATUS, status));
    }

    @Override
    public void pointValueSender(PointValue pointValue) {
        if (null != pointValue) {
            log.debug("Send point value: {}", JsonUtil.toJsonString(pointValue));
            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_VALUE,
                    RabbitConstant.ROUTING_POINT_VALUE_PREFIX + serviceName,
                    pointValue
            );
        }
    }

    @Override
    public void pointValueSender(List<PointValue> pointValues) {
        // TODO 需要添加新的队列支持list数据发送
        if (null != pointValues) {
            pointValues.forEach(this::pointValueSender);
        }
    }

    @Override
    public void close(CharSequence template, Object... params) {
        log.error(CharSequenceUtil.format(template, params));
        ((ConfigurableApplicationContext) applicationContext).close();
        System.exit(1);
    }

}
