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
package io.github.pnoker.common.manager.grpc.server.driver;

import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.driver.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcOffsetPointQuery;
import io.github.pnoker.api.common.driver.GrpcPointQuery;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.common.manager.grpc.server.manager.ReactiveGrpcServerSupport;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.grpc.stub.StreamObserver;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive gRPC server handling driver-to-manager point requests. */
@Service
public class DriverPointServer extends PointApiGrpc.PointApiImplBase {

    private final GrpcPointBuilder grpcPointBuilder;
    private final ReactivePointService reactivePointService;
    private final ReactiveDriverService reactiveDriverService;
    private final ReactiveDeviceService reactiveDeviceService;

    @Autowired
    public DriverPointServer(
            GrpcPointBuilder grpcPointBuilder,
            ReactivePointService reactivePointService,
            ReactiveDriverService reactiveDriverService,
            ReactiveDeviceService reactiveDeviceService) {
        this.grpcPointBuilder = grpcPointBuilder;
        this.reactivePointService = reactivePointService;
        this.reactiveDriverService = reactiveDriverService;
        this.reactiveDeviceService = reactiveDeviceService;
    }

    @Override
    public void list(GrpcOffsetPointQuery request, StreamObserver<GrpcOffsetPagePointDTO> responseObserver) {
        Mono<GrpcOffsetPagePointDTO> response = Mono.defer(() -> {
            var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
            long offset = page.offset();
            int limit = page.limit();
            return reactiveDriverService
                    .getById(request.getTenantId(), request.getDriverId())
                    .flatMapMany(driver -> resolveProfileIds(request, driver)
                            .flatMapMany(profileIds -> Flux.fromIterable(profileIds)
                                    .flatMap(
                                            profileId -> reactivePointService.listByProfileId(
                                                    request.getTenantId(), profileId),
                                            8))
                            .filter(point -> Objects.equals(point.getTenantId(), driver.getTenantId()))
                            .distinct(PointBO::getId)
                            .sort(Comparator.comparing(PointBO::getId, Comparator.nullsLast(Long::compareTo))))
                    .collectList()
                    .map(points -> {
                        int from = (int) Math.min(offset, points.size());
                        long endExclusive = offset > Long.MAX_VALUE - limit ? Long.MAX_VALUE : offset + limit;
                        long end = Math.min((long) points.size(), endExclusive);
                        int to = (int) Math.max(from, end);
                        List<GrpcPointDTO> items = points.subList(from, to).stream()
                                .map(grpcPointBuilder::buildGrpcDTOByBO)
                                .toList();
                        return GrpcOffsetPagePointDTO.newBuilder()
                                .setPage(io.github.pnoker.api.common.OffsetPage.newBuilder()
                                        .setOffset(offset)
                                        .setLimit(limit)
                                        .setTotal(points.size())
                                        .setHasNext(to < points.size()))
                                .addAllItems(items)
                                .build();
                    })
                    .onErrorResume(
                            NotFoundException.class,
                            ignored -> Mono.error(new NotFoundException("driver does not exist")));
        });
        ReactiveGrpcServerSupport.subscribe(response, responseObserver);
    }

    @Override
    public void getById(GrpcPointQuery request, StreamObserver<GrpcPointDTO> responseObserver) {
        Mono<GrpcPointDTO> response = reactiveDriverService
                .getById(request.getTenantId(), request.getDriverId())
                .flatMap(driver -> reactivePointService
                        .getById(request.getTenantId(), request.getPointId())
                        .filter(point -> Objects.equals(point.getTenantId(), driver.getTenantId()))
                        .flatMap(point -> driverHasPoint(driver, point)
                                .filter(Boolean.TRUE::equals)
                                .map(ignored -> grpcPointBuilder.buildGrpcDTOByBO(point))))
                .switchIfEmpty(Mono.error(new NotFoundException("point does not exist")));
        ReactiveGrpcServerSupport.subscribe(response, responseObserver);
    }

    private Mono<Set<Long>> resolveProfileIds(GrpcOffsetPointQuery request, DriverBO driver) {
        if (request.hasDeviceId()) {
            return reactiveDeviceService
                    .getById(request.getTenantId(), request.getDeviceId())
                    .filter(device -> Objects.equals(device.getDriverId(), driver.getId()))
                    .map(DeviceBO::getProfileId)
                    .filter(Objects::nonNull)
                    .filter(profileId -> !request.hasProfileId() || Objects.equals(profileId, request.getProfileId()))
                    .map(Set::of)
                    .defaultIfEmpty(Set.of());
        }
        return reactiveDeviceService
                .listByDriverId(request.getTenantId(), driver.getId())
                .map(DeviceBO::getProfileId)
                .filter(Objects::nonNull)
                .filter(profileId -> !request.hasProfileId() || Objects.equals(profileId, request.getProfileId()))
                .collect(Collectors.toSet());
    }

    private Mono<Boolean> driverHasPoint(DriverBO driver, PointBO point) {
        return reactiveDeviceService
                .listByDriverId(driver.getTenantId(), driver.getId())
                .map(DeviceBO::getProfileId)
                .filter(Objects::nonNull)
                .any(profileId -> Objects.equals(profileId, point.getProfileId()));
    }
}
