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

package io.github.pnoker.common.driver.receiver.rabbit;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.entity.dto.DriverCommandDTO;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ consumer for driver-level commands.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverCommandReceiver {

    /**
     * Receive and process driver commands from RabbitMQ queue
     *
     * @param channel          RabbitMQ channel
     * @param message          RabbitMQ message
     * @param driverCommandDTO Driver command data transfer object
     */
    @RabbitHandler
    @RabbitListener(queues = "#{driverCommandQueue.name}")
    public void driverCommandReceive(Channel channel, Message message, DriverCommandDTO driverCommandDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // Log the received driver command
            log.info("driver command: {}", driverCommandDTO);
            if (Objects.isNull(driverCommandDTO)) {
                log.error("Invalid driver command: {}", driverCommandDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            // Log any errors that occur during message processing
            log.error(e.getMessage(), e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
