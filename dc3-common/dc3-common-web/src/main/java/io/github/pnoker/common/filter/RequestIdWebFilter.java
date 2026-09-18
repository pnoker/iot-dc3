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
package io.github.pnoker.common.filter;

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Propagates a per-request id so every log line within a request carries the same
 * {@code requestId} (rendered by the {@code [%X{requestId:-}]} slot in the logback pattern).
 * <p>
 * <b>Production-grade OpenTelemetry Integration:</b> This filter now integrates with
 * OpenTelemetry. The requestId is set to the OpenTelemetry Trace ID when a trace exists,
 * falling back to a UUID when no trace is present. This ensures:
 * <ul>
 *   <li>Logs and distributed traces use the same identifier</li>
 *   <li>Backward compatibility with X-Request-Id header</li>
 *   <li>Full interoperability with OpenTelemetry ecosystem</li>
 * </ul>
 *
 * <p>The id is taken from (in order of priority):
 * <ol>
 *   <li>The inbound {@code X-Request-Id} header (backward compatibility)</li>
 *   <li>The current OpenTelemetry Trace ID</li>
 *   <li>A fresh UUID as a last resort</li>
 * </ol>
 * The same id is echoed back on the response via {@code X-Request-Id}, so callers can
 * correlate a failing request with server logs and distributed traces.
 *
 * <p><b>Why Reactor Context:</b> Reactor chains may change execution threads. Publishing the id in
 * the Reactor {@link Context} keeps it attached to the subscriber rather than a specific thread,
 * while the response header gives callers a transport-level correlation key.
 *
 * @author pnoker
 * @since 2026.7.7
 */
@AutoConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String requestId = headers.getFirst(RequestIdConstant.HEADER);

        // Priority 1: Use X-Request-Id from header (backward compatibility)
        // Priority 2: Use OpenTelemetry Trace ID if available
        if (requestId == null || requestId.isBlank()) {
            Span currentSpan = Span.current();
            SpanContext spanContext = currentSpan.getSpanContext();
            if (spanContext.isValid()) {
                requestId = spanContext.getTraceId();
            }
        }

        // Priority 3: Fall back to UUID if no trace or header
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String finalRequestId = requestId;
        // Echo back so callers can correlate a failing request with server-side logs and traces.
        exchange.getResponse().getHeaders().add(RequestIdConstant.HEADER, finalRequestId);
        // Publish in the Reactor context so downstream reactive operators retain the
        // correlation id even when execution changes threads.
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(RequestIdConstant.REACTOR_CONTEXT_KEY, finalRequestId));
    }
}
