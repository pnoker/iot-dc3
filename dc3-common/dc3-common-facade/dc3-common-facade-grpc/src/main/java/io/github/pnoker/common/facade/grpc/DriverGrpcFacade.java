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

import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDriverIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDriverListDTO;
import io.github.pnoker.api.center.manager.GrpcOffsetDriverQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageDriverDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcDriverBuilder;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * gRPC DriverFacade: forwards to Manager Center via
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class DriverGrpcFacade implements DriverFacade {

    private final DriverApiGrpc.DriverApiStub driverApiStub;

    private final FacadeGrpcDriverBuilder facadeGrpcDriverBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<FacadeDriverBO> getByIdReactive(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.empty();
        GrpcDriverQuery request = GrpcDriverQuery.newBuilder()
                .setDriverId(id)
                .setTenantId(tenantId)
                .build();
        DriverApiGrpc.DriverApiStub stub = grpcFacadeSupport.withDeadline(driverApiStub);
        return ReactiveGrpcClientSupport.<GrpcDriverQuery, GrpcDriverDTO>unary(
                        "DriverFacade.getById", observer -> stub.getByDriverId(request, observer))
                .map(facadeGrpcDriverBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeDriverBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) return Flux.empty();
        List<Long> values = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (values.isEmpty()) return Flux.empty();
        GrpcDriverIdsQuery request = GrpcDriverIdsQuery.newBuilder()
                .addAllDriverIds(values)
                .setTenantId(tenantId)
                .build();
        DriverApiGrpc.DriverApiStub stub = grpcFacadeSupport.withDeadline(driverApiStub);
        return ReactiveGrpcClientSupport.<GrpcDriverIdsQuery, GrpcDriverListDTO>unary(
                        "DriverFacade.listByIds", observer -> stub.listByDriverIds(request, observer))
                .flatMapMany(response ->
                        Flux.fromIterable(response.getItemsList()).map(facadeGrpcDriverBuilder::toFacadeBO));
    }

    @Override
    public Mono<OffsetPage<FacadeDriverBO>> listReactive(FacadeDriverOffsetQuery query) {
        GrpcOffsetDriverQuery request = facadeGrpcDriverBuilder.toGrpcOffsetQuery(query);
        DriverApiGrpc.DriverApiStub stub = grpcFacadeSupport.withDeadline(driverApiStub);
        return ReactiveGrpcClientSupport.<GrpcOffsetDriverQuery, GrpcOffsetPageDriverDTO>unary(
                        "DriverFacade.list", observer -> stub.list(request, observer))
                .map(response -> {
                    if (!response.hasPage()) throw new IllegalStateException("DriverFacade.list returned no page");
                    var page = response.getPage();
                    return OffsetPage.of(
                            response.getItemsList().stream()
                                    .map(facadeGrpcDriverBuilder::toFacadeBO)
                                    .toList(),
                            page.getOffset(),
                            page.getLimit(),
                            page.getTotal());
                });
    }
}
