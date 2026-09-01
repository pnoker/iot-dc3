package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.repository.EventParamFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive application service for event parameters. */
public interface ReactiveEventParamService {
    Mono<EventParamBO> add(EventParamBO value);
    Mono<EventParamBO> update(EventParamBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<EventParamBO> getById(Long tenantId, Long id);
    Flux<EventParamBO> listByEventId(Long tenantId, Long eventId);
    Flux<EventParamBO> listByIds(Long tenantId, Collection<Long> ids);
    Mono<OffsetPage<EventParamBO>> list(EventParamFilter filter);
}
