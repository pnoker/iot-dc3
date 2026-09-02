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

import io.github.pnoker.api.center.manager.EventApiGrpc;
import io.github.pnoker.api.center.manager.GrpcEventIdsQuery;
import io.github.pnoker.api.center.manager.GrpcEventListDTO;
import io.github.pnoker.api.center.manager.GrpcEventQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetEventQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageEventDTO;
import io.github.pnoker.api.common.GrpcEventDTO;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.query.FacadeEventOffsetQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcEventBuilder;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EventGrpcFacade implements EventFacade {

    private final EventApiGrpc.EventApiStub eventApiStub;
    private final FacadeGrpcEventBuilder eventBuilder;
    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Mono<FacadeEventBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null) {
            return Mono.empty();
        }
        GrpcEventQuery request =
                GrpcEventQuery.newBuilder().setTenantId(tenantId).setEventId(id).build();
        return ReactiveGrpcClientSupport.<GrpcEventQuery, GrpcEventDTO>unary(
                        "EventFacade.getById",
                        observer -> grpcFacadeSupport.withDeadline(eventApiStub).getById(request, observer))
                .map(eventBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids) {
        List<Long> values = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (tenantId == null || values.isEmpty()) {
            return Flux.empty();
        }
        GrpcEventIdsQuery request = GrpcEventIdsQuery.newBuilder()
                .setTenantId(tenantId)
                .addAllEventIds(values)
                .build();
        return ReactiveGrpcClientSupport.<GrpcEventIdsQuery, GrpcEventListDTO>unary(
                        "EventFacade.listByIds",
                        observer -> grpcFacadeSupport.withDeadline(eventApiStub).listByIds(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()))
                .map(eventBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeEventBO>> list(FacadeEventOffsetQuery query) {
        var request = eventBuilder.toGrpcOffsetQuery(query);
        return ReactiveGrpcClientSupport.<GrpcOffsetEventQuery, GrpcOffsetPageEventDTO>unary(
                        "EventFacade.list",
                        observer -> grpcFacadeSupport.withDeadline(eventApiStub).list(request, observer))
                .map(response -> OffsetPage.of(
                        response.getItemsList().stream()
                                .map(eventBuilder::toFacadeBO)
                                .toList(),
                        response.getPage().getOffset(),
                        response.getPage().getLimit(),
                        response.getPage().getTotal()));
    }
}
