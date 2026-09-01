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

import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcTenantBuilder;
import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

/**
 * gRPC server handling tenant facade requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServer extends TenantApiGrpc.TenantApiImplBase {

    private final GrpcTenantBuilder grpcTenantBuilder;

    private final ReactiveTenantService tenantService;

    @Override
    public void getByCode(GrpcCodeQuery request, StreamObserver<GrpcTenantDTO> responseObserver) {
        Context grpcContext = Context.current();
        Disposable subscription = tenantService.getByCode(request.getCode())
                .switchIfEmpty(reactor.core.publisher.Mono.error(
                        io.grpc.Status.NOT_FOUND.withDescription("Tenant not found").asRuntimeException()))
                .subscribe(entityBO -> {
                    if (grpcContext.isCancelled()) return;
                    GrpcTenantDTO response = grpcTenantBuilder.buildGrpcDTOByBO(entityBO);
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, error -> {
                    if (grpcContext.isCancelled()) return;
                    log.warn("getByCode failed", error);
                    responseObserver.onError(error instanceof io.grpc.StatusRuntimeException
                            ? error
                            : io.grpc.Status.INTERNAL.withDescription("getByCode failed").withCause(error)
                            .asRuntimeException());
                });
        grpcContext.addListener(context -> subscription.dispose(), Runnable::run);
    }

}
