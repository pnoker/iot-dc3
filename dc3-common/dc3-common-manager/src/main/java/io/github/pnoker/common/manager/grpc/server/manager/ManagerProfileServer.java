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

import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetProfileQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcProfileIdsQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcProfileListDTO;
import io.github.pnoker.api.center.manager.ProfileApiGrpc;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.grpc.builder.GrpcProfileBuilder;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Profile gRPC API.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Service
public class ManagerProfileServer extends ProfileApiGrpc.ProfileApiImplBase {

    private final GrpcProfileBuilder grpcProfileBuilder;

    private final ReactiveProfileService reactiveProfileService;

    @Autowired
    public ManagerProfileServer(GrpcProfileBuilder builder, ReactiveProfileService service) {
        this.grpcProfileBuilder = builder;
        this.reactiveProfileService = service;
    }

    @Override
    public void list(GrpcOffsetProfileQuery request, StreamObserver<GrpcOffsetPageProfileDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(Mono.fromSupplier(() -> filter(request)).flatMap(reactiveProfileService::list)
                .map(page -> GrpcOffsetPageProfileDTO.newBuilder()
                        .setPage(io.github.pnoker.api.common.OffsetPage.newBuilder().setOffset(page.offset()).setLimit(page.limit()).setTotal(page.total()).setHasNext(page.hasNext()))
                        .addAllItems(page.items().stream().map(grpcProfileBuilder::buildGrpcDTOByBO).toList()).build()), observer);
    }

    private ProfileFilter filter(GrpcOffsetProfileQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new ProfileFilter(request.getTenantId(), request.getProfileName(), request.getProfileCode(),
                request.hasProfileShareFlag() ? io.github.pnoker.common.enums.ProfileShareTypeEnum.ofIndex((byte) request.getProfileShareFlag()) : null,
                request.hasProfileTypeFlag() ? io.github.pnoker.common.enums.ProfileTypeEnum.ofIndex((byte) request.getProfileTypeFlag()) : null,
                request.hasEnableFlag() ? io.github.pnoker.common.enums.EnableFlagEnum.ofIndex((byte) request.getEnableFlag()) : null,
                request.hasGroupId() ? request.getGroupId() : null, request.hasLabelId() ? request.getLabelId() : null,
                request.hasVersion() ? request.getVersion() : null, request.hasDeviceId() ? request.getDeviceId() : null,
                page.offset(), page.limit(), page.sort());
    }

    @Override
    public void getByProfileId(GrpcProfileQuery request, StreamObserver<io.github.pnoker.api.common.GrpcProfileDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactiveProfileService.getById(request.getTenantId(), request.getProfileId())
                .map(grpcProfileBuilder::buildGrpcDTOByBO)
                .switchIfEmpty(Mono.error(new NotFoundException("profile does not exist"))), responseObserver);
    }

    @Override
    public void listByProfileIds(GrpcProfileIdsQuery request,
                                 StreamObserver<GrpcProfileListDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactiveProfileService.listByIds(request.getTenantId(), request.getProfileIdsList())
                .map(grpcProfileBuilder::buildGrpcDTOByBO).collectList()
                .map(values -> GrpcProfileListDTO.newBuilder().addAllItems(values).build()), responseObserver);
    }

    @Override
    public void listByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcProfileListDTO> responseObserver) {
        ReactiveGrpcServerSupport.subscribe(reactiveProfileService.listByDeviceId(request.getTenantId(), request.getDeviceId())
                .map(grpcProfileBuilder::buildGrpcDTOByBO).collectList()
                .map(values -> GrpcProfileListDTO.newBuilder().addAllItems(values).build()), responseObserver);
    }

}
