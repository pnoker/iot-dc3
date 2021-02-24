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

package com.dc3.center.manager.service.rabbit;

import cn.hutool.core.convert.Convert;
import com.dc3.center.manager.service.DriverService;
import com.dc3.common.bean.driver.DriverEvent;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.constant.Common;
import com.dc3.common.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 接收驱动发送过来的驱动事件数据
 * 其中包括：驱动心跳事件、在线、离线、故障等其他事件
 *
 * @author pnoker
 */
@Slf4j
@Component
public class DriverEventReceiver {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private DriverService driverService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEvent driverEvent) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == driverEvent || StringUtils.isEmpty(driverEvent.getServiceName())) {
                log.error("Invalid driver event");
                return;
            }

            if (Common.Driver.Event.HEARTBEAT.equals(driverEvent.getType())) {
                log.debug("Driver heartbeat event, From: {}, Received: {}", message.getMessageProperties().getReceivedRoutingKey(), driverEvent);
                // Save driver heartbeat to Redis
                redisUtil.setKey(
                        Common.Cache.DRIVER_STATUS_KEY_PREFIX + driverEvent.getServiceName(),
                        driverEvent.getContent(),
                        driverEvent.getTimeOut(),
                        driverEvent.getTimeUnit()
                );
            }

            if (Common.Driver.Event.SYNC.equals(driverEvent.getType())) {
                log.info("Driver sync driver metadata event, From: {}, Received: {}", message.getMessageProperties().getReceivedRoutingKey(), driverEvent);
                driverService.syncDriverMetadata(Convert.convert(DriverRegister.class, driverEvent.getContent()));
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
