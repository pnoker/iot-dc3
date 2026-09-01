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

import io.github.pnoker.api.center.auth.GrpcLoginNameQuery;
import io.github.pnoker.api.center.auth.GrpcLocalCredentialDTO;
import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcLocalCredentialBuilder;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * gRPC server handling local credential facade requests.
 *
 * @author pnoker
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCredentialServer extends LocalCredentialApiGrpc.LocalCredentialApiImplBase {

    private final GrpcLocalCredentialBuilder grpcLocalCredentialBuilder;

    private final ReactiveLocalCredentialService reactiveLocalCredentialService;

    @Override
    public void getByLoginName(GrpcLoginNameQuery request, StreamObserver<GrpcLocalCredentialDTO> responseObserver) {
        subscribe(reactiveLocalCredentialService.getByLoginName(request.getTenantId(), request.getLoginName()),
                responseObserver);
    }

    private void subscribe(Mono<LocalCredentialBO> publisher,
                           StreamObserver<GrpcLocalCredentialDTO> responseObserver) {
        Context context = Context.current();
        AtomicBoolean terminated = new AtomicBoolean();
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        Disposable disposable = publisher
                .switchIfEmpty(Mono.error(io.grpc.Status.NOT_FOUND.withDescription("Local credential not found")
                        .asRuntimeException()))
                .map(grpcLocalCredentialBuilder::buildGrpcDTOByBO)
                .subscribe(value -> {
            if (context.isCancelled() || !terminated.compareAndSet(false, true)) return;
            responseObserver.onNext(value);
            responseObserver.onCompleted();
        }, error -> {
            if (context.isCancelled() || !terminated.compareAndSet(false, true)) return;
            log.warn("getByLoginName failed", error);
            responseObserver.onError(error instanceof io.grpc.StatusRuntimeException
                    ? error
                    : io.grpc.Status.INTERNAL.withDescription("getByLoginName failed").withCause(error)
                    .asRuntimeException());
        });
        subscription.set(disposable);
        context.addListener(ignored -> {
            Disposable current = subscription.get();
            if (current != null) current.dispose();
        }, MoreExecutors.directExecutor());
        if (context.isCancelled()) disposable.dispose();
    }

}
