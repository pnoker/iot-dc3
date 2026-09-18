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
import io.github.pnoker.common.data.repository.ReactiveCommandHistoryStore;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * RabbitMQ receiver for custom command call result receipts sent by drivers.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandResultReceiver {

    private final ReactiveCommandHistoryStore historyStore;

    /**
     * Consume a command execution result and update the matching command history record
     * by record id with its status, error, and result values.
     *
     * @param message   the raw message carrying the delivery tag
     * @param ack       acknowledgment handle for the message
     */
    @Dc3Listener(topic = MqTopic.COMMAND_RESULT)
    public Mono<Void> onResult(MqReceived<CommandCallResultDTO> message, Acknowledgment ack) {
        CommandCallResultDTO resultDTO = message.payload();
        if (Objects.isNull(resultDTO) || Objects.isNull(resultDTO.recordId())) {
            ack.reject(false);
            return Mono.empty();
        }

        Long tenantId = resultDTO.tenantId() != null ? resultDTO.tenantId() : tenantId(message);
        if (tenantId == null || tenantId <= 0 || resultDTO.status() == null) {
            ack.reject(false);
            return Mono.empty();
        }
        log.info("Receive command result: recordId={}, status={}", resultDTO.recordId(), resultDTO.status());
        return historyStore
                .complete(
                        tenantId,
                        resultDTO.recordId(),
                        resultDTO.status(),
                        resultDTO.resultValues() == null
                                ? null
                                : io.github.pnoker.common.utils.JsonUtil.toJsonString(resultDTO.resultValues()),
                        resultDTO.configSnapshot(),
                        resultDTO.errorCode(),
                        resultDTO.errorMessage(),
                        resultDTO.finishedAt())
                .doOnNext(updated -> {
                    if (updated) {
                        log.info(
                                "Updated command record status: recordId={}, status={}",
                                resultDTO.recordId(),
                                resultDTO.status());
                    }
                })
                .doOnError(error ->
                        log.error("Command result persistence failed, recordId={}", resultDTO.recordId(), error))
                .then();
    }

    private Long tenantId(MqReceived<?> message) {
        try {
            String value = message.headers().get(io.github.pnoker.common.mq.MqHeaders.TENANT_ID);
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
