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
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ receiver for point value messages that have been TTL-expired and
 * dead-lettered. Logs and acknowledges the expired message to alert operators
 * of potential data loss.
 *
 * @author pnoker
 * @version 2026.6.5
 * @since 2026.6.5
 */
@Slf4j
@Component
public class PointValueDeadReceiver {

    /**
     * Consume a point value dead-letter message (TTL-expired) and log it for diagnostics;
     * no record is updated, the message is simply acknowledged.
     *
     * @param channel the RabbitMQ channel for manual ack
     * @param message the dead-letter message
     */
    @RabbitHandler
    @RabbitListener(queues = "#{pointValueDeadQueue.name}")
    public void onDeadPointValue(Channel channel, Message message) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.warn("Point value TTL expired and was dead-lettered. Headers: {}",
                    message.getMessageProperties().getHeaders());
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Failed to ack dead-lettered point value", e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
