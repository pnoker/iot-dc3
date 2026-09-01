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

package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.auth.GrpcPermissionQuery;
import io.github.pnoker.api.center.auth.GrpcPermissionCodesDTO;
import io.github.pnoker.api.center.auth.PermissionApiGrpc;
import io.github.pnoker.common.facade.api.PermissionFacade;
import io.github.pnoker.common.facade.grpc.config.GrpcFacadeProperties;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Reactive gRPC {@link PermissionFacade}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionGrpcFacade implements PermissionFacade {

    private final PermissionApiGrpc.PermissionApiStub permissionApiStub;

    private final GrpcFacadeProperties properties;

    @Override
    public Mono<Set<String>> listPermissionCodes(Long tenantId, Long principalId) {
        if (tenantId == null || principalId == null) {
            return Mono.just(Set.of());
        }
        GrpcPermissionQuery request = GrpcPermissionQuery.newBuilder()
                .setTenantId(tenantId)
                .setPrincipalId(principalId)
                .build();
        return Mono.create(sink -> deadlineStub().listPermissionCodes(request, new StreamObserver<>() {
            @Override
            public void onNext(GrpcPermissionCodesDTO response) {
                sink.success(response.getCodesList().stream()
                        .filter(code -> code != null && !code.isBlank())
                        .collect(Collectors.toUnmodifiableSet()));
            }

            @Override
            public void onError(Throwable error) {
                sink.error(error);
            }

            @Override
            public void onCompleted() {
                // Unary response is completed from onNext; duplicate completion is ignored by MonoSink.
            }
        }));
    }

    private PermissionApiGrpc.PermissionApiStub deadlineStub() {
        return properties.getDeadlineMs() > 0
                ? permissionApiStub.withDeadlineAfter(properties.getDeadlineMs(), TimeUnit.MILLISECONDS)
                : permissionApiStub;
    }
}
