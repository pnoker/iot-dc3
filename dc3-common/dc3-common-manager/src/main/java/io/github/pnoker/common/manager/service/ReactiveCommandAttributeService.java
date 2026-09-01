package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.repository.CommandAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/** Reactive command attribute application service. */
public interface ReactiveCommandAttributeService {
    Mono<CommandAttributeBO> add(CommandAttributeBO value);
    Mono<CommandAttributeBO> update(CommandAttributeBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<CommandAttributeBO> getById(Long tenantId, Long id);
    Mono<CommandAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);
    Flux<CommandAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<OffsetPage<CommandAttributeBO>> list(CommandAttributeFilter filter);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<List<CommandAttributeBO>> saveBatch(List<CommandAttributeBO> values);
    Mono<List<CommandAttributeBO>> updateBatch(List<CommandAttributeBO> values);
}
