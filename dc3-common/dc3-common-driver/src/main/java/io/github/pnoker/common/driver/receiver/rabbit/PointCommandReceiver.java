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
import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * RabbitMQ consumer that dispatches point read and write commands to the corresponding
 * services. Performs idempotent deduplication and sends result receipts back to the
 * data center.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandReceiver {

    private final DriverReadService driverReadService;
    private final DriverWriteService driverWriteService;
    private final DriverSenderService driverSenderService;
    private final CommandDedupCache dedupCache;

    private static final int SCHEMA_VERSION = 1;

    /**
     * Receive and process point commands from RabbitMQ queue.
     *
     * @param channel   RabbitMQ channel for message acknowledgment
     * @param message   Raw RabbitMQ message containing delivery information
     * @param entityDTO Point command data transfer object containing command details
     */
    @RabbitHandler
    @RabbitListener(queues = "#{pointCommandQueue.name}")
    public void pointCommandReceive(Channel channel, Message message, PointCommandDTO entityDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean redelivered = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        try {
            log.info("Receive point command: {}", JsonUtil.toJsonString(entityDTO));

            if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getType())
                    || StringUtils.isEmpty(entityDTO.getContent())) {
                log.error("Invalid point command: {}", entityDTO);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            String commandId = entityDTO.getCommandId();
            Long tenantId = entityDTO.getTenantId();

            // Dedup check
            if (Objects.nonNull(commandId) && !dedupCache.tryAcquire(commandId)) {
                log.warn("Duplicate command detected: commandId={}", commandId);
                sendResult(commandId, tenantId, PointCommandStatusEnum.DUPLICATE.getCode(),
                        null, "DUPLICATE", "Command already processed", channel, deliveryTag);
                return;
            }

            // Dispatch
            switch (entityDTO.getType()) {
                case READ:
                    driverReadService.read(entityDTO);
                    break;
                case WRITE:
                    driverWriteService.write(entityDTO);
                    break;
                default:
                    log.error("Unsupported point command type: {}", entityDTO.getType());
                    sendResult(commandId, tenantId, PointCommandStatusEnum.FAILED.getCode(),
                            null, "UNSUPPORTED_TYPE", "Unsupported command type: " + entityDTO.getType(),
                            channel, deliveryTag);
                    return;
            }

            // Success
            sendResult(commandId, tenantId, PointCommandStatusEnum.SUCCESS.getCode(),
                    null, null, null, channel, deliveryTag);

        } catch (Exception e) {
            if (redelivered) {
                log.error("Point command failed on redelivery, sending FAILED. deliveryTag={}", deliveryTag, e);
                String commandId = Objects.nonNull(entityDTO) ? entityDTO.getCommandId() : null;
                Long tenantId = Objects.nonNull(entityDTO) ? entityDTO.getTenantId() : null;
                sendResult(commandId, tenantId, PointCommandStatusEnum.FAILED.getCode(),
                        null, "DRIVER_ERROR", e.getMessage(), channel, deliveryTag);
            } else {
                log.warn("Point command failed, requeueing. deliveryTag={}", deliveryTag, e);
                RabbitAckUtil.nack(channel, deliveryTag, true);
            }
        }
    }

    private void sendResult(String commandId, Long tenantId, String status,
                            String responseValue, String errorCode, String errorMessage,
                            Channel channel, long deliveryTag) {
        try {
            if (Objects.nonNull(commandId)) {
                PointCommandResultDTO result = PointCommandResultDTO.builder()
                        .commandId(commandId)
                        .tenantId(tenantId)
                        .status(status)
                        .responseValue(responseValue)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .finishedAt(Instant.now())
                        .schemaVersion(SCHEMA_VERSION)
                        .build();
                driverSenderService.pointCommandResultSender(result);
            }
        } catch (Exception ex) {
            log.error("Failed to send command result, commandId={}", commandId, ex);
        }
        RabbitAckUtil.ack(channel, deliveryTag);
    }

}
