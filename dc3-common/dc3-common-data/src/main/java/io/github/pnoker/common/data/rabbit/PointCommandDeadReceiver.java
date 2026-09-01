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
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    private final ReactivePointCommandStore pointCommandStore;

    /**
     * Consume a point command dead-letter message and mark the matching point command
     * history record as dead, using the message correlation id as the command id.
     *
     * @param message the dead-letter delivery carrying the correlation id header
     * @param ack     the acknowledgement handle
     */
    @Dc3Listener(topic = MqTopic.POINT_COMMAND_DEAD)
    public Mono<Void> onDeadLetter(MqReceived<Object> message, Acknowledgment ack) {
        String correlationId = message.headers().get(MqHeaders.CORRELATION_ID);
        String tenantHeader = message.headers().get(MqHeaders.TENANT_ID);
        if (correlationId == null || tenantHeader == null) {
            ack.reject(false);
            return Mono.empty();
        }
        try {
            Long tenantId = Long.valueOf(tenantHeader);
            return pointCommandStore.markDead(tenantId, correlationId, "DLX",
                            "Message rejected to dead letter queue", java.time.Instant.now())
                    .doOnNext(updated -> {
                        if (!updated) {
                            ack.reject(false);
                        }
                    })
                    .doOnError(error -> log.error("Dead letter processing failed, commandId={}",
                            correlationId, error))
                    .then();
        } catch (NumberFormatException error) {
            ack.reject(false);
            return Mono.empty();
        }
    }

}
