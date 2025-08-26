/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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
