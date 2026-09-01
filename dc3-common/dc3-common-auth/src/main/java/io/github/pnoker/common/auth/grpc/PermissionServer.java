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

import io.github.pnoker.api.center.auth.GrpcPermissionQuery;
import io.github.pnoker.api.center.auth.GrpcPermissionCodesDTO;
import io.github.pnoker.api.center.auth.PermissionApiGrpc;
import io.github.pnoker.common.auth.repository.ReactivePermissionStore;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * gRPC server handling permission-code lookup requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServer extends PermissionApiGrpc.PermissionApiImplBase {

    private final ReactivePermissionStore permissionStore;

    @Override
    public void listPermissionCodes(GrpcPermissionQuery request,
                                    StreamObserver<GrpcPermissionCodesDTO> responseObserver) {
        permissionStore.listResourceCodes(request.getTenantId(), request.getPrincipalId())
                .collectList()
                .subscribe(codes -> {
                    GrpcPermissionCodesDTO.Builder builder = GrpcPermissionCodesDTO.newBuilder();
                    codes.forEach(builder::addCodes);
                    responseObserver.onNext(builder.build());
                    responseObserver.onCompleted();
                }, error -> {
                    log.warn("listPermissionCodes failed, tenant={}, principal={}",
                            request.getTenantId(), request.getPrincipalId(), error);
                    responseObserver.onError(io.grpc.Status.INTERNAL
                            .withDescription("permission lookup failed")
                            .withCause(error).asRuntimeException());
                });
    }

}
