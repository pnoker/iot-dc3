package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.repository.PointAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/** Reactive point attribute application service. */
public interface ReactivePointAttributeService {
    Mono<PointAttributeBO> add(PointAttributeBO value);
    Mono<PointAttributeBO> update(PointAttributeBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<PointAttributeBO> getById(Long tenantId, Long id);
    Mono<PointAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);
    Flux<PointAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<OffsetPage<PointAttributeBO>> list(PointAttributeFilter filter);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<List<PointAttributeBO>> saveBatch(List<PointAttributeBO> values);
    Mono<List<PointAttributeBO>> updateBatch(List<PointAttributeBO> values);
}
