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

import io.github.pnoker.api.center.data.GrpcDriverStatusQuery;
import io.github.pnoker.api.center.data.GrpcIdsStatusQuery;
import io.github.pnoker.api.center.data.GrpcProfileStatusQuery;
import io.github.pnoker.api.center.data.GrpcStatusMap;
import io.github.pnoker.api.center.data.GrpcSystemHealthDTO;
import io.github.pnoker.api.center.data.GrpcTenantHealthQuery;
import io.github.pnoker.api.center.data.StatusHealthApiGrpc;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * gRPC StatusHealthFacade: forwards to Data Center via {@link StatusHealthApiGrpc}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusHealthGrpcFacade implements StatusHealthFacade {

    private final StatusHealthApiGrpc.StatusHealthApiStub statusHealthApiStub;

    @Override
    public Mono<Map<Long, String>> listDeviceStatusesByIdsReactive(Long tenantId, Collection<Long> deviceIds) {
        return unaryReactive(idsQuery(tenantId, deviceIds), statusHealthApiStub::deviceStatusesByIds)
                .map(GrpcStatusMap::getDataMap);
    }

    @Override
    public Mono<Map<Long, String>> listDeviceStatusesByProfileIdReactive(Long tenantId, Long profileId) {
        GrpcProfileStatusQuery request = GrpcProfileStatusQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L))
                .setProfileId(Objects.requireNonNullElse(profileId, 0L))
                .build();
        return unaryReactive(request, statusHealthApiStub::deviceStatusesByProfileId)
                .map(GrpcStatusMap::getDataMap);
    }

    @Override
    public Mono<Map<Long, String>> listDriverStatusesByIdsReactive(Long tenantId, Collection<Long> driverIds) {
        return unaryReactive(idsQuery(tenantId, driverIds), statusHealthApiStub::driverStatusesByIds)
                .map(GrpcStatusMap::getDataMap);
    }

    @Override
    public Mono<FacadeDriverDeviceStatusSummaryBO> getDriverDeviceStatusSummaryReactive(Long tenantId, Long driverId) {
        GrpcDriverStatusQuery request = GrpcDriverStatusQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L))
                .setDriverId(Objects.requireNonNullElse(driverId, 0L))
                .build();
        return unaryReactive(request, statusHealthApiStub::driverDeviceStatusSummary)
                .map(response -> FacadeDriverDeviceStatusSummaryBO.fromMap(response.getDataMap()))
                .onErrorResume(error -> io.grpc.Status.fromThrowable(error).getCode() == io.grpc.Status.Code.NOT_FOUND
                        ? Mono.empty()
                        : Mono.error(error));
    }

    @Override
    public Mono<FacadeSystemHealthBO> systemHealthReactive(Long tenantId) {
        GrpcTenantHealthQuery request = GrpcTenantHealthQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L))
                .build();
        return unaryReactive(request, statusHealthApiStub::systemHealth).map(this::toFacadeHealth);
    }

    private <Request, Response> Mono<Response> unaryReactive(
            Request request, java.util.function.BiConsumer<Request, StreamObserver<Response>> invocation) {
        return Mono.create(sink -> {
            java.util.concurrent.atomic.AtomicReference<io.grpc.stub.ClientCallStreamObserver<Request>> callRef =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
            ClientResponseObserver<Request, Response> observer = new ClientResponseObserver<>() {
                @Override
                public void beforeStart(io.grpc.stub.ClientCallStreamObserver<Request> call) {
                    callRef.set(call);
                    if (cancelled.get()) {
                        call.cancel("reactor subscriber cancelled", null);
                    }
                }

                @Override
                public void onNext(Response response) {
                    sink.success(response);
                }

                @Override
                public void onError(Throwable error) {
                    sink.error(error);
                }

                @Override
                public void onCompleted() {}
            };
            sink.onCancel(() -> {
                cancelled.set(true);
                io.grpc.stub.ClientCallStreamObserver<Request> call = callRef.get();
                if (call != null) {
                    call.cancel("reactor subscriber cancelled", null);
                }
            });
            try {
                invocation.accept(request, observer);
            } catch (Throwable error) {
                sink.error(error);
            }
        });
    }

    private GrpcIdsStatusQuery idsQuery(Long tenantId, Collection<Long> ids) {
        GrpcIdsStatusQuery.Builder builder =
                GrpcIdsStatusQuery.newBuilder().setTenantId(Objects.requireNonNullElse(tenantId, 0L));
        if (Objects.nonNull(ids)) {
            builder.addAllIds(ids.stream().filter(Objects::nonNull).distinct().toList());
        }
        return builder.build();
    }

    private FacadeSystemHealthBO toFacadeHealth(GrpcSystemHealthDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }
        FacadeSystemHealthBO health = new FacadeSystemHealthBO();
        health.setCenter(dto.getCenterMap());
        health.setInfra(dto.getInfraMap());
        health.setDrivers(new FacadeSystemHealthBO.FleetSummary(
                dto.getDrivers().getTotal(), dto.getDrivers().getOnline()));
        health.setDevices(new FacadeSystemHealthBO.FleetSummary(
                dto.getDevices().getTotal(), dto.getDevices().getOnline()));
        return health;
    }
}
