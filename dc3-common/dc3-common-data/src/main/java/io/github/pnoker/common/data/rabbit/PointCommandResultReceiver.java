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
import io.github.pnoker.common.data.dal.PointCommandHistoryManager;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * RabbitMQ receiver for point command result receipts sent by drivers.
 * Updates the matching {@code dc3_point_command_history} row with the terminal status.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandResultReceiver {

    private final PointCommandHistoryManager pointCommandHistoryManager;

    /**
     * Consume a point command execution result and update the matching point command
     * history record by command id with its status, error, and response value.
     *
     * @param channel   the RabbitMQ channel for manual ack
     * @param message   the raw message carrying the delivery tag
     * @param resultDTO the deserialized point command result
     */
    @RabbitHandler
    @RabbitListener(queues = "#{pointCommandResultQueue.name}")
    public void onResult(Channel channel, Message message, PointCommandResultDTO resultDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (Objects.isNull(resultDTO) || Objects.isNull(resultDTO.commandId())) {
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            log.info("Receive point command result: commandId={}, status={}", resultDTO.commandId(), resultDTO.status());

            PointCommandHistoryDO commandDO = pointCommandHistoryManager.lambdaQuery()
                    .eq(PointCommandHistoryDO::getCommandId, resultDTO.commandId())
                    .one();

            if (Objects.nonNull(commandDO)) {
                commandDO.setStatus(resultDTO.status());
                commandDO.setErrorCode(resultDTO.errorCode());
                commandDO.setErrorMessage(resultDTO.errorMessage());
                commandDO.setResponseValue(resultDTO.responseValue());
                if (Objects.nonNull(resultDTO.finishedAt())) {
                    commandDO.setFinishTime(LocalDateTime.ofInstant(resultDTO.finishedAt(), ZoneId.systemDefault()));
                } else {
                    commandDO.setFinishTime(LocalDateTime.now());
                }
                pointCommandHistoryManager.updateById(commandDO);
                log.info("Updated command status: commandId={}, status={}", resultDTO.commandId(), resultDTO.status());
            } else {
                log.warn("Command not found for result: commandId={}", resultDTO.commandId());
            }

            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Point command result processing failed, deliveryTag={}", deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
