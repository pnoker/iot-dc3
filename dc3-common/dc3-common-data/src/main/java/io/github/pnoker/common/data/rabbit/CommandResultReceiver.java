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
import io.github.pnoker.common.data.dal.CommandHistoryManager;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.utils.JsonUtil;
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
 * RabbitMQ receiver for custom command call result receipts sent by drivers.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandResultReceiver {

    private final CommandHistoryManager commandHistoryManager;

    @RabbitHandler
    @RabbitListener(queues = "#{commandResultQueue.name}")
    public void onResult(Channel channel, Message message, CommandCallResultDTO resultDTO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (Objects.isNull(resultDTO) || Objects.isNull(resultDTO.recordId())) {
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }

            log.info("Receive command result: recordId={}, status={}", resultDTO.recordId(), resultDTO.status());

            CommandHistoryDO recordDO = commandHistoryManager.lambdaQuery()
                    .eq(CommandHistoryDO::getRecordId, resultDTO.recordId())
                    .one();

            if (Objects.nonNull(recordDO)) {
                recordDO.setStatus(resultDTO.status());
                recordDO.setErrorCode(resultDTO.errorCode());
                recordDO.setErrorMessage(resultDTO.errorMessage());
                if (Objects.nonNull(resultDTO.resultValues())) {
                    recordDO.setResultValues(JsonUtil.toJsonString(resultDTO.resultValues()));
                }
                if (Objects.nonNull(resultDTO.finishedAt())) {
                    recordDO.setFinishTime(LocalDateTime.ofInstant(resultDTO.finishedAt(), ZoneId.systemDefault()));
                } else {
                    recordDO.setFinishTime(LocalDateTime.now());
                }
                commandHistoryManager.updateById(recordDO);
                log.info("Updated command record status: recordId={}, status={}", resultDTO.recordId(), resultDTO.status());
            } else {
                log.warn("Command record not found for result: recordId={}", resultDTO.recordId());
            }

            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Command result processing failed, deliveryTag={}", deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
