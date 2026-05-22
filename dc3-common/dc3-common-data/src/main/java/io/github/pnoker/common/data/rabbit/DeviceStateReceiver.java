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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.DeviceStateService;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ receiver for device state events.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStateReceiver {

    private final DeviceStateService deviceStateService;

    @RabbitHandler
    @RabbitListener(queues = "#{deviceStateQueue.name}")
    public void deviceStateReceive(Channel channel, Message message, DeviceStateDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.debug("Receive device state: {}", JsonUtil.toJsonString(entityDTO));
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDeviceId())
                    || Objects.isNull(entityDTO.getDriverId()) || Objects.isNull(entityDTO.getTenantId())
                    || Objects.isNull(entityDTO.getStatus()) || Objects.isNull(entityDTO.getTimeUnit())
                    || entityDTO.getTimeOut() <= 0) {
                log.error("Invalid device state: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            deviceStateService.heartbeat(entityDTO);
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Device state consume failed, deliveryTag={}", deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
