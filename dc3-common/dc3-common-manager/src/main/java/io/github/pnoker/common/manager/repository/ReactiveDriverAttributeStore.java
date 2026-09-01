package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive persistence port for tenant-scoped driver attributes. */
public interface ReactiveDriverAttributeStore {
    Mono<DriverAttributeBO> get(Long tenantId, Long id);
    Mono<DriverAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId);
    Flux<DriverAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<DriverAttributeBO> insert(DriverAttributeBO value);
    Mono<DriverAttributeBO> update(DriverAttributeBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<OffsetPage<DriverAttributeBO>> list(DriverAttributeFilter filter);
}
