package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.repository.CommandParamFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/** Reactive application service for command parameters. */
public interface ReactiveCommandParamService {
    Mono<CommandParamBO> add(CommandParamBO value);
    Mono<CommandParamBO> update(CommandParamBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<CommandParamBO> getById(Long tenantId, Long id);
    Flux<CommandParamBO> listByCommandId(Long tenantId, Long commandId);
    Flux<CommandParamBO> listByIds(Long tenantId, Collection<Long> ids);
    Mono<OffsetPage<CommandParamBO>> list(CommandParamFilter filter);
}
