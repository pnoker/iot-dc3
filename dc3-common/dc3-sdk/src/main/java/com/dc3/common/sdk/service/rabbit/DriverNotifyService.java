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

package com.dc3.common.sdk.service.rabbit;

import com.dc3.common.bean.driver.DriverOperation;
import com.dc3.common.constant.Operation;
import com.dc3.common.sdk.service.DriverCommonService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 接收驱动发送过来的数据
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverNotifyService {

    @Resource
    private DriverCommonService driverCommonService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverNotifyQueue.name}")
    public void driverNotifyReceive(Channel channel, Message message, DriverOperation operation) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.debug("Notification from {}", message.getMessageProperties().getReceivedRoutingKey());

            switch (operation.getCommand()) {
                case Operation.Profile.ADD:
                    driverCommonService.addProfile(operation.getId());
                    break;
                case Operation.Profile.DELETE:
                    driverCommonService.deleteProfile(operation.getId());
                    break;
                case Operation.Device.ADD:
                    driverCommonService.addDevice(operation.getId());
                    break;
                case Operation.Device.DELETE:
                    driverCommonService.deleteDevice(operation.getId());
                    break;
                case Operation.Device.UPDATE:
                    driverCommonService.updateDevice(operation.getId());
                    break;
                case Operation.Point.ADD:
                    driverCommonService.addPoint(operation.getId());
                    break;
                case Operation.Point.DELETE:
                    driverCommonService.deletePoint(operation.getId(), operation.getParentId());
                    break;
                case Operation.Point.UPDATE:
                    driverCommonService.updatePoint(operation.getId());
                    break;
                case Operation.DriverInfo.ADD:
                    driverCommonService.addDriverInfo(operation.getId());
                    break;
                case Operation.DriverInfo.DELETE:
                    driverCommonService.deleteDriverInfo(operation.getAttributeId(), operation.getParentId());
                    break;
                case Operation.DriverInfo.UPDATE:
                    driverCommonService.updateDriverInfo(operation.getId());
                    break;
                case Operation.PointInfo.ADD:
                    driverCommonService.addPointInfo(operation.getId());
                    break;
                case Operation.PointInfo.DELETE:
                    driverCommonService.deletePointInfo(operation.getId(), operation.getAttributeId(), operation.getParentId());
                    break;
                case Operation.PointInfo.UPDATE:
                    driverCommonService.updatePointInfo(operation.getId());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
