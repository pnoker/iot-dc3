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

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.data.dal.CommandHistoryManager;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RabbitMQ receiver for custom command messages rejected into the dead letter exchange.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandDeadReceiver {

    private final CommandHistoryManager commandHistoryManager;

    /**
     * Consume a command dead-letter message and mark the matching command history record
     * as dead, using the message correlation id as the record id.
     *
     * @param message the dead-letter delivery carrying the correlation id header
     * @param ack     the acknowledgement handle
     */
    @Dc3Listener(topic = MqTopic.COMMAND_DEAD)
    public void onDeadLetter(MqReceived<Object> message, Acknowledgment ack) {
        try {
            String correlationId = message.headers().get(MqHeaders.CORRELATION_ID);
            if (Objects.nonNull(correlationId)) {
                CommandHistoryDO recordDO = commandHistoryManager.lambdaQuery()
                        .eq(CommandHistoryDO::getRecordId, correlationId)
                        .one();
                if (Objects.nonNull(recordDO)) {
                    recordDO.setStatus(PointCommandStatusEnum.DEAD);
                    recordDO.setErrorCode("DLX");
                    recordDO.setErrorMessage("Message rejected to dead letter queue");
                    recordDO.setFinishTime(LocalDateTime.now());
                    commandHistoryManager.updateById(recordDO);
                    log.info("Marked dead command record: recordId={}", correlationId);
                }
            }
            ack.ack();
        } catch (Exception e) {
            log.error("Command dead letter processing failed", e);
            ack.reject(true);
        }
    }

}
