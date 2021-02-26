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
import com.dc3.center.manager.service.DriverService;
import com.dc3.common.bean.driver.DriverConfiguration;
import com.dc3.common.bean.driver.DriverEvent;
import com.dc3.common.bean.driver.DriverMetadata;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.constant.Common;
import com.dc3.common.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private BatchService batchService;
    @Resource
    private DriverService driverService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEvent driverEvent) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (null == driverEvent || StringUtils.isEmpty(driverEvent.getServiceName())) {
                log.error("Invalid driver event");
                return;
            }
            log.debug("Driver {} event, From: {}, Event: {}", driverEvent.getType(), message.getMessageProperties().getReceivedRoutingKey(),driverEvent);

            if (Common.Driver.Event.CHECK_MANAGER_VALID.equals(driverEvent.getType())) {
                String back = Common.Driver.Status.REGISTERING;
                try {
                    DriverRegister driverRegister = Convert.convert(DriverRegister.class, driverEvent.getContent());
                    driverService.driverRegister(driverRegister);
                } catch (Exception e) {
                    back = e.getMessage();
                }

                rabbitTemplate.convertAndSend(
                        Common.Rabbit.TOPIC_EXCHANGE_CONFIGURATION,
                        Common.Rabbit.ROUTING_DRIVER_CONFIGURATION_PREFIX + driverEvent.getServiceName(),
                        new DriverConfiguration(
                                Common.Driver.Type.DRIVER,
                                Common.Driver.CHECK_MANAGER_VALID_BACK,
                                back
                        )
                );
            }

            if (Common.Driver.Event.SYNC_DRIVER_METADATA.equals(driverEvent.getType())) {
                String back = Common.Driver.Status.ONLINE;
                DriverMetadata driverMetadata = null;
                try {
                    driverMetadata = batchService.batchDriverMetadata(driverEvent.getServiceName());
                } catch (Exception e) {
                    back = e.getMessage();
                }

                rabbitTemplate.convertAndSend(
                        Common.Rabbit.TOPIC_EXCHANGE_CONFIGURATION,
                        Common.Rabbit.ROUTING_DRIVER_CONFIGURATION_PREFIX + driverEvent.getServiceName(),
                        new DriverConfiguration(
                                Common.Driver.Type.DRIVER,
                                Common.Driver.SYNC_DRIVER_METADATA_BACK,
                                driverMetadata,
                                back
                        )
                );
            }

            if (Common.Driver.Event.HEARTBEAT.equals(driverEvent.getType())) {
                // Save driver heartbeat to Redis
                redisUtil.setKey(
                        Common.Cache.DRIVER_STATUS_KEY_PREFIX + driverEvent.getServiceName(),
                        driverEvent.getContent(),
                        driverEvent.getTimeOut(),
                        driverEvent.getTimeUnit()
                );
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
