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
package io.github.pnoker.common.driver.grpc.client;

import io.github.pnoker.common.exception.ServiceException;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

final class ReactiveGrpcClientSupport {

    private ReactiveGrpcClientSupport() {
        throw new IllegalStateException("utility class");
    }

    static <Q, S> Mono<S> unary(String operation, Consumer<ClientResponseObserver<Q, S>> invocation) {
        return Mono.create(sink -> {
            AtomicReference<ClientCallStreamObserver<Q>> call = new AtomicReference<>();
            ClientResponseObserver<Q, S> observer = new ClientResponseObserver<>() {
                @Override
                public void beforeStart(ClientCallStreamObserver<Q> requestStream) {
                    call.set(requestStream);
                    sink.onCancel(() -> requestStream.cancel(operation + " cancelled", null));
                }

                @Override
                public void onNext(S response) {
                    sink.success(response);
                }

                @Override
                public void onError(Throwable error) {
                    Status status = Status.fromThrowable(error);
                    String description = Objects.requireNonNullElse(status.getDescription(), error.getMessage());
                    sink.error(new ServiceException(
                            operation + " transport failed: [" + status.getCode() + "] " + description, error));
                }

                @Override
                public void onCompleted() {
                    sink.success();
                }
            };
            try {
                invocation.accept(observer);
            } catch (RuntimeException error) {
                ClientCallStreamObserver<Q> requestStream = call.get();
                if (requestStream != null) {
                    requestStream.cancel(operation + " setup failed", error);
                }
                sink.error(error);
            }
        });
    }

    static <Q, S> Flux<S> stream(String operation, Consumer<ClientResponseObserver<Q, S>> invocation) {
        return Flux.create(
                sink -> {
                    AtomicReference<ClientCallStreamObserver<Q>> call = new AtomicReference<>();
                    ClientResponseObserver<Q, S> observer = new ClientResponseObserver<>() {
                        @Override
                        public void beforeStart(ClientCallStreamObserver<Q> requestStream) {
                            call.set(requestStream);
                            sink.onCancel(() -> requestStream.cancel(operation + " cancelled", null));
                        }

                        @Override
                        public void onNext(S response) {
                            sink.next(response);
                        }

                        @Override
                        public void onError(Throwable error) {
                            Status status = Status.fromThrowable(error);
                            String description =
                                    Objects.requireNonNullElse(status.getDescription(), error.getMessage());
                            sink.error(new ServiceException(
                                    operation + " transport failed: [" + status.getCode() + "] " + description, error));
                        }

                        @Override
                        public void onCompleted() {
                            sink.complete();
                        }
                    };
                    try {
                        invocation.accept(observer);
                    } catch (RuntimeException error) {
                        ClientCallStreamObserver<Q> requestStream = call.get();
                        if (requestStream != null) {
                            requestStream.cancel(operation + " setup failed", error);
                        }
                        sink.error(error);
                    }
                },
                FluxSink.OverflowStrategy.BUFFER);
    }
}
