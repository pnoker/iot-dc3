package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.repository.EventAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/** Reactive event attribute application service. */
public interface ReactiveEventAttributeService {
    Mono<EventAttributeBO> add(EventAttributeBO value);
    Mono<EventAttributeBO> update(EventAttributeBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<EventAttributeBO> getById(Long tenantId, Long id);
    Mono<EventAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);
    Flux<EventAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<OffsetPage<EventAttributeBO>> list(EventAttributeFilter filter);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<List<EventAttributeBO>> saveBatch(List<EventAttributeBO> values);
    Mono<List<EventAttributeBO>> updateBatch(List<EventAttributeBO> values);
}
