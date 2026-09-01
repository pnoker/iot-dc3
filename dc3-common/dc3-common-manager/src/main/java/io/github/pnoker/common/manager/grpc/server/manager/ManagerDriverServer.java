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

import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcDriverIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDriverListDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.api.center.manager.GrpcOffsetDriverQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageDriverDTO;
import io.github.pnoker.api.common.OffsetPage;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;


/**
 * gRPC server handling manager driver facade requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Service
public class ManagerDriverServer extends DriverApiGrpc.DriverApiImplBase {

    private final GrpcDriverBuilder grpcDriverBuilder;

    private final ReactiveDriverService reactiveDriverService;

    @Autowired
    public ManagerDriverServer(GrpcDriverBuilder builder, ReactiveDriverService service) {
        this.grpcDriverBuilder = builder;
        this.reactiveDriverService = service;
    }

    @Override
    public void list(GrpcOffsetDriverQuery request, StreamObserver<GrpcOffsetPageDriverDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(Mono.fromSupplier(() -> filter(request)).flatMap(reactiveDriverService::list)
                .map(page -> GrpcOffsetPageDriverDTO.newBuilder().setPage(OffsetPage.newBuilder().setOffset(page.offset()).setLimit(page.limit()).setTotal(page.total()).setHasNext(page.hasNext()))
                        .addAllItems(page.items().stream().map(grpcDriverBuilder::buildGrpcDTOByBO).toList()).build()), observer);
    }

    private DriverFilter filter(GrpcOffsetDriverQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new DriverFilter(request.getTenantId(), request.getDriverName(), request.getDriverCode(), request.getServiceName(), request.getServiceHost(),
                request.hasDriverTypeFlag() ? io.github.pnoker.common.enums.DriverTypeEnum.ofIndex((byte) request.getDriverTypeFlag()) : null,
                request.hasEnableFlag() ? io.github.pnoker.common.enums.EnableFlagEnum.ofIndex((byte) request.getEnableFlag()) : null,
                request.hasVersion() ? request.getVersion() : null, request.hasGroupId() ? request.getGroupId() : null,
                request.hasLabelId() ? request.getLabelId() : null, page.offset(), page.limit(), page.sort());
    }

    @Override
    public void getByDeviceId(GrpcDeviceQuery request, StreamObserver<io.github.pnoker.api.common.GrpcDriverDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactiveDriverService.getByDeviceId(request.getTenantId(), request.getDeviceId())
                .map(grpcDriverBuilder::buildGrpcDTOByBO)
                .switchIfEmpty(Mono.error(new NotFoundException("driver does not exist"))), responseObserver);
    }

    @Override
    public void listByDriverIds(GrpcDriverIdsQuery request, StreamObserver<GrpcDriverListDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactiveDriverService.listByIds(request.getTenantId(), request.getDriverIdsList())
                .map(grpcDriverBuilder::buildGrpcDTOByBO).collectList()
                .map(values -> GrpcDriverListDTO.newBuilder().addAllItems(values).build()), responseObserver);
    }

    @Override
    public void getByDriverId(GrpcDriverQuery request, StreamObserver<io.github.pnoker.api.common.GrpcDriverDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactiveDriverService.getById(request.getTenantId(), request.getDriverId())
                .map(grpcDriverBuilder::buildGrpcDTOByBO)
                .switchIfEmpty(Mono.error(new NotFoundException("driver does not exist"))), responseObserver);
    }

}
