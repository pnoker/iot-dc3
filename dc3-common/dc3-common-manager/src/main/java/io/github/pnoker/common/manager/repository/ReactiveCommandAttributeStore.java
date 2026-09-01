package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive persistence port for tenant-scoped command attributes. */
public interface ReactiveCommandAttributeStore {
    Mono<CommandAttributeBO> get(Long tenantId, Long id);
    Mono<CommandAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId);
    Flux<CommandAttributeBO> listByDriverId(Long tenantId, Long driverId);
    Mono<CommandAttributeBO> insert(CommandAttributeBO value);
    Mono<CommandAttributeBO> update(CommandAttributeBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);
    Mono<OffsetPage<CommandAttributeBO>> list(CommandAttributeFilter filter);
}
