package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive persistence port for tenant-scoped event parameters. */
public interface ReactiveEventParamStore {
    Mono<EventParamBO> get(Long tenantId, Long id);
    Flux<EventParamBO> listByEventId(Long tenantId, Long eventId);
    Flux<EventParamBO> listByIds(Long tenantId, Collection<Long> ids);
    Mono<Boolean> existsByNameOrCode(Long tenantId, Long eventId, String paramName, String paramCode, Long excludedId);
    Mono<EventParamBO> insert(EventParamBO value);
    Mono<EventParamBO> update(EventParamBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<Long> deleteByEventId(Long tenantId, Long eventId, Long operatorId, String operatorName);
    Mono<OffsetPage<EventParamBO>> list(EventParamFilter filter);
}
