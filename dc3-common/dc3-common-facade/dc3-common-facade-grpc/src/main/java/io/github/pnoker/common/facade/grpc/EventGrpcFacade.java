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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
        GrpcEventQuery request = GrpcEventQuery.newBuilder().setTenantId(tenantId).setEventId(id).build();
        return ReactiveGrpcClientSupport.<GrpcEventQuery, GrpcEventDTO>
                unary("EventFacade.getById", observer -> grpcFacadeSupport.withDeadline(eventApiStub)
                        .getById(request, observer))
                .map(eventBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids) {
        List<Long> values = ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (tenantId == null || values.isEmpty()) {
            return Flux.empty();
        }
        GrpcEventIdsQuery request = GrpcEventIdsQuery.newBuilder()
                .setTenantId(tenantId)
                .addAllEventIds(values)
                .build();
        return ReactiveGrpcClientSupport.<GrpcEventIdsQuery, GrpcEventListDTO>
                        unary("EventFacade.listByIds", observer -> grpcFacadeSupport.withDeadline(eventApiStub)
                                .listByIds(request, observer))
                .flatMapMany(response -> Flux.fromIterable(response.getItemsList()))
                .map(eventBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeEventBO>> list(FacadeEventOffsetQuery query) {
        var request = eventBuilder.toGrpcOffsetQuery(query);
        return ReactiveGrpcClientSupport.<GrpcOffsetEventQuery, GrpcOffsetPageEventDTO>
                unary("EventFacade.list", observer -> grpcFacadeSupport.withDeadline(eventApiStub)
                        .list(request, observer))
                .map(response -> OffsetPage.of(response.getItemsList().stream().map(eventBuilder::toFacadeBO).toList(),
                        response.getPage().getOffset(), response.getPage().getLimit(), response.getPage().getTotal()));
    }
}
