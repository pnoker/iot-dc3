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
import com.dc3.center.manager.service.BatchService;
import com.dc3.center.manager.service.DriverSdkService;
import com.dc3.common.bean.driver.DriverConfiguration;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.constant.Common;
import com.dc3.common.model.DriverEvent;
import com.dc3.common.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    private BatchService batchService;
    @Resource
    private DriverSdkService driverSdkService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEvent driverEvent) {
        try {
            MessageProperties properties = message.getMessageProperties();
            channel.basicAck(properties.getDeliveryTag(), true);
            if (null == driverEvent || StringUtils.isEmpty(driverEvent.getServiceName())) {
                log.error("Invalid driver event {}", driverEvent);
                return;
            }

            log.debug("Driver {} event, From: {}, Received: {}", driverEvent.getType(), message.getMessageProperties().getReceivedRoutingKey(), driverEvent);
            String routingKey = Common.Rabbit.ROUTING_DRIVER_CONFIGURATION_PREFIX + driverEvent.getServiceName();

            switch (driverEvent.getType()) {
                case Common.Driver.Event.REGISTER_HANDSHAKE:
                    DriverConfiguration driverConfiguration = new DriverConfiguration(
                            Common.Driver.Type.DRIVER,
                            Common.Driver.Event.REGISTER_HANDSHAKE_BACK,
                            null,
                            Common.Response.OK
                    );
                    rabbitTemplate.convertAndSend(
                            Common.Rabbit.TOPIC_EXCHANGE_CONFIGURATION,
                            routingKey,
                            driverConfiguration
                    );
                    break;
                case Common.Driver.Event.DRIVER_REGISTER:
                    driverConfiguration = new DriverConfiguration(
                            Common.Driver.Type.DRIVER,
                            Common.Driver.Event.DRIVER_REGISTER_BACK,
                            null,
                            Common.Response.OK
                    );
                    try {
                        driverSdkService.driverRegister(Convert.convert(DriverRegister.class, driverEvent.getContent()));
                    } catch (Exception e) {
                        driverConfiguration.setResponse(e.getMessage());
                    }
                    rabbitTemplate.convertAndSend(
                            Common.Rabbit.TOPIC_EXCHANGE_CONFIGURATION,
                            routingKey,
                            driverConfiguration
                    );
                    break;
                case Common.Driver.Event.SYNC_DRIVER_METADATA:
                    driverConfiguration = new DriverConfiguration(
                            Common.Driver.Type.DRIVER,
                            Common.Driver.Event.SYNC_DRIVER_METADATA_BACK,
                            null,
                            Common.Response.OK
                    );
                    try {
                        driverConfiguration.setContent(batchService.batchDriverMetadata(driverEvent.getServiceName()));
                    } catch (Exception e) {
                        driverConfiguration.setResponse(e.getMessage());
                    }
                    rabbitTemplate.convertAndSend(
                            Common.Rabbit.TOPIC_EXCHANGE_CONFIGURATION,
                            routingKey,
                            driverConfiguration
                    );
                    break;
                case Common.Driver.Event.HEARTBEAT:
                    redisUtil.setKey(
                            Common.Cache.DRIVER_STATUS_KEY_PREFIX + driverEvent.getServiceName(),
                            driverEvent.getContent(),
                            driverEvent.getTimeOut(),
                            driverEvent.getTimeUnit()
                    );
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
