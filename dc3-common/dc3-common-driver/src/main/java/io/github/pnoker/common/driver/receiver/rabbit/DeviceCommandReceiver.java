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
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ consumer that dispatches device read and write commands to the corresponding
 * services.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandReceiver {

    private final DriverReadService driverReadService;

    private final DriverWriteService driverWriteService;

    /**
     * Receive and process device commands from RabbitMQ queue
     *
     * @param channel   RabbitMQ channel for message acknowledgment
     * @param message   Raw RabbitMQ message containing delivery information
     * @param entityDTO Device command data transfer object containing command details
     */
    @RabbitHandler
    @RabbitListener(queues = "#{deviceCommandQueue.name}")
    public void deviceCommandReceive(Channel channel, Message message, DeviceCommandDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean redelivered = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        try {
            log.info("Receive device command: {}", JsonUtil.toJsonString(entityDTO));

            // Validate command data
            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getType())
                    || StringUtils.isEmpty(entityDTO.getContent())) {
                log.error("Invalid device command: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            // Process command based on type
            switch (entityDTO.getType()) {
                case READ:
                    // Execute read operation
                    driverReadService.read(entityDTO);
                    break;
                case WRITE:
                    // Execute write operation
                    driverWriteService.write(entityDTO);
                    break;
                default:
                    log.error("Unsupported device command type: {}", entityDTO.getType());
                    RabbitAckUtil.reject(channel, deliveryTag);
                    return;
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            // Bounded retry instead of unconditional requeue: a first-time failure may be
            // a transient device hiccup, so let RabbitMQ redeliver once. If we fail
            // again on redelivery we treat the message as poison and reject it without
            // requeue — without a DLQ in place this drops the command, but it stops a
            // single bad payload from pinning the consumer in an infinite loop.
            if (redelivered) {
                log.error("Device command failed on redelivery, dropping. type={}, deliveryTag={}",
                        Objects.nonNull(entityDTO) ? entityDTO.getType() : null, deliveryTag, e);
                RabbitAckUtil.reject(channel, deliveryTag);
            } else {
                log.warn("Device command failed, requeueing for one retry. type={}, deliveryTag={}, error={}",
                        Objects.nonNull(entityDTO) ? entityDTO.getType() : null, deliveryTag, e.getMessage(), e);
                RabbitAckUtil.nack(channel, deliveryTag, true);
            }
        }
    }

}
