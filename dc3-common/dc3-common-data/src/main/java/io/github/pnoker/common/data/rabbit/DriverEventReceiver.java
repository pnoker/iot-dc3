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
import io.github.pnoker.common.data.biz.DriverEventService;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Driver Event : , , ,
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverEventReceiver {

    private final DriverEventService driverEventService;

    public DriverEventReceiver(DriverEventService driverEventService) {
        this.driverEventService = driverEventService;
    }

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEventDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.debug("Receive driver event: {}", JsonUtil.toJsonString(entityDTO));
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getType())
                    || StringUtils.isEmpty(entityDTO.getContent())) {
                log.error("Invalid driver event: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            switch (entityDTO.getType()) {
                case HEARTBEAT:
                    driverEventService.heartbeatEvent(entityDTO);
                    break;
                case ALARM:
                    driverEventService.alarmEvent(entityDTO);
                    break;
                default:
                    log.error("Invalid event type, {}", entityDTO.getType());
                    RabbitAckUtil.reject(channel, deliveryTag);
                    return;
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Driver event consume failed, type={}, deliveryTag={}, routingKey={}",
                    Objects.nonNull(entityDTO) ? entityDTO.getType() : null, deliveryTag,
                    message.getMessageProperties().getReceivedRoutingKey(), e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
