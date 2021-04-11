/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.DeviceEvent;
import com.dc3.common.model.DriverEvent;
import com.dc3.common.model.Point;
import com.dc3.common.model.PointValue;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.DriverService;
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
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private DriverContext driverContext;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ApplicationContext applicationContext;

    public String convertValue(Long deviceId, Long pointId, String rawValue) {
        String value;
        Point point = driverContext.getDevicePointByDeviceIdAndPointId(deviceId, pointId);
        switch (point.getType()) {
            case Common.ValueType.STRING:
                value = rawValue;
                break;
            case Common.ValueType.BYTE:
            case Common.ValueType.SHORT:
            case Common.ValueType.INT:
            case Common.ValueType.LONG:
            case Common.ValueType.DOUBLE:
            case Common.ValueType.FLOAT:
                try {
                    float base = null != point.getBase() ? point.getBase() : 0;
                    float multiple = null != point.getMultiple() ? point.getMultiple() : 1;
                    double temp = (Convert.convert(Double.class, rawValue.trim()) + base) * multiple;
                    if (null != point.getMinimum() && temp < point.getMinimum()) {
                        log.info("Device({}) point({}) value({}) is lower than lower limit({})", deviceId, pointId, temp, point.getMinimum());
                        deviceEventSender(deviceId, pointId, Common.Device.Event.OVER_LOWER_LIMIT,
                                String.format("Value(%s) is lower than lower limit %s", temp, point.getMinimum()));
                    }
                    if (null != point.getMaximum() && temp > point.getMaximum()) {
                        log.info("Device({}) point({}) value({}) is greater than upper limit({})", deviceId, pointId, temp, point.getMaximum());
                        deviceEventSender(deviceId, pointId, Common.Device.Event.OVER_UPPER_LIMIT,
                                String.format("Value(%s) is greater than upper limit %s", temp, point.getMaximum()));
                    }
                    value = String.format(point.getFormat(), temp);
                } catch (Exception e) {
                    throw new ServiceException("Invalid device({}) point({}) value({}), error: {}", deviceId, pointId, rawValue, e.getMessage());
                }
                break;
            case Common.ValueType.BOOLEAN:
                try {
                    value = String.valueOf(Boolean.parseBoolean(rawValue.trim()));
                } catch (Exception e) {
                    throw new ServiceException("Invalid device({}) point({}) value({}), error: {}", deviceId, pointId, rawValue, e.getMessage());
                }
                break;
            default:
                throw new ServiceException("Invalid device({}) point({}) value({}) type: {} ", deviceId, pointId, rawValue, point.getType());
        }

        return value;
    }

    @Override
    public void driverEventSender(DriverEvent driverEvent) {
        if (null != driverEvent) {
            log.debug("Send driver event: {}", JSON.toJSONString(driverEvent));
            rabbitTemplate.convertAndSend(
                    Common.Rabbit.TOPIC_EXCHANGE_EVENT,
                    Common.Rabbit.ROUTING_DRIVER_EVENT_PREFIX + serviceName,
                    driverEvent
            );
        }
    }

    public void deviceEventSender(DeviceEvent deviceEvent) {
        if (null != deviceEvent) {
            log.debug("Send device event: {}", JSON.toJSONString(deviceEvent));
            rabbitTemplate.convertAndSend(
                    Common.Rabbit.TOPIC_EXCHANGE_EVENT,
                    Common.Rabbit.ROUTING_DEVICE_EVENT_PREFIX + serviceName,
                    deviceEvent
            );
        }
    }

    public void deviceEventSender(Long deviceId, String type, String content) {
        deviceEventSender(new DeviceEvent(deviceId, type, content));
    }

    public void deviceEventSender(Long deviceId, Long pointId, String type, String content) {
        deviceEventSender(new DeviceEvent(deviceId, pointId, type, content));
    }

    public void pointValueSender(PointValue pointValue) {
        if (null != pointValue) {
            log.debug("Send point value: {}", JSON.toJSONString(pointValue));
            rabbitTemplate.convertAndSend(
                    Common.Rabbit.TOPIC_EXCHANGE_VALUE,
                    Common.Rabbit.ROUTING_POINT_VALUE_PREFIX + serviceName,
                    pointValue
            );
        }
    }

    public void pointValueSender(List<PointValue> pointValues) {
        // TODO 需要添加新的队列支持list数据发送
        if (null != pointValues) {
            pointValues.forEach(this::pointValueSender);
        }
    }

    public void close(CharSequence template, Object... params) {
        log.error(StrUtil.format(template, params));
        ((ConfigurableApplicationContext) applicationContext).close();
        System.exit(1);
    }

}
