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

import cn.hutool.core.text.CharSequenceUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 接收设备指令
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DeviceCommandReceiver {

    @Resource
    private DriverReadService driverReadService;
    @Resource
    private DriverWriteService driverWriteService;

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
        try {
            // Acknowledge message receipt
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.info("Receive device command: {}", JsonUtil.toJsonString(entityDTO));

            // Validate command data
            if (Objects.isNull(entityDTO)
                    || Objects.isNull(entityDTO.getType())
                    || CharSequenceUtil.isEmpty(entityDTO.getContent())) {
                log.error("Invalid device command: {}", entityDTO);
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
                case CONFIG:
                    // to do something
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
