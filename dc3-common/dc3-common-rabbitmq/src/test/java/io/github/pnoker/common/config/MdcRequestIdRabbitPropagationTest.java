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

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies the request id (traceId) propagation across the RabbitMQ hop:
 * <ul>
 *   <li>the listener {@link MdcRequestIdListenerAdvice} restores the id from the
 *       {@code X-Request-Id} message header into the MDC for the handling duration, then
 *       clears it (also on exception);</li>
 *   <li>when the header is absent, a fresh UUID is minted so the consumer is still
 *       self-consistent;</li>
 *   <li>the {@link MdcRequestIdMessagePostProcessor} stamps the current MDC {@code requestId}
 *       onto every outbound message, and stamps nothing when MDC is empty.</li>
 * </ul>
 *
 * <p>Together with the HTTP filter and gRPC interceptors, this closes the
 * HTTP&nbsp;→&nbsp;gRPC&nbsp;→&nbsp;RabbitMQ traceId chain.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
class MdcRequestIdRabbitPropagationTest {

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @AfterEach
    void clearMdc() {
        MDC.remove(MDC_REQUEST_ID);
    }

    private MethodInvocation invocationWith(Message message) throws Throwable {
        MethodInvocation invocation = Mockito.mock(MethodInvocation.class);
        Method onMessage = Object.class.getMethod("toString");
        when(invocation.getMethod()).thenReturn(onMessage);
        when(invocation.getArguments()).thenReturn(new Object[]{message});
        when(invocation.proceed()).thenReturn(null);
        return invocation;
    }

    private Message messageWithHeader(String headerValue) {
        MessageProperties properties = new MessageProperties();
        if (headerValue != null) {
            properties.setHeader(HEADER_REQUEST_ID, headerValue);
        }
        return new Message(new byte[0], properties);
    }

    // ---------------- consumer side (advice) ----------------

    @Test
    void adviceRestoresRequestIdFromHeaderDuringHandling() throws Throwable {
        // The producer stamped "req-abc" onto the message. During handling the MDC must
        // expose that exact id so the consumer's log.* calls are correlated with the
        // originating HTTP request.
        Message message = messageWithHeader("req-abc");
        String[] capturedDuringHandling = new String[1];
        MethodInvocation invocation = invocationWith(message);
        when(invocation.proceed()).thenAnswer(inv -> {
            capturedDuringHandling[0] = MDC.get(MDC_REQUEST_ID);
            return null;
        });
        new MdcRequestIdListenerAdvice().invoke(invocation);
        assertThat(capturedDuringHandling[0]).isEqualTo("req-abc");
    }

    @Test
    void adviceClearsMdcAfterHandlingEvenOnSuccess() throws Throwable {
        Message message = messageWithHeader("req-abc");
        new MdcRequestIdListenerAdvice().invoke(invocationWith(message));
        // The pooled listener thread must not leak the id into the next message.
        assertThat((Object) MDC.get(MDC_REQUEST_ID)).isNull();
    }

    @Test
    void adviceClearsMdcAfterHandlingEvenOnException() throws Throwable {
        Message message = messageWithHeader("req-abc");
        MethodInvocation invocation = invocationWith(message);
        when(invocation.proceed()).thenThrow(new IllegalStateException("listener blew up"));
        try {
            new MdcRequestIdListenerAdvice().invoke(invocation);
        } catch (IllegalStateException expected) {
            // expected — the advice must propagate the original exception unchanged
        }
        assertThat((Object) MDC.get(MDC_REQUEST_ID)).isNull();
    }

    @Test
    void adviceMintsUuidWhenHeaderAbsent() throws Throwable {
        // A message published outside any HTTP request (driver registration, Quartz job)
        // carries no header. The consumer must still be traceable within itself.
        Message message = messageWithHeader(null);
        String[] capturedDuringHandling = new String[1];
        MethodInvocation invocation = invocationWith(message);
        when(invocation.proceed()).thenAnswer(inv -> {
            capturedDuringHandling[0] = MDC.get(MDC_REQUEST_ID);
            return null;
        });
        new MdcRequestIdListenerAdvice().invoke(invocation);
        assertThat(capturedDuringHandling[0]).isNotBlank();
    }

    // ---------------- producer side (post-processor) ----------------

    @Test
    void postProcessorStampsCurrentRequestIdOnOutboundMessage() {
        // A producer running inside an HTTP request (or a consumer that already restored the
        // id) has the id in its MDC. The post-processor must copy it onto the message header.
        MDC.put(MDC_REQUEST_ID, "req-from-producer");
        try {
            Message outbound = new MdcRequestIdMessagePostProcessor()
                    .postProcessMessage(messageWithHeader(null));
            assertThat((String) outbound.getMessageProperties().getHeader(HEADER_REQUEST_ID))
                    .isEqualTo("req-from-producer");
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    @Test
    void postProcessorLeavesHeaderAbsentWhenMdcEmpty() {
        // When MDC has no request id, nothing should be stamped — the consumer mints its own.
        assertThat((Object) MDC.get(MDC_REQUEST_ID)).isNull();
        Message outbound = new MdcRequestIdMessagePostProcessor()
                .postProcessMessage(messageWithHeader(null));
        assertThat((Object) outbound.getMessageProperties().getHeader(HEADER_REQUEST_ID)).isNull();
    }
}
