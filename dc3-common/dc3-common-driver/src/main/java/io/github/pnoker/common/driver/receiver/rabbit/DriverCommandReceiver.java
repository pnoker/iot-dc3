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

package io.github.pnoker.common.driver.receiver.rabbit;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.entity.dto.DriverCommandDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 接收驱动指令
 *
 * @author pnoker
 * @version 2025.6.1
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverCommandReceiver {

    /**
     * Receive and process driver commands from RabbitMQ queue
     *
     * @param channel           RabbitMQ channel
     * @param message          RabbitMQ message
     * @param driverCommandDTO Driver command data transfer object
     */
    @RabbitHandler
    @RabbitListener(queues = "#{driverCommandQueue.name}")
    public void driverCommandReceive(Channel channel, Message message, DriverCommandDTO driverCommandDTO) {
        try {
            // Acknowledge the message receipt to RabbitMQ
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            // Log the received driver command
            log.info("driver command: {}", driverCommandDTO);
        } catch (Exception e) {
            // Log any errors that occur during message processing
            log.error(e.getMessage(), e);
        }
    }

}
