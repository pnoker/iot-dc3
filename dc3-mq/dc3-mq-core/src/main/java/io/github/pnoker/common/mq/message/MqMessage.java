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
package io.github.pnoker.common.mq.message;

import io.github.pnoker.common.constant.mq.MqTopic;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Outbound message: logical topic + semantic partition key + payload object. The API
 * layer serializes the payload to JSON bytes and stamps the standard headers before the
 * adapter sees it (see {@code WireMqMessage}).
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Getter
@Builder
public class MqMessage {

    private final MqTopic topic;

    /**
     * Semantic key used for routing/partitioning, e.g. {@code "driver.<service>"},
     * {@code "<service>.<node>"} or {@code "task.<channelType>"}. Broker-specific
     * prefix conventions are adapter internals.
     */
    private final String partitionKey;

    private final Object payload;

    /**
     * Extra headers; standard headers ({@code dc3-type}, {@code X-Request-Id}) are
     * stamped by the API layer and cannot be overridden here.
     */
    @Singular
    private final Map<String, String> headers;

    /**
     * Requested delay before delivery; respected natively where the adapter declares
     * {@code capabilities.delayedMessage()}, otherwise falls back to a local scheduler.
     */
    @Builder.Default
    private final Duration delay = Duration.ZERO;

    /**
     * Build a message for a topic with an explicit partition key (ordered streams).
     */
    public static MqMessage of(MqTopic topic, String partitionKey, Object payload) {
        return MqMessage.builder()
                .topic(topic)
                .partitionKey(Objects.requireNonNull(partitionKey, "partitionKey"))
                .payload(Objects.requireNonNull(payload, "payload"))
                .build();
    }
}
