package io.github.pnoker.common.manager.grpc.server.manager;

import io.github.pnoker.api.center.manager.EventApiGrpc;
import io.github.pnoker.api.center.manager.GrpcEventIdsQuery;
import io.github.pnoker.api.center.manager.GrpcEventListDTO;
import io.github.pnoker.api.center.manager.GrpcEventQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetEventQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageEventDTO;
import io.github.pnoker.api.common.GrpcEventDTO;
import io.github.pnoker.api.common.OffsetPage;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventBuilder;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class ManagerEventServer extends EventApiGrpc.EventApiImplBase {

    private final GrpcEventBuilder grpcEventBuilder;
    private final ReactiveEventService eventService;

    @Override
    public void list(GrpcOffsetEventQuery request, StreamObserver<GrpcOffsetPageEventDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(Mono.fromSupplier(() -> filter(request))
                .flatMap(eventService::list)
                .map(page ->
                GrpcOffsetPageEventDTO.newBuilder()
                        .setPage(OffsetPage.newBuilder()
                                .setOffset(page.offset())
                                .setLimit(page.limit())
                                .setTotal(page.total())
                                .setHasNext(page.hasNext()))
                        .addAllItems(page.items().stream().map(grpcEventBuilder::buildGrpcDTOByBO).toList())
                        .build()), observer);
    }

    @Override
    public void getById(GrpcEventQuery request, StreamObserver<GrpcEventDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(eventService.getById(request.getTenantId(), request.getEventId())
                .map(grpcEventBuilder::buildGrpcDTOByBO), observer);
    }

    @Override
    public void listByIds(GrpcEventIdsQuery request, StreamObserver<GrpcEventListDTO> observer) {
        ReactiveGrpcServerSupport.subscribe(eventService.listByIds(request.getTenantId(), request.getEventIdsList())
                .map(grpcEventBuilder::buildGrpcDTOByBO)
                .collectList()
                .map(items -> GrpcEventListDTO.newBuilder().addAllItems(items).build()), observer);
    }

    private EventFilter filter(GrpcOffsetEventQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new EventFilter(request.getTenantId(), request.getEventName(), request.getEventCode(),
                request.hasEventTypeFlag() ? EventTypeFlagEnum.ofIndex((byte) request.getEventTypeFlag()) : null,
                request.hasEventLevelFlag() ? EventLevelEnum.ofIndex((byte) request.getEventLevelFlag()) : null,
                request.hasProfileId() ? request.getProfileId() : null,
                request.hasEnableFlag() ? EnableFlagEnum.ofIndex((byte) request.getEnableFlag()) : null,
                request.hasVersion() ? request.getVersion() : null,
                request.hasDeviceId() ? request.getDeviceId() : null,
                page.offset(), page.limit(), page.sort());
    }
}
