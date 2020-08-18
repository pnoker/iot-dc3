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

package com.dc3.center.data.service.rabbit;

import com.dc3.common.bean.driver.DeviceStatus;
import com.dc3.common.constant.Common;
import com.dc3.common.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 接收驱动发送过来的数据
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DeviceStatusReceiver {

    @Resource
    private RedisUtil redisUtil;

    @RabbitHandler
    @RabbitListener(queues = "#{deviceStatusQueue.name}")
    public void pointValueReceive(Channel channel, Message message, DeviceStatus deviceStatus) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.debug("Device status from {}", message.getMessageProperties().getReceivedRoutingKey());

            if (null == deviceStatus || null == deviceStatus.getDeviceId()) {
                log.error("Invalid device status: {}", deviceStatus);
                return;
            }

            log.debug("Received device({}) status({})", deviceStatus.getDeviceId(), deviceStatus.getStatus());
            // Save device status to redis, 15 minutes
            redisUtil.setKey(
                    Common.Cache.DEVICE_STATUS_KEY_PREFIX + deviceStatus.getDeviceId(),
                    deviceStatus.getStatus(),
                    deviceStatus.getTimeOut(),
                    deviceStatus.getTimeUnit()
            );
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
