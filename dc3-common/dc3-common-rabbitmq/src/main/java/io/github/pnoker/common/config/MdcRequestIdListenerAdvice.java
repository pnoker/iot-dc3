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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.UUID;

/**
 * Restores the request id into the MDC for the duration of a RabbitMQ message handling, so the
 * traceId chain is continuous across the HTTP&nbsp;→&nbsp;gRPC&nbsp;→&nbsp;RabbitMQ hops.
 *
 * <p>The id is carried in the {@value #HEADER_REQUEST_ID} message header, stamped on the
 * producer side by the {@code beforePublish} post-processor in {@link RabbitConfig}. On the
 * consumer side, the listener container runs on a pooled thread; this advice reads the header
 * before the listener runs, publishes it into the MDC, and removes it afterwards — guaranteeing
 * the entry never leaks to the next message processed on the same pooled thread.
 *
 * <p>When the header is absent (e.g. a driver registration, a Quartz-triggered publish, or a
 * producer that predates this wiring), a fresh UUID is minted so the consumer's logs are still
 * self-consistent within that single message's handling.
 *
 * <p>Registered on every listener container factory via {@code setAdviceChain(...)}, so all
 * {@code @RabbitListener} methods are covered without touching individual receivers.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
public class MdcRequestIdListenerAdvice implements MethodInterceptor {

    /**
     * MDC key — kept in sync with {@code RequestIdWebFilter.MDC_REQUEST_ID} (same placeholder in
     * the logback pattern).
     */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * AMQP message header carrying the request id, mirroring the HTTP/gRPC {@code X-Request-Id}.
     */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = extractMessage(invocation.getArguments());
        String requestId = readRequestId(message);
        MDC.put(MDC_REQUEST_ID, requestId);
        try {
            return invocation.proceed();
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    /**
     * Pull the {@link Message} argument out of the listener invocation. Spring AMQP listeners
     * are invoked either as {@code onMessage(Message)} or
     * {@code onMessage(Message, Channel)}; both expose the message as the first argument.
     */
    private Message extractMessage(Object[] arguments) {
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument instanceof Message message) {
                    return message;
                }
            }
        }
        return null;
    }

    /**
     * Read the request id from the message header, falling back to a fresh UUID so the consumer
     * is always traceable within itself even when the producer did not stamp one.
     */
    private String readRequestId(Message message) {
        if (message != null) {
            MessageProperties properties = message.getMessageProperties();
            if (properties != null) {
                String header = properties.getHeader(HEADER_REQUEST_ID);
                if (header != null && !header.isBlank()) {
                    return header;
                }
            }
        }
        return UUID.randomUUID().toString();
    }
}
