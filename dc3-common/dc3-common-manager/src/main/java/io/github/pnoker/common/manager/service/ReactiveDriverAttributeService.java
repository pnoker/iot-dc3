package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.repository.DriverAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/** Reactive driver attribute application service. */
public interface ReactiveDriverAttributeService {
    Mono<DriverAttributeBO> add(DriverAttributeBO value);
    Mono<DriverAttributeBO> update(DriverAttributeBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<DriverAttributeBO> getById(Long tenantId, Long id);
    Mono<DriverAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);
    Flux<DriverAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<OffsetPage<DriverAttributeBO>> list(DriverAttributeFilter filter);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<List<DriverAttributeBO>> saveBatch(List<DriverAttributeBO> values);
    Mono<List<DriverAttributeBO>> updateBatch(List<DriverAttributeBO> values);
}
