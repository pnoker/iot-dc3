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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

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
 * <p><b>Why Reactor Context, not MDC:</b> This is a WebFlux application. Controllers run their
 * blocking suppliers on {@code Schedulers.boundedElastic()} (see {@code BaseController.async()}),
 * which is a different thread than the Netty event loop this filter runs on. MDC is
 * {@link ThreadLocal}-backed and does not cross that hop, so the id is published via the Reactor
 * {@link Context} instead — it propagates along the call chain regardless of thread switches.
 * {@code BaseController.async()} reads it back from the {@link ContextView} and sets MDC on the
 * worker thread, where the business {@code log.*} calls actually execute.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.7
 */
@AutoConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {

    /**
     * MDC key under which the request id is published on the worker thread (matches the
     * logback pattern placeholder). Shared with {@code BaseController.async()}.
     */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * Reactor Context key under which the request id travels from this filter to
     * {@code BaseController.async()} across the boundedElastic thread hop.
     */
    public static final String CONTEXT_REQUEST_ID = "dc3.requestId";

    /**
     * Header name for the request id, read on the way in and written on the way out.
     */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String requestId = headers.getFirst(HEADER_REQUEST_ID);
        
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
        exchange.getResponse().getHeaders().add(HEADER_REQUEST_ID, finalRequestId);
        // Publish the id through the Reactor Context (not just MDC): the Context propagates
        // along the reactive call chain regardless of thread hops, so BaseController.async()
        // — which switches to Schedulers.boundedElastic() — can read it and set MDC on the
        // worker thread where business log.* calls actually execute. MDC alone would be lost
        // the moment the supplier hops off the Netty event loop.
        return chain.filter(exchange).contextWrite(ctx -> ctx.put(CONTEXT_REQUEST_ID, finalRequestId));
    }
}
