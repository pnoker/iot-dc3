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

package com.dc3.common.sdk.service;

import cn.hutool.core.convert.Convert;
import com.dc3.common.bean.driver.DeviceEvent;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.DriverContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverService {

    @Value("${spring.application.name}")
    private String serviceName;

    @Resource
    private DriverContext driverContext;
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 将位号原始值进行处理和转换
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param rawValue Raw Value
     * @return PointValue
     */
    public String convertValue(Long deviceId, Long pointId, String rawValue) {
        String value;
        Point point = driverContext.getDevicePoint(deviceId, pointId);
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
                        deviceEventSender(deviceId, pointId, Common.Device.Event.LIMIT,
                                String.format("Value(%s) is lower than lower limit %s", temp, point.getMinimum()));
                    }
                    if (null != point.getMaximum() && temp > point.getMaximum()) {
                        log.info("Device({}) point({}) value({}) is greater than upper limit({})", deviceId, pointId, temp, point.getMaximum());
                        deviceEventSender(deviceId, pointId, Common.Device.Event.LIMIT,
                                String.format("Value(%s) is greater than upper limit %s", temp, point.getMaximum()));
                    }
                    value = String.format(point.getFormat(), temp);
                } catch (Exception e) {
                    throw new ServiceException(String.format("Invalid device(%s) point(%s) value(%s), error: %s", deviceId, pointId, rawValue, e.getMessage()));
                }
                break;
            case Common.ValueType.BOOLEAN:
                try {
                    value = String.valueOf(Boolean.parseBoolean(rawValue.trim()));
                } catch (Exception e) {
                    throw new ServiceException(String.format("Invalid device(%s) point(%s) value(%s), error: %s", deviceId, pointId, rawValue, e.getMessage()));
                }
                break;
            default:
                throw new ServiceException(String.format("Invalid device(%s) point(%s) value(%s) type: %s ", deviceId, pointId, rawValue, point.getType()));
        }

        log.debug("Convert device({}) point({}) rawValue({}) to value({})", deviceId, pointId, rawValue, value);
        return value;
    }

    /**
     * 发送设备事件
     *
     * @param deviceEvent Device Event
     */
    public void deviceEventSender(DeviceEvent deviceEvent) {
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_EVENT, Common.Rabbit.ROUTING_DEVICE_EVENT_PREFIX + serviceName, deviceEvent);
    }

    /**
     * 发送设备事件
     *
     * @param deviceId Device Id
     * @param type     Event Type, STATUS、LIMIT
     * @param content  Event Content
     */
    public void deviceEventSender(Long deviceId, String type, String content) {
        deviceEventSender(new DeviceEvent(deviceId, type, content));
    }

    /**
     * 发送设备事件
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param type     Event Type, STATUS、LIMIT
     * @param content  Event Content
     */
    public void deviceEventSender(Long deviceId, Long pointId, String type, String content) {
        deviceEventSender(new DeviceEvent(deviceId, pointId, type, content));
    }

    /**
     * 发送位号值到消息组件
     *
     * @param pointValue PointValue
     */
    public void pointValueSender(PointValue pointValue) {
        if (null != pointValue) {
            log.debug("Send single point data: {}", pointValue);
            rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_VALUE, Common.Rabbit.ROUTING_POINT_VALUE_PREFIX + serviceName, pointValue);
        }
    }

    /**
     * 批量发送位号值到消息组件
     *
     * @param pointValues PointValue Array
     */
    public void pointValueSender(List<PointValue> pointValues) {
        pointValues.forEach(this::pointValueSender);
    }

}
