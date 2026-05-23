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
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RabbitMQ receiver for custom command messages rejected into the dead letter exchange.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandDeadReceiver {

    private final CommandHistoryManager commandHistoryManager;

    @RabbitHandler
    @RabbitListener(queues = "#{commandDeadQueue.name}")
    public void onDeadLetter(Channel channel, Message message) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String correlationId = message.getMessageProperties().getCorrelationId();
            if (Objects.nonNull(correlationId)) {
                CommandHistoryDO recordDO = commandHistoryManager.lambdaQuery()
                        .eq(CommandHistoryDO::getRecordId, correlationId)
                        .one();
                if (Objects.nonNull(recordDO)) {
                    recordDO.setStatus(PointCommandStatusEnum.DEAD.getCode());
                    recordDO.setErrorCode("DLX");
                    recordDO.setErrorMessage("Message rejected to dead letter queue");
                    recordDO.setFinishTime(LocalDateTime.now());
                    commandHistoryManager.updateById(recordDO);
                    log.info("Marked dead command record: recordId={}", correlationId);
                }
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Command dead letter processing failed", e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
