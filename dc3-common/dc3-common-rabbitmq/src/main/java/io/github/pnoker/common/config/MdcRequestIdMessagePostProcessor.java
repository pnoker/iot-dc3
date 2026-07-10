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

package io.github.pnoker.common.config;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * Stamps the current request id (from the MDC) onto every outbound RabbitMQ message's
 * {@code X-Request-Id} header, so the consumer can restore it via
 * {@link MdcRequestIdListenerAdvice} and keep the traceId chain continuous across the broker
 * hop.
 *
 * <p>When MDC has no request id (a publish not driven by an HTTP request — driver
 * registration, Quartz job, …), nothing is stamped and the consumer mints its own id.
 *
 * <p>Extracted as a named class (rather than a lambda in {@link RabbitConfig}) so it can be
 * unit-tested directly without reaching into the {@code RabbitTemplate} via reflection.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
public class MdcRequestIdMessagePostProcessor implements MessagePostProcessor {

    /**
     * MDC key — kept in sync with {@code RequestIdWebFilter.MDC_REQUEST_ID}.
     */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * AMQP header carrying the request id, mirroring the HTTP/gRPC {@code X-Request-Id}.
     */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public Message postProcessMessage(Message message) {
        String requestId = MDC.get(MDC_REQUEST_ID);
        if (requestId != null && !requestId.isBlank()) {
            message.getMessageProperties().setHeader(HEADER_REQUEST_ID, requestId);
        }
        return message;
    }
}
