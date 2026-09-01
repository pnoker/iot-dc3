package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive persistence port for tenant-scoped point attributes. */
public interface ReactivePointAttributeStore {
    Mono<PointAttributeBO> get(Long tenantId, Long id);
    Mono<PointAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId);
    Flux<PointAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<PointAttributeBO> insert(PointAttributeBO value);
    Mono<PointAttributeBO> update(PointAttributeBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<OffsetPage<PointAttributeBO>> list(PointAttributeFilter filter);
}
