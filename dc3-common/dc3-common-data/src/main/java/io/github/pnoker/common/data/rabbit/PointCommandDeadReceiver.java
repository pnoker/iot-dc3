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
import io.github.pnoker.common.data.dal.PointCommandHistoryManager;
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
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
 * RabbitMQ receiver for point command messages that have been rejected into the
 * dead letter exchange. Marks the corresponding command row as {@code dead}.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandDeadReceiver {

    private final PointCommandHistoryManager pointCommandHistoryManager;

    /**
     * Consume a point command dead-letter message and mark the matching point command
     * history record as dead, using the message correlation id as the command id.
     *
     * @param message the dead-letter delivery carrying the correlation id header
     * @param ack     the acknowledgement handle
     */
    @Dc3Listener(topic = MqTopic.POINT_COMMAND_DEAD)
    public void onDeadLetter(MqReceived<Object> message, Acknowledgment ack) {
        try {
            String correlationId = message.headers().get(MqHeaders.CORRELATION_ID);
            if (Objects.nonNull(correlationId)) {
                PointCommandHistoryDO commandDO = pointCommandHistoryManager.lambdaQuery()
                        .eq(PointCommandHistoryDO::getCommandId, correlationId)
                        .one();
                if (Objects.nonNull(commandDO)) {
                    commandDO.setStatus(PointCommandStatusEnum.DEAD);
                    commandDO.setErrorCode("DLX");
                    commandDO.setErrorMessage("Message rejected to dead letter queue");
                    commandDO.setFinishTime(LocalDateTime.now());
                    pointCommandHistoryManager.updateById(commandDO);
                    log.info("Marked dead command: commandId={}", correlationId);
                }
            }
            ack.ack();
        } catch (Exception e) {
            log.error("Dead letter processing failed", e);
            ack.reject(true);
        }
    }

}
