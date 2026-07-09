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

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.MDC;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Companion to {@link RequestIdGrpcClientInterceptor}: reads the request id from the
 * inbound gRPC metadata and publishes it into the MDC for the duration of the call, so
 * every log line emitted by the service implementation carries the same id as the
 * originating HTTP request.
 *
 * <p>When the caller did not send an {@code X-Request-Id} (e.g. a driver registering itself,
 * or any gRPC call not initiated from an HTTP request), a fresh UUID is generated so the
 * request is still traceable within this service.
 *
 * <p>MDC is set on {@code onHalfClose} (right before the service method runs) and cleared on
 * completion or cancellation, to avoid leaking the entry across pooled netty threads. It is
 * re-applied on every callback because gRPC callbacks may run on different threads.
 *
 * <p>Registered as a {@code @GlobalServerInterceptor} bean so spring-grpc applies it to
 * every gRPC server automatically.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@Component
@GlobalServerInterceptor
public class RequestIdGrpcServerInterceptor implements ServerInterceptor {

    /**
     * MDC key — kept in sync with {@code RequestIdWebFilter.MDC_REQUEST_ID}.
     */
    private static final String MDC_REQUEST_ID = "requestId";

    /**
     * gRPC metadata key for the request id, mirroring the HTTP header name.
     */
    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of("X-Request-Id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        // Read the id supplied by the caller (set by RequestIdGrpcClientInterceptor upstream),
        // or mint one when absent so the call is still self-consistent in this service's logs.
        String requestId = headers.get(REQUEST_ID_KEY);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return new RequestIdListener<>(next.startCall(call, headers), requestId);
    }

    /**
     * Forwards every listener callback while keeping the request id in the MDC. The service
     * method runs during {@code onHalfClose}, but other callbacks (onReady, onCancel, ...)
     * may fire on a different thread, so the MDC entry is re-applied defensively before each
     * forwarding and cleared on terminal callbacks.
     */
    private static final class RequestIdListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final String requestId;

        RequestIdListener(ServerCall.Listener<ReqT> delegate, String requestId) {
            super(delegate);
            this.requestId = requestId;
        }

        private void withRequestId(Runnable action) {
            MDC.put(MDC_REQUEST_ID, requestId);
            try {
                action.run();
            } finally {
                MDC.remove(MDC_REQUEST_ID);
            }
        }

        @Override
        public void onMessage(ReqT message) {
            withRequestId(() -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            // This is where the actual service method executes.
            withRequestId(super::onHalfClose);
        }

        @Override
        public void onCancel() {
            withRequestId(super::onCancel);
        }

        @Override
        public void onComplete() {
            withRequestId(super::onComplete);
        }

        @Override
        public void onReady() {
            withRequestId(super::onReady);
        }
    }
}
