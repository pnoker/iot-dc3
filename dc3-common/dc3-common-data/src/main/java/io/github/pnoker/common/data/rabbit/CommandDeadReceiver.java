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
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    private final ReactiveCommandHistoryStore historyStore;

    /**
     * Consume a command dead-letter message and mark the matching command history record
     * as dead, using the message correlation id as the record id.
     *
     * @param message the dead-letter delivery carrying the correlation id header
     * @param ack     the acknowledgement handle
     */
    @Dc3Listener(topic = MqTopic.COMMAND_DEAD)
    public Mono<Void> onDeadLetter(MqReceived<Object> message, Acknowledgment ack) {
        String correlationId = message.headers().get(MqHeaders.CORRELATION_ID);
        Long tenantId = tenantId(message);
        if (correlationId == null || tenantId == null || tenantId <= 0) {
            ack.reject(false);
            return Mono.empty();
        }
        return historyStore
                .markDead(
                        tenantId,
                        correlationId,
                        "DLX",
                        "Message rejected to dead letter queue",
                        java.time.Instant.now())
                .doOnNext(updated -> {
                    if (updated) {
                        log.info("Marked dead command record: recordId={}", correlationId);
                    }
                })
                .doOnError(
                        error -> log.error("Command dead letter persistence failed, recordId={}", correlationId, error))
                .then();
    }

    private Long tenantId(MqReceived<?> message) {
        try {
            String value = message.headers().get(MqHeaders.TENANT_ID);
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
