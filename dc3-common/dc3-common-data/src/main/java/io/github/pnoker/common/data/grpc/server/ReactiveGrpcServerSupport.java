package io.github.pnoker.common.data.grpc.server;

import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class ReactiveGrpcServerSupport {

    private ReactiveGrpcServerSupport() {
        throw new IllegalStateException("utility class");
    }

    static <T> void subscribe(Mono<T> publisher, StreamObserver<T> observer) {
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        if (observer instanceof ServerCallStreamObserver<?> serverObserver) {
            serverObserver.setOnCancelHandler(() -> dispose(subscription));
        }
        Disposable disposable = publisher.subscribe(observer::onNext,
                error -> observer.onError(toStatus(error)), observer::onCompleted);
        subscription.set(disposable);
        if (observer instanceof ServerCallStreamObserver<?> serverObserver && serverObserver.isCancelled()) {
            dispose(subscription);
        }
    }

    private static void dispose(AtomicReference<Disposable> subscription) {
        Disposable disposable = subscription.get();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private static RuntimeException toStatus(Throwable error) {
        String description = Objects.requireNonNullElse(error.getMessage(), error.getClass().getSimpleName());
        Status status;
        if (error instanceof NotFoundException) {
            status = Status.NOT_FOUND;
        } else if (error instanceof DuplicateException) {
            status = Status.ALREADY_EXISTS;
        } else if (error instanceof AssociatedException) {
            status = Status.FAILED_PRECONDITION;
        } else if (error instanceof RequestException || error instanceof IllegalArgumentException) {
            status = Status.INVALID_ARGUMENT;
        } else if (error instanceof BusinessException) {
            status = Status.FAILED_PRECONDITION;
        } else {
            status = Status.INTERNAL;
        }
        return status.withDescription(description).withCause(error).asRuntimeException();
    }
}
