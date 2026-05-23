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
 * RabbitMQ receiver for point command messages that have been rejected into the
 * dead letter exchange. Marks the corresponding command row as {@code dead}.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandDeadReceiver {

    private final PointCommandHistoryManager pointCommandHistoryManager;

    @RabbitHandler
    @RabbitListener(queues = "#{pointCommandDeadQueue.name}")
    public void onDeadLetter(Channel channel, Message message) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String correlationId = message.getMessageProperties().getCorrelationId();
            if (Objects.nonNull(correlationId)) {
                PointCommandHistoryDO commandDO = pointCommandHistoryManager.lambdaQuery()
                        .eq(PointCommandHistoryDO::getCommandId, correlationId)
                        .one();
                if (Objects.nonNull(commandDO)) {
                    commandDO.setStatus(PointCommandStatusEnum.DEAD.getCode());
                    commandDO.setErrorCode("DLX");
                    commandDO.setErrorMessage("Message rejected to dead letter queue");
                    commandDO.setFinishTime(LocalDateTime.now());
                    pointCommandHistoryManager.updateById(commandDO);
                    log.info("Marked dead command: commandId={}", correlationId);
                }
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Dead letter processing failed", e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
