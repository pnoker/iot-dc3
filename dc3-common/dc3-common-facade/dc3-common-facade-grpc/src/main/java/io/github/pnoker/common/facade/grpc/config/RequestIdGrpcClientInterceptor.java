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

package io.github.pnoker.common.facade.grpc.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.stereotype.Component;

/**
 * Propagates the current request id from the caller's MDC into the gRPC metadata, so the
 * downstream service can pick it up via {@link RequestIdGrpcServerInterceptor} and keep
 * the same id in its log lines. This closes the traceId gap across the
 * HTTP&nbsp;→&nbsp;Gateway&nbsp;→&nbsp;gRPC&nbsp;→&nbsp;center-services hop.
 *
 * <p><b>Production-grade OpenTelemetry Integration:</b> This interceptor now integrates
 * with OpenTelemetry. It will use:
 * <ol>
 *   <li>MDC requestId (from existing RequestId mechanism)</li>
 *   <li>OpenTelemetry Trace ID (if available and no MDC value)</li>
 * </ol>
 * This ensures full compatibility with both systems while maintaining backward compatibility.
 *
 * <p>The header name mirrors the HTTP {@code X-Request-Id} convention. When no id is available
 * (e.g. a gRPC call triggered outside any HTTP request, such as a Quartz job or a driver
 * registration), nothing is attached and the call proceeds unchanged.
 *
 * <p>Registered as a {@code @GlobalClientInterceptor} bean so spring-grpc applies it to every
 * client channel automatically.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@Component
@GlobalClientInterceptor
public class RequestIdGrpcClientInterceptor implements ClientInterceptor {

    /**
     * MDC key — kept in sync with {@code RequestIdWebFilter.MDC_REQUEST_ID} (same name, same
     * placeholder in the logback pattern).
     */
    private static final String MDC_REQUEST_ID = "requestId";

    /**
     * gRPC metadata key for the request id. ASCII, matching the HTTP header name.
     */
    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of("X-Request-Id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        String requestId = MDC.get(MDC_REQUEST_ID);
        
        // If MDC doesn't have requestId, try to get it from OpenTelemetry
        if (requestId == null || requestId.isBlank()) {
            Span currentSpan = Span.current();
            SpanContext spanContext = currentSpan.getSpanContext();
            if (spanContext.isValid()) {
                requestId = spanContext.getTraceId();
            }
        }
        
        if (requestId == null || requestId.isBlank()) {
            return next.newCall(method, callOptions);
        }
        
        final String finalRequestId = requestId;
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(REQUEST_ID_KEY, finalRequestId);
                super.start(responseListener, headers);
            }
        };
    }
}
