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

import io.github.pnoker.api.center.data.GrpcDriverStatusQuery;
import io.github.pnoker.api.center.data.GrpcFleetSummaryDTO;
import io.github.pnoker.api.center.data.GrpcIdsStatusQuery;
import io.github.pnoker.api.center.data.GrpcProfileStatusQuery;
import io.github.pnoker.api.center.data.GrpcStatusMap;
import io.github.pnoker.api.center.data.GrpcStringMap;
import io.github.pnoker.api.center.data.GrpcSystemHealthDTO;
import io.github.pnoker.api.center.data.GrpcTenantHealthQuery;
import io.github.pnoker.api.center.data.StatusHealthApiGrpc;
import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * gRPC server implementation for status and health queries.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusHealthServer extends StatusHealthApiGrpc.StatusHealthApiImplBase {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final ReactiveEntityStateStore entityStateStore;

    private final SystemHealthService systemHealthService;

    @Override
    public void deviceStatusesByIds(GrpcIdsStatusQuery request, StreamObserver<GrpcStatusMap> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(
                deviceFacade
                        .listByIdsReactive(request.getTenantId(), request.getIdsList())
                        .map(FacadeDeviceBO::getId)
                        .collectList()
                        .flatMap(ids -> entityStateStore
                                .listStateFlags(request.getTenantId(), EntityTypeEnum.DEVICE, ids)
                                .map(flags -> statusCodes(ids, flags)))
                        .map(statuses ->
                                GrpcStatusMap.newBuilder().putAllData(statuses).build()),
                responseObserver);
    }

    @Override
    public void deviceStatusesByProfileId(
            GrpcProfileStatusQuery request, StreamObserver<GrpcStatusMap> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(
                deviceFacade
                        .listByProfileIdReactive(request.getTenantId(), request.getProfileId())
                        .map(FacadeDeviceBO::getId)
                        .collectList()
                        .flatMap(ids -> entityStateStore
                                .listStateFlags(request.getTenantId(), EntityTypeEnum.DEVICE, ids)
                                .map(flags -> statusCodes(ids, flags)))
                        .map(statuses ->
                                GrpcStatusMap.newBuilder().putAllData(statuses).build()),
                responseObserver);
    }

    @Override
    public void driverStatusesByIds(GrpcIdsStatusQuery request, StreamObserver<GrpcStatusMap> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(
                driverFacade
                        .listByIdsReactive(request.getTenantId(), request.getIdsList())
                        .map(FacadeDriverBO::getId)
                        .collectList()
                        .flatMap(ids -> entityStateStore
                                .listStateFlags(request.getTenantId(), EntityTypeEnum.DRIVER, ids)
                                .map(flags -> statusCodes(ids, flags)))
                        .map(statuses ->
                                GrpcStatusMap.newBuilder().putAllData(statuses).build()),
                responseObserver);
    }

    @Override
    public void driverDeviceStatusSummary(
            GrpcDriverStatusQuery request, StreamObserver<GrpcStringMap> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(
                driverFacade
                        .getByIdReactive(request.getTenantId(), request.getDriverId())
                        .flatMap(driver -> deviceFacade
                                .listByDriverIdReactive(request.getTenantId(), request.getDriverId())
                                .map(FacadeDeviceBO::getId)
                                .collectList()
                                .flatMap(ids -> entityStateStore
                                        .listStateFlags(request.getTenantId(), EntityTypeEnum.DEVICE, ids)
                                        .map(flags -> new FacadeDriverDeviceStatusSummaryBO(
                                                request.getDriverId(),
                                                ids.size(),
                                                (int) ids.stream()
                                                        .filter(id -> EntityStatusEnum.ONLINE
                                                                .getIndex()
                                                                .equals(flags.get(id)))
                                                        .count(),
                                                (int) ids.stream()
                                                        .filter(id -> !EntityStatusEnum.ONLINE
                                                                .getIndex()
                                                                .equals(flags.get(id)))
                                                        .count()))))
                        .map(summary -> GrpcStringMap.newBuilder()
                                .putAllData(summary.toMap())
                                .build())
                        .switchIfEmpty(Mono.error(new io.github.pnoker.common.exception.NotFoundException("Driver"))),
                responseObserver);
    }

    @Override
    public void systemHealth(GrpcTenantHealthQuery request, StreamObserver<GrpcSystemHealthDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(
                systemHealthService
                        .snapshot(request.getTenantId())
                        .switchIfEmpty(Mono.error(new IllegalStateException("system health returned no value")))
                        .map(health -> {
                            GrpcSystemHealthDTO dto = GrpcSystemHealthDTO.newBuilder()
                                    .putAllCenter(nullToEmpty(health.getCenter()))
                                    .putAllInfra(nullToEmpty(health.getInfra()))
                                    .setDrivers(toGrpcSummary(health.getDrivers()))
                                    .setDevices(toGrpcSummary(health.getDevices()))
                                    .build();
                            return dto;
                        }),
                responseObserver);
    }

    private Map<String, String> nullToEmpty(Map<String, String> source) {
        return Objects.nonNull(source) ? source : Map.of();
    }

    private GrpcFleetSummaryDTO toGrpcSummary(SystemHealthVO.FleetSummary summary) {
        if (Objects.isNull(summary)) {
            return GrpcFleetSummaryDTO.newBuilder().build();
        }
        return GrpcFleetSummaryDTO.newBuilder()
                .setTotal(summary.getTotal())
                .setOnline(summary.getOnline())
                .build();
    }

    private Map<Long, String> statusCodes(Collection<Long> ids, Map<Long, Byte> flags) {
        Map<Long, String> statuses = new LinkedHashMap<>();
        ids.forEach(id -> {
            EntityStatusEnum status = EntityStatusEnum.ofIndex(flags.get(id));
            statuses.put(id, status == null ? EntityStatusEnum.OFFLINE.getCode() : status.getCode());
        });
        return statuses;
    }

    private <T> void complete(StreamObserver<T> observer, T response) {
        observer.onNext(response);
        observer.onCompleted();
    }
}
