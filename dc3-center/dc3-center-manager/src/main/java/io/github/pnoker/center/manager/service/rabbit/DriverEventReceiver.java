/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.manager.service.rabbit;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.pnoker.center.manager.service.BatchService;
import io.github.pnoker.center.manager.service.DriverSdkService;
import io.github.pnoker.center.manager.service.EventService;
import io.github.pnoker.common.bean.driver.DriverConfiguration;
import io.github.pnoker.common.bean.driver.DriverRegister;
import io.github.pnoker.common.constant.CacheConstant;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.model.DriverEvent;
import io.github.pnoker.common.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

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
    private EventService eventService;
    @Resource
    private BatchService batchService;
    @Resource
    private DriverSdkService driverSdkService;

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEvent driverEvent) {
        try {
            MessageProperties properties = message.getMessageProperties();
            channel.basicAck(properties.getDeliveryTag(), true);
            if (null == driverEvent || StrUtil.isEmpty(driverEvent.getServiceName())) {
                log.error("Invalid driver event {}", driverEvent);
                return;
            }

            log.debug("Driver {} event, From: {}, Received: {}", driverEvent.getType(), message.getMessageProperties().getReceivedRoutingKey(), driverEvent);
            String routingKey = CommonConstant.Rabbit.ROUTING_DRIVER_METADATA_PREFIX + driverEvent.getServiceName();

            switch (driverEvent.getType()) {
                case CommonConstant.Driver.Event.DRIVER_HANDSHAKE:
                    DriverConfiguration driverConfiguration = new DriverConfiguration(
                            CommonConstant.Driver.Type.DRIVER,
                            CommonConstant.Driver.Event.DRIVER_HANDSHAKE_BACK,
                            null,
                            CommonConstant.Response.OK
                    );
                    rabbitTemplate.convertAndSend(
                            CommonConstant.Rabbit.TOPIC_EXCHANGE_METADATA,
                            routingKey,
                            driverConfiguration
                    );
                    break;
                case CommonConstant.Driver.Event.DRIVER_REGISTER:
                    driverConfiguration = new DriverConfiguration(
                            CommonConstant.Driver.Type.DRIVER,
                            CommonConstant.Driver.Event.DRIVER_REGISTER_BACK,
                            null,
                            CommonConstant.Response.OK
                    );
                    try {
                        driverSdkService.driverRegister(Convert.convert(DriverRegister.class, driverEvent.getContent()));
                    } catch (Exception e) {
                        driverConfiguration.setResponse(e.getMessage());
                    }
                    rabbitTemplate.convertAndSend(
                            CommonConstant.Rabbit.TOPIC_EXCHANGE_METADATA,
                            routingKey,
                            driverConfiguration
                    );
                    break;
                case CommonConstant.Driver.Event.DRIVER_METADATA_SYNC:
                    driverConfiguration = new DriverConfiguration(
                            CommonConstant.Driver.Type.DRIVER,
                            CommonConstant.Driver.Event.DRIVER_METADATA_SYNC_BACK,
                            null,
                            CommonConstant.Response.OK
                    );
                    try {
                        driverConfiguration.setContent(batchService.batchDriverMetadata(driverEvent.getServiceName()));
                    } catch (Exception e) {
                        driverConfiguration.setResponse(e.getMessage());
                    }
                    rabbitTemplate.convertAndSend(
                            CommonConstant.Rabbit.TOPIC_EXCHANGE_METADATA,
                            routingKey,
                            driverConfiguration
                    );
                    break;
                case CommonConstant.Driver.Event.DRIVER_HEARTBEAT:
                    redisUtil.setKey(
                            CacheConstant.Prefix.DRIVER_STATUS_KEY_PREFIX + driverEvent.getServiceName(),
                            driverEvent.getContent(),
                            driverEvent.getTimeOut(),
                            driverEvent.getTimeUnit()
                    );
                    break;
                case CommonConstant.Driver.Event.ERROR:
                    //TODO 去重
                    threadPoolExecutor.execute(() -> eventService.addDriverEvent(driverEvent));
                default:
                    log.error("Invalid event type, {}", driverEvent.getType());
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
