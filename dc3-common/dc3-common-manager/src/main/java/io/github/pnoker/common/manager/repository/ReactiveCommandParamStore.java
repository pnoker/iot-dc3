package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive persistence port for tenant-scoped command parameters. */
public interface ReactiveCommandParamStore {
    Mono<CommandParamBO> get(Long tenantId, Long id);
    Flux<CommandParamBO> listByCommandId(Long tenantId, Long commandId);
    Flux<CommandParamBO> listByIds(Long tenantId, Collection<Long> ids);
    Mono<Boolean> existsByNameOrCode(Long tenantId, Long commandId, String paramName, String paramCode, Long excludedId);
    Mono<CommandParamBO> insert(CommandParamBO value);
    Mono<CommandParamBO> update(CommandParamBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<Long> deleteByCommandId(Long tenantId, Long commandId, Long operatorId, String operatorName);
    Mono<OffsetPage<CommandParamBO>> list(CommandParamFilter filter);
}
