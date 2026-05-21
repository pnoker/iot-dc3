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

import lombok.RequiredArgsConstructor;
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ receiver for driver alarm events.
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverAlarmReceiver {

    private final DriverAlarmService driverAlarmService;

    @RabbitHandler
    @RabbitListener(queues = "#{driverAlarmQueue.name}")
    public void driverAlarmReceive(Channel channel, Message message, DriverAlarmDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.debug("Receive driver alarm: {}", JsonUtil.toJsonString(entityDTO));
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId())) {
                log.error("Invalid driver alarm: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            driverAlarmService.alarm(entityDTO);
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Driver alarm consume failed, deliveryTag={}", deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
