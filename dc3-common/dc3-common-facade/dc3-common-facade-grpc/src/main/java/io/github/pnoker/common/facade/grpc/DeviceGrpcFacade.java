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

import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcDeviceListDTO;
import io.github.pnoker.api.center.manager.GrpcDeviceOwnerDTO;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcDeviceBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC implementation: forwards each call to Manager Center via
 * <p>
 * Selected when {@code dc3.facade.manager.mode=grpc} (or unset — grpc is the default in the
 * auto-configuration declaration).
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class DeviceGrpcFacade implements DeviceFacade {


    private final DeviceApiGrpc.DeviceApiStub deviceApiStub;

    private final FacadeGrpcDeviceBuilder facadeGrpcDeviceBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<FacadeDeviceBO> getByIdReactive(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(id).setTenantId(tenantId).build();
        DeviceApiGrpc.DeviceApiStub stub = grpcFacadeSupport.withDeadline(deviceApiStub);
        return ReactiveGrpcClientSupport.<GrpcDeviceQuery, GrpcDeviceDTO>unary(
                        "DeviceFacade.getById", observer -> stub.getByDeviceId(request, observer))
                .map(facadeGrpcDeviceBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeDeviceBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        List<Long> values = ids == null ? List.of() : ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (tenantId == null || values.isEmpty()) return Flux.empty();
        GrpcDeviceIdsQuery request = GrpcDeviceIdsQuery.newBuilder().addAllDeviceIds(values).setTenantId(tenantId).build();
        DeviceApiGrpc.DeviceApiStub stub = grpcFacadeSupport.withDeadline(deviceApiStub);
        return ReactiveGrpcClientSupport.<GrpcDeviceIdsQuery, GrpcDeviceListDTO>unary(
                        "DeviceFacade.listByIds", observer -> stub.listByDeviceIds(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()).map(facadeGrpcDeviceBuilder::toFacadeBO));
    }

    @Override
    public Mono<io.github.pnoker.db.r2dbc.core.page.OffsetPage<FacadeDeviceBO>> listReactive(FacadeDeviceOffsetQuery query) {
        if (query == null) return Mono.error(new IllegalArgumentException("query is required"));
        GrpcOffsetDeviceQuery request = facadeGrpcDeviceBuilder.toGrpcOffsetQuery(query);
        DeviceApiGrpc.DeviceApiStub stub = grpcFacadeSupport.withDeadline(deviceApiStub);
        return ReactiveGrpcClientSupport.<GrpcOffsetDeviceQuery, GrpcOffsetPageDeviceDTO>unary(
                        "DeviceFacade.list", observer -> stub.list(request, observer))
                .map(response -> {
                    if (!response.hasPage()) throw new ServiceException("DeviceFacade.list returned no page");
                    var page = response.getPage();
                    List<FacadeDeviceBO> items = response.getItemsList().stream().map(facadeGrpcDeviceBuilder::toFacadeBO).toList();
                    return io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(items, page.getOffset(), page.getLimit(), page.getTotal());
                });
    }

    @Override
    public Flux<FacadeDeviceBO> listByProfileIdReactive(Long tenantId, Long profileId) {
        if (tenantId == null || profileId == null) return Flux.empty();
        GrpcProfileQuery request = GrpcProfileQuery.newBuilder().setProfileId(profileId).setTenantId(tenantId).build();
        DeviceApiGrpc.DeviceApiStub stub = grpcFacadeSupport.withDeadline(deviceApiStub);
        return ReactiveGrpcClientSupport.<GrpcProfileQuery, GrpcDeviceListDTO>unary(
                        "DeviceFacade.listByProfileId", observer -> stub.listByProfileId(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()).map(facadeGrpcDeviceBuilder::toFacadeBO));
    }

    @Override
    public Flux<FacadeDeviceBO> listByDriverIdReactive(Long tenantId, Long driverId) {
        if (tenantId == null || driverId == null) return Flux.empty();
        GrpcDriverQuery request = GrpcDriverQuery.newBuilder().setDriverId(driverId).setTenantId(tenantId).build();
        DeviceApiGrpc.DeviceApiStub stub = grpcFacadeSupport.withDeadline(deviceApiStub);
        return ReactiveGrpcClientSupport.<GrpcDriverQuery, GrpcDeviceListDTO>unary(
                        "DeviceFacade.listByDriverId", observer -> stub.listByDriverId(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()).map(facadeGrpcDeviceBuilder::toFacadeBO));
    }

    @Override
    public Mono<FacadeDeviceOwnerBO> getActiveOwnerReactive(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Mono.empty();
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(deviceId).setTenantId(tenantId).build();
        DeviceApiGrpc.DeviceApiStub stub = grpcFacadeSupport.withDeadline(deviceApiStub);
        return ReactiveGrpcClientSupport.<GrpcDeviceQuery, GrpcDeviceOwnerDTO>unary(
                        "DeviceFacade.getActiveOwner", observer -> stub.getActiveOwner(request, observer))
                .map(response -> new FacadeDeviceOwnerBO(response.getDriverId(), response.getOwnerNode(), response.getFencingToken()));
    }

}
