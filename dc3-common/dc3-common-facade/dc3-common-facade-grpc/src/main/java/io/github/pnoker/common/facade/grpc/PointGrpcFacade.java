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

import io.github.pnoker.api.center.manager.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcOffsetPointQuery;
import io.github.pnoker.api.center.manager.GrpcPointIdsQuery;
import io.github.pnoker.api.center.manager.GrpcPointListDTO;
import io.github.pnoker.api.center.manager.GrpcPointQuery;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointBuilder;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * gRPC PointFacade: forwards to Manager Center. Canonical reactive methods use the
 * asynchronous stub; legacy methods remain available to callers that have not moved yet.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class PointGrpcFacade implements PointFacade {

    private final PointApiGrpc.PointApiStub pointApiStub;

    private final FacadeGrpcPointBuilder facadeGrpcPointBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<FacadePointBO> getByIdReactive(Long tenantId, Long id) {
        if (tenantId == null || id == null) {
            return Mono.empty();
        }
        GrpcPointQuery request =
                GrpcPointQuery.newBuilder().setPointId(id).setTenantId(tenantId).build();
        PointApiGrpc.PointApiStub stub = grpcFacadeSupport.withDeadline(pointApiStub);
        return ReactiveGrpcClientSupport.<GrpcPointQuery, GrpcPointDTO>unary(
                        "PointFacade.getById", observer -> stub.getById(request, observer))
                .map(facadeGrpcPointBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadePointBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        if (tenantId == null || ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        List<Long> pointIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (pointIds.isEmpty()) {
            return Flux.empty();
        }
        GrpcPointIdsQuery request = GrpcPointIdsQuery.newBuilder()
                .addAllPointIds(pointIds)
                .setTenantId(tenantId)
                .build();
        PointApiGrpc.PointApiStub stub = grpcFacadeSupport.withDeadline(pointApiStub);
        return ReactiveGrpcClientSupport.<GrpcPointIdsQuery, GrpcPointListDTO>unary(
                        "PointFacade.listByIds", observer -> stub.listByIds(request, observer))
                .flatMapMany(
                        response -> Flux.fromIterable(response.getItemsList()).map(facadeGrpcPointBuilder::toFacadeBO));
    }

    @Override
    public Mono<OffsetPage<FacadePointBO>> listReactive(FacadePointOffsetQuery query) {
        GrpcOffsetPointQuery request = facadeGrpcPointBuilder.toGrpcOffsetQuery(query);
        PointApiGrpc.PointApiStub stub = grpcFacadeSupport.withDeadline(pointApiStub);
        return ReactiveGrpcClientSupport.<GrpcOffsetPointQuery, GrpcOffsetPagePointDTO>unary(
                        "PointFacade.list", observer -> stub.list(request, observer))
                .map(response -> {
                    if (!response.hasPage()) throw new IllegalStateException("PointFacade.list returned no page");
                    var page = response.getPage();
                    List<FacadePointBO> items = response.getItemsList().stream()
                            .map(facadeGrpcPointBuilder::toFacadeBO)
                            .toList();
                    return OffsetPage.of(items, page.getOffset(), page.getLimit(), page.getTotal());
                });
    }
}
