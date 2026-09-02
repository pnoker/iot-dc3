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
package io.github.pnoker.common.data.grpc.server;

import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.BusinessException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

final class ReactiveGrpcServerSupport {

    private ReactiveGrpcServerSupport() {
        throw new IllegalStateException("utility class");
    }

    static <T> void subscribe(Mono<T> publisher, StreamObserver<T> observer) {
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        if (observer instanceof ServerCallStreamObserver<?> serverObserver) {
            serverObserver.setOnCancelHandler(() -> dispose(subscription));
        }
        Disposable disposable = publisher.subscribe(
                observer::onNext, error -> observer.onError(toStatus(error)), observer::onCompleted);
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
        String description =
                Objects.requireNonNullElse(error.getMessage(), error.getClass().getSimpleName());
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
