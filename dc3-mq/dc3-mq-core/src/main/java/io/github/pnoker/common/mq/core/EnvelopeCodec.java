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
package io.github.pnoker.common.mq.core;

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.utils.JsonUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Serializes business messages into the broker-neutral wire envelope: JSON body plus
 * standardized string headers. Serialization lives here — never in adapters and never
 * on the broker — so the wire format is identical across brokers.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
public final class EnvelopeCodec {

    private EnvelopeCodec() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Build the wire envelope for an outbound message: JSON body, {@code dc3-type}
     * header from the payload class, {@code X-Request-Id} from the MDC (with the
     * OpenTelemetry trace id as fallback when no request-scoped id is present).
     *
     * @param message the business message
     * @return the wire envelope
     */
    public static WireMqMessage prepare(MqMessage message) {
        Object payload = Objects.requireNonNull(message.getPayload(), "payload");
        Map<String, String> headers = new HashMap<>();
        if (Objects.nonNull(message.getHeaders())) {
            headers.putAll(message.getHeaders());
        }
        headers.put(MqHeaders.DC3_TYPE, payload.getClass().getName());
        String requestId = currentRequestId();
        if (Objects.nonNull(requestId)) {
            headers.put(MqHeaders.REQUEST_ID, requestId);
        }
        byte[] body = JsonUtil.toJsonString(payload).getBytes(StandardCharsets.UTF_8);
        return new WireMqMessage(
                message.getTopic(),
                message.getPartitionKey(),
                body,
                headers,
                Objects.isNull(message.getDelay()) ? java.time.Duration.ZERO : message.getDelay());
    }

    /**
     * Deserialize a raw delivery into the declared payload type. The {@code dc3-type}
     * header is informational; the subscription's declared type wins, which also keeps
     * pre-migration messages (carrying only the legacy type header) consumable.
     *
     * @param delivery    the raw delivery
     * @param payloadType the declared payload type
     * @param <T>         payload type
     * @return the deserialized payload
     */
    public static <T> T deserialize(WireMqDelivery delivery, Class<T> payloadType) {
        String json = new String(delivery.body(), StandardCharsets.UTF_8);
        return JsonUtil.parseObject(json, payloadType);
    }

    private static String currentRequestId() {
        String requestId = MDC.get(RequestIdConstant.MDC_KEY);
        if (Objects.isNull(requestId) || requestId.isBlank()) {
            SpanContext spanContext = Span.current().getSpanContext();
            if (spanContext.isValid()) {
                requestId = spanContext.getTraceId();
            }
        }
        return Objects.nonNull(requestId) && !requestId.isBlank() ? requestId : null;
    }
}
