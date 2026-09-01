package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive application service for command metadata. */
public interface ReactiveCommandService {
    Mono<CommandBO> getById(Long tenantId, Long id);
    Mono<CommandBO> add(CommandBO value);
    Mono<CommandBO> update(CommandBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Flux<CommandBO> listByIds(Long tenantId, List<Long> ids);
    Flux<CommandBO> listByProfileId(Long tenantId, Long profileId);
    Flux<CommandBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<OffsetPage<CommandBO>> list(CommandFilter filter);
}
