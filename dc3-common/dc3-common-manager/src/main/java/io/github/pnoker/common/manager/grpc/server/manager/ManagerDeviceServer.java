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
package io.github.pnoker.common.manager.grpc.server.manager;

import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDeviceListDTO;
import io.github.pnoker.api.center.manager.GrpcDeviceOwnerDTO;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.biz.DriverLeaseService;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive gRPC server handling manager-to-manager device requests. */
@Service
public class ManagerDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {

    private final GrpcDeviceBuilder grpcDeviceBuilder;
    private final ReactiveDeviceService reactiveDeviceService;
    private final DriverLeaseService driverLeaseService;

    @Autowired
    public ManagerDeviceServer(
            GrpcDeviceBuilder grpcDeviceBuilder,
            ReactiveDeviceService reactiveDeviceService,
            DriverLeaseService driverLeaseService) {
        this.grpcDeviceBuilder = grpcDeviceBuilder;
        this.reactiveDeviceService = reactiveDeviceService;
        this.driverLeaseService = driverLeaseService;
    }

    @Override
    public void list(GrpcOffsetDeviceQuery request, StreamObserver<GrpcOffsetPageDeviceDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                reactiveDeviceService.list(offsetFilter(request)).map(page -> offsetResponse(page)), observer);
    }

    @Override
    public void getActiveOwner(GrpcDeviceQuery request, StreamObserver<GrpcDeviceOwnerDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                driverLeaseService
                        .getActiveOwner(request.getTenantId(), request.getDeviceId())
                        .map(owner -> GrpcDeviceOwnerDTO.newBuilder()
                                .setDriverId(owner.driverId())
                                .setOwnerNode(owner.ownerNode())
                                .setFencingToken(owner.fencingToken())
                                .build())
                        .switchIfEmpty(Mono.error(new NotFoundException("device owner does not exist"))),
                observer);
    }

    @Override
    public void listByDriverId(GrpcDriverQuery request, StreamObserver<GrpcDeviceListDTO> observer) {
        listResponse(reactiveDeviceService.listByDriverId(request.getTenantId(), request.getDriverId()), observer);
    }

    @Override
    public void listByProfileId(GrpcProfileQuery request, StreamObserver<GrpcDeviceListDTO> observer) {
        listResponse(reactiveDeviceService.listByProfileId(request.getTenantId(), request.getProfileId()), observer);
    }

    @Override
    public void listByDeviceIds(GrpcDeviceIdsQuery request, StreamObserver<GrpcDeviceListDTO> observer) {
        if (request.getDeviceIdsList().stream().anyMatch(id -> id <= 0)) {
            ReactiveGrpcServerSupport.subscribe(
                    Mono.error(new IllegalArgumentException("device ids must be positive")), observer);
            return;
        }
        List<Long> ids = request.getDeviceIdsList().stream().distinct().toList();
        listResponse(reactiveDeviceService.listByIds(request.getTenantId(), ids), observer);
    }

    @Override
    public void getByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcDeviceDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                reactiveDeviceService
                        .getById(request.getTenantId(), request.getDeviceId())
                        .map(grpcDeviceBuilder::buildGrpcDTOByBO)
                        .switchIfEmpty(Mono.error(new NotFoundException("device does not exist"))),
                observer);
    }

    private DeviceFilter offsetFilter(GrpcOffsetDeviceQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new DeviceFilter(
                request.getTenantId(),
                request.getDeviceName(),
                request.getDeviceCode(),
                request.hasDriverId() ? request.getDriverId() : null,
                request.hasProfileId() ? request.getProfileId() : null,
                request.hasEnableFlag()
                        ? io.github.pnoker.common.enums.EnableFlagEnum.ofIndex((byte) request.getEnableFlag())
                        : null,
                request.hasVersion() ? request.getVersion() : null,
                request.hasGroupId() ? request.getGroupId() : null,
                request.hasLabelId() ? request.getLabelId() : null,
                page.offset(),
                page.limit(),
                page.sort());
    }

    private GrpcOffsetPageDeviceDTO offsetResponse(OffsetPage<DeviceBO> page) {
        return GrpcOffsetPageDeviceDTO.newBuilder()
                .setPage(io.github.pnoker.api.common.OffsetPage.newBuilder()
                        .setOffset(page.offset())
                        .setLimit(page.limit())
                        .setTotal(page.total())
                        .setHasNext(page.hasNext()))
                .addAllItems(page.items().stream()
                        .map(grpcDeviceBuilder::buildGrpcDTOByBO)
                        .toList())
                .build();
    }

    private void listResponse(Flux<DeviceBO> values, StreamObserver<GrpcDeviceListDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(
                values.map(grpcDeviceBuilder::buildGrpcDTOByBO)
                        .collectList()
                        .map(devices -> GrpcDeviceListDTO.newBuilder()
                                .addAllItems(devices)
                                .build()),
                observer);
    }
}
