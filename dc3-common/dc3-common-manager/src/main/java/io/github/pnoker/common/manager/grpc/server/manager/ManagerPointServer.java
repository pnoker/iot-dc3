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

import io.github.pnoker.api.center.manager.GrpcOffsetPointQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcPointIdsQuery;
import io.github.pnoker.api.center.manager.GrpcPointQuery;
import io.github.pnoker.api.center.manager.GrpcPointListDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.common.manager.repository.PointFilter;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * gRPC server handling manager point facade requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Service
public class ManagerPointServer extends PointApiGrpc.PointApiImplBase {

    private final GrpcPointBuilder grpcPointBuilder;

    private final ReactivePointService reactivePointService;

    @Autowired
    public ManagerPointServer(GrpcPointBuilder grpcPointBuilder, ReactivePointService reactivePointService) {
        this.grpcPointBuilder = grpcPointBuilder;
        this.reactivePointService = reactivePointService;
    }

    @Override
    public void list(GrpcOffsetPointQuery request, StreamObserver<GrpcOffsetPagePointDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(Mono.fromSupplier(() -> filter(request))
                .flatMap(reactivePointService::list)
                .map(page -> GrpcOffsetPagePointDTO.newBuilder()
                        .setPage(io.github.pnoker.api.common.OffsetPage.newBuilder().setOffset(page.offset())
                                .setLimit(page.limit()).setTotal(page.total()).setHasNext(page.hasNext()))
                        .addAllItems(page.items().stream().map(grpcPointBuilder::buildGrpcDTOByBO).toList()).build()), observer);
    }

    private PointFilter filter(GrpcOffsetPointQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new PointFilter(request.getTenantId(), request.getPointName(), request.getPointCode(),
                request.hasPointTypeFlag() ? io.github.pnoker.common.enums.PointTypeEnum.ofIndex((byte) request.getPointTypeFlag()) : null,
                request.hasRwFlag() ? io.github.pnoker.common.enums.RwTypeEnum.ofIndex((byte) request.getRwFlag()) : null,
                request.hasProfileId() ? request.getProfileId() : null,
                request.hasEnableFlag() ? io.github.pnoker.common.enums.EnableFlagEnum.ofIndex((byte) request.getEnableFlag()) : null,
                request.hasGroupId() ? request.getGroupId() : null, request.hasLabelId() ? request.getLabelId() : null,
                request.hasVersion() ? request.getVersion() : null, request.hasDeviceId() ? request.getDeviceId() : null,
                page.offset(), page.limit(), page.sort());
    }

    @Override
    public void listByIds(GrpcPointIdsQuery request, StreamObserver<GrpcPointListDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactivePointService.listByIds(request.getTenantId(), request.getPointIdsList())
                .map(grpcPointBuilder::buildGrpcDTOByBO).collectList()
                .map(values -> GrpcPointListDTO.newBuilder().addAllItems(values).build()), responseObserver);
    }

    @Override
    public void getById(GrpcPointQuery request, StreamObserver<GrpcPointDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactivePointService.getById(request.getTenantId(), request.getPointId())
                .map(grpcPointBuilder::buildGrpcDTOByBO)
                .switchIfEmpty(Mono.error(new NotFoundException("point does not exist"))), responseObserver);
    }

}
