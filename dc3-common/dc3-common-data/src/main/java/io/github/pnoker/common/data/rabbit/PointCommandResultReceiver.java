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
import io.github.pnoker.common.data.repository.ReactivePointCommandStore;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * RabbitMQ receiver for point command result receipts sent by drivers.
 * Updates the matching {@code dc3_point_command_history} row with the terminal status.
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandResultReceiver {

    private final ReactivePointCommandStore pointCommandStore;

    /**
     * Consume a point command execution result and update the matching point command
     * history record by command id with its status, error, and response value.
     *
     * @param channel   the RabbitMQ channel for manual ack
     * @param message   the raw message carrying the delivery tag
     * @param resultDTO the deserialized point command result
     */
    @Dc3Listener(topic = MqTopic.POINT_COMMAND_RESULT)
    public Mono<Void> onResult(MqReceived<PointCommandResultDTO> message, Acknowledgment ack) {
        PointCommandResultDTO resultDTO = message.payload();
        if (Objects.isNull(resultDTO) || Objects.isNull(resultDTO.commandId()) || resultDTO.commandId().isBlank()
                || resultDTO.tenantId() == null || resultDTO.status() == null) {
            ack.reject(false);
            return Mono.empty();
        }
        return pointCommandStore.complete(resultDTO.tenantId(), resultDTO.commandId(), resultDTO.status(),
                        resultDTO.responseValue(), resultDTO.errorCode(), resultDTO.errorMessage(), resultDTO.finishedAt())
                .doOnNext(updated -> {
                    if (!updated) {
                        ack.reject(false);
                    }
                })
                .doOnError(error -> log.error("Point command result processing failed, commandId={}",
                        resultDTO.commandId(), error))
                .then();
    }

}
