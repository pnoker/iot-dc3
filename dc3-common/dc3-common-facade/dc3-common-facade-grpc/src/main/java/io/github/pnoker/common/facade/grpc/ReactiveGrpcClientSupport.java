package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.common.exception.ServiceException;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

final class ReactiveGrpcClientSupport {

    private ReactiveGrpcClientSupport() {
        throw new IllegalStateException("utility class");
    }

    static <Q, S> Mono<S> unary(String operation, Consumer<ClientResponseObserver<Q, S>> invocation) {
        return Mono.create(sink -> {
            AtomicReference<ClientCallStreamObserver<Q>> call = new AtomicReference<>();
            AtomicBoolean cancelled = new AtomicBoolean();
            sink.onCancel(() -> {
                cancelled.set(true);
                ClientCallStreamObserver<Q> requestStream = call.get();
                if (requestStream != null) requestStream.cancel(operation + " cancelled", null);
            });
            ClientResponseObserver<Q, S> observer = new ClientResponseObserver<>() {
                @Override
                public void beforeStart(ClientCallStreamObserver<Q> requestStream) {
                    call.set(requestStream);
                    if (cancelled.get()) requestStream.cancel(operation + " cancelled", null);
                }

                @Override
                public void onNext(S response) {
                    if (!cancelled.get()) sink.success(response);
                }

                @Override
                public void onError(Throwable error) {
                    if (cancelled.get()) return;
                    Status status = Status.fromThrowable(error);
                    if (status.getCode() == Status.Code.NOT_FOUND) {
                        sink.success();
                        return;
                    }
                    String description = Objects.requireNonNullElse(status.getDescription(), error.getMessage());
                    sink.error(new ServiceException(operation + " transport failed: [" + status.getCode() + "] " + description,
                            error));
                }

                @Override
                public void onCompleted() {
                    if (!cancelled.get()) sink.success();
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
        return Flux.create(sink -> {
            AtomicReference<ClientCallStreamObserver<Q>> call = new AtomicReference<>();
            AtomicBoolean cancelled = new AtomicBoolean();
            sink.onCancel(() -> {
                cancelled.set(true);
                ClientCallStreamObserver<Q> requestStream = call.get();
                if (requestStream != null) requestStream.cancel(operation + " cancelled", null);
            });
            ClientResponseObserver<Q, S> observer = new ClientResponseObserver<>() {
                @Override public void beforeStart(ClientCallStreamObserver<Q> requestStream) {
                    call.set(requestStream);
                    if (cancelled.get()) requestStream.cancel(operation + " cancelled", null);
                }
                @Override public void onNext(S response) { if (!cancelled.get()) sink.next(response); }
                @Override public void onError(Throwable error) {
                    if (cancelled.get()) return;
                    Status status = Status.fromThrowable(error);
                    if (status.getCode() == Status.Code.NOT_FOUND) { sink.complete(); return; }
                    String description = Objects.requireNonNullElse(status.getDescription(), error.getMessage());
                    sink.error(new ServiceException(operation + " transport failed: [" + status.getCode() + "] " + description, error));
                }
                @Override public void onCompleted() { if (!cancelled.get()) sink.complete(); }
            };
            try { invocation.accept(observer); }
            catch (RuntimeException error) {
                ClientCallStreamObserver<Q> requestStream = call.get();
                if (requestStream != null) requestStream.cancel(operation + " setup failed", error);
                sink.error(error);
            }
        });
    }
}
