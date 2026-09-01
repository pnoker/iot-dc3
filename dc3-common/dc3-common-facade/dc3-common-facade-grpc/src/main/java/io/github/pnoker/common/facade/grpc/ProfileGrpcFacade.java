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

import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcOffsetProfileQuery;
import io.github.pnoker.api.center.manager.GrpcProfileIdsQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcProfileListDTO;
import io.github.pnoker.api.center.manager.ProfileApiGrpc;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileOffsetQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcProfileBuilder;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * gRPC ProfileFacade: forwards to Manager Center via {@link ProfileApiGrpc}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class ProfileGrpcFacade implements ProfileFacade {


    private final ProfileApiGrpc.ProfileApiStub profileApiStub;

    private final FacadeGrpcProfileBuilder facadeGrpcProfileBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<FacadeProfileBO> getByIdReactive(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        GrpcProfileQuery request = GrpcProfileQuery.newBuilder().setProfileId(id).setTenantId(tenantId).build();
        ProfileApiGrpc.ProfileApiStub stub = grpcFacadeSupport.withDeadline(profileApiStub);
        return ReactiveGrpcClientSupport.<GrpcProfileQuery, GrpcProfileDTO>unary(
                        "ProfileFacade.getById", observer -> stub.getByProfileId(request, observer))
                .map(facadeGrpcProfileBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeProfileBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> normalized = ids.stream().filter(value -> value != null && value > 0).distinct().toList();
        if (normalized.isEmpty()) return Flux.empty();
        GrpcProfileIdsQuery request = GrpcProfileIdsQuery.newBuilder().addAllProfileIds(normalized).setTenantId(tenantId).build();
        ProfileApiGrpc.ProfileApiStub stub = grpcFacadeSupport.withDeadline(profileApiStub);
        return ReactiveGrpcClientSupport.<GrpcProfileIdsQuery, GrpcProfileListDTO>unary(
                        "ProfileFacade.listByIds", observer -> stub.listByProfileIds(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()).map(facadeGrpcProfileBuilder::toFacadeBO));
    }

    @Override
    public Flux<FacadeProfileBO> listByDeviceIdReactive(Long tenantId, Long deviceId) {
        if (tenantId == null || deviceId == null) return Flux.empty();
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(deviceId).setTenantId(tenantId).build();
        ProfileApiGrpc.ProfileApiStub stub = grpcFacadeSupport.withDeadline(profileApiStub);
        return ReactiveGrpcClientSupport.<GrpcDeviceQuery, GrpcProfileListDTO>unary(
                        "ProfileFacade.listByDeviceId", observer -> stub.listByDeviceId(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()).map(facadeGrpcProfileBuilder::toFacadeBO));
    }

    @Override
    public Mono<OffsetPage<FacadeProfileBO>> listReactive(FacadeProfileOffsetQuery query) {
        GrpcOffsetProfileQuery request = facadeGrpcProfileBuilder.toGrpcOffsetQuery(query);
        ProfileApiGrpc.ProfileApiStub stub = grpcFacadeSupport.withDeadline(profileApiStub);
        return ReactiveGrpcClientSupport.<GrpcOffsetProfileQuery, GrpcOffsetPageProfileDTO>unary(
                        "ProfileFacade.list", observer -> stub.list(request, observer))
                .map(response -> {
                    if (!response.hasPage()) throw new IllegalStateException("ProfileFacade.list returned no page");
                    var page = response.getPage();
                    List<FacadeProfileBO> items = response.getItemsList().stream()
                            .map(facadeGrpcProfileBuilder::toFacadeBO).toList();
                    return OffsetPage.of(items, page.getOffset(), page.getLimit(), page.getTotal());
                });
    }

}
