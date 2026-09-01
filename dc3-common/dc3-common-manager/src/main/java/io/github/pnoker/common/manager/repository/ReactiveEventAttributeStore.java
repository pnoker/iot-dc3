package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive persistence port for tenant-scoped event attributes. */
public interface ReactiveEventAttributeStore {
    Mono<EventAttributeBO> get(Long tenantId, Long id);
    Mono<EventAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId);
    Flux<EventAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<EventAttributeBO> insert(EventAttributeBO value);
    Mono<EventAttributeBO> update(EventAttributeBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<OffsetPage<EventAttributeBO>> list(EventAttributeFilter filter);
}
