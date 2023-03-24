/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service.rabbit;

import cn.hutool.core.util.ObjectUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.center.data.service.EventService;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.driver.EventConstant;
import io.github.pnoker.common.entity.DeviceEvent;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 接收驱动发送过来的设备事件
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DeviceEventReceiver {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private EventService eventService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RabbitHandler
    @RabbitListener(queues = "#{deviceEventQueue.name}")
    public void deviceEventReceive(Channel channel, Message message, DeviceEvent deviceEvent) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            if (ObjectUtil.isNull(deviceEvent) || ObjectUtil.isNull(deviceEvent.getDeviceId())) {
                log.error("Invalid device event: {}", deviceEvent);
                return;
            }
            log.debug("Device {} event, From: {}, Event: {}", deviceEvent.getType(), message.getMessageProperties().getReceivedRoutingKey(), deviceEvent);

            switch (deviceEvent.getType()) {
                // Save device heartbeat to Redis
                case EventConstant.Device.STATUS:
                    redisUtil.setKey(
                            PrefixConstant.DEVICE_STATUS_KEY_PREFIX + deviceEvent.getDeviceId(),
                            deviceEvent.getContent(),
                            deviceEvent.getTimeOut(),
                            deviceEvent.getTimeUnit()
                    );
                    break;
                case EventConstant.Device.ERROR:
                    //TODO 去重
                    threadPoolExecutor.execute(() -> eventService.addDeviceEvent(deviceEvent));
                    break;
                default:
                    log.error("Invalid event type, {}", deviceEvent.getType());
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
