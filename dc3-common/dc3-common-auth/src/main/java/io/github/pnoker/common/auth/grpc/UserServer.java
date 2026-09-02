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
package io.github.pnoker.common.auth.grpc;

import com.google.common.util.concurrent.MoreExecutors;
import io.github.pnoker.api.center.auth.GrpcIdQuery;
import io.github.pnoker.api.center.auth.GrpcUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcUserBuilder;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * gRPC server handling user facade requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServer extends UserApiGrpc.UserApiImplBase {

    private final GrpcUserBuilder grpcUserBuilder;

    private final ReactiveUserService reactiveUserService;

    @Override
    public void getById(GrpcIdQuery request, StreamObserver<GrpcUserDTO> responseObserver) {
        subscribe(reactiveUserService.getById(request.getTenantId(), request.getId()), "getById", responseObserver);
    }

    @Override
    public void getByPrincipalId(GrpcIdQuery request, StreamObserver<GrpcUserDTO> responseObserver) {
        subscribe(
                reactiveUserService.getByPrincipalId(request.getTenantId(), request.getId()),
                "getByPrincipalId",
                responseObserver);
    }

    private void subscribe(Mono<UserBO> publisher, String operation, StreamObserver<GrpcUserDTO> responseObserver) {
        Context context = Context.current();
        AtomicBoolean terminated = new AtomicBoolean();
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        Disposable disposable = publisher
                .onErrorResume(
                        NotFoundException.class,
                        error -> Mono.error(io.grpc.Status.NOT_FOUND
                                .withDescription("User not found")
                                .asRuntimeException()))
                .map(grpcUserBuilder::buildGrpcDTOByBO)
                .subscribe(
                        value -> {
                            if (context.isCancelled() || !terminated.compareAndSet(false, true)) return;
                            responseObserver.onNext(value);
                            responseObserver.onCompleted();
                        },
                        error -> {
                            if (context.isCancelled() || !terminated.compareAndSet(false, true)) return;
                            log.warn("{} failed", operation, error);
                            responseObserver.onError(
                                    error instanceof io.grpc.StatusRuntimeException
                                            ? error
                                            : io.grpc.Status.INTERNAL
                                                    .withDescription(operation + " failed")
                                                    .withCause(error)
                                                    .asRuntimeException());
                        });
        subscription.set(disposable);
        context.addListener(
                ignored -> {
                    Disposable current = subscription.get();
                    if (current != null) current.dispose();
                },
                MoreExecutors.directExecutor());
        if (context.isCancelled()) disposable.dispose();
    }
}
