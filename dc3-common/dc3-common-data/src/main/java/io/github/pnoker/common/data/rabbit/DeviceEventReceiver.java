/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.data.rabbit;

import cn.hutool.core.text.CharSequenceUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.DeviceEventService;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * 接收驱动发送过来的设备事件
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DeviceEventReceiver {

    private final DeviceEventService deviceEventService;

    public DeviceEventReceiver(DeviceEventService deviceEventService) {
        this.deviceEventService = deviceEventService;
    }

    @RabbitHandler
    @RabbitListener(queues = "#{deviceEventQueue.name}")
    public void deviceEventReceive(Channel channel, Message message, DeviceEventDTO entityDTO) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.debug("Receive device event: {}", JsonUtil.toJsonString(entityDTO));
            if (Objects.isNull(entityDTO)
                    || Objects.isNull(entityDTO.getType())
                    || CharSequenceUtil.isEmpty(entityDTO.getContent())) {
                log.error("Invalid device event: {}", entityDTO);
                return;
            }

            switch (entityDTO.getType()) {
                // Save device heartbeat to Redis
                case HEARTBEAT:
                    deviceEventService.heartbeatEvent(entityDTO);
                    break;
                case ALARM:
                    break;
                default:
                    log.error("Invalid event type, {}", entityDTO.getType());
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
