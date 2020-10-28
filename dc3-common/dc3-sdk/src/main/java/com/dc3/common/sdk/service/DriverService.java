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

import com.dc3.api.center.manager.feign.BatchClient;
import com.dc3.common.bean.R;
import com.dc3.common.bean.batch.BatchDriver;
import com.dc3.common.bean.driver.DeviceEvent;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.constant.Common;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.util.DriverUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private BatchClient batchClient;

    /**
     * 批量导入
     * 可通过解析配置文件实现
     *
     * @param batchDrivers List<BatchDriver>
     * @return boolean
     */
    public boolean batchImportBatchDriver(List<BatchDriver> batchDrivers) {
        R<Boolean> batchDriver = batchClient.batchImportBatchDriver(batchDrivers);
        return batchDriver.isOk();
    }

    /**
     * 将位号原始值进行处理和转换
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param rawValue Raw Value
     * @return PointValue
     */
    public String convertValue(Long deviceId, Long pointId, String rawValue) {
        String value = DriverUtils.processValue(rawValue, driverContext.getDevicePoint(deviceId, pointId));
        log.debug("Convert device({}), point({}), raw: {},to value: {}", deviceId, pointId, rawValue, value);
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
        DeviceEvent deviceEvent = new DeviceEvent(deviceId, type, content);
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_EVENT, Common.Rabbit.ROUTING_DEVICE_EVENT_PREFIX + serviceName, deviceEvent);
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
        DeviceEvent deviceEvent = new DeviceEvent(deviceId, pointId, type, content);
        rabbitTemplate.convertAndSend(Common.Rabbit.TOPIC_EXCHANGE_EVENT, Common.Rabbit.ROUTING_DEVICE_EVENT_PREFIX + serviceName, deviceEvent);
    }

    /**
     * 发送设备状态，同时设置实时数据超时时间
     * 设备状态值
     * Common.Device
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     * 在线，离线，维护，故障
     *
     * @param deviceId Device Id
     * @param status   Common.Device [ONLINE, OFFLINE, MAINTAIN, FAULT]
     */
    public void deviceStatusSender(Long deviceId, String status) {
        deviceEventSender(deviceId, Common.Device.Event.STATUS, status);
    }

    /**
     * 发送设备状态，同时设置实时数据超时时间
     * 设备状态值
     * Common.Device
     * ONLINE, OFFLINE, MAINTAIN, FAULT
     * 在线，离线，维护，故障
     *
     * @param deviceId Device Id
     * @param status   Common.Device [ONLINE, OFFLINE, MAINTAIN, FAULT]
     * @param timeOut  超时时间
     * @param timeUnit 超时时间单位 java.util.concurrent.TimeUnit
     */
    public void deviceStatusSender(Long deviceId, String status, int timeOut, TimeUnit timeUnit) {
        deviceEventSender(new DeviceEvent(deviceId, Common.Device.Event.STATUS, status, timeOut, timeUnit));
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
