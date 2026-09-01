package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive persistence port for tenant-scoped commands. */
public interface ReactiveCommandStore {
    Mono<CommandBO> get(Long tenantId, Long id);
    Mono<Boolean> existsByNameOrCode(Long tenantId, Long profileId, String commandName, String commandCode, Long excludingId);
    Mono<CommandBO> insert(CommandBO value);
    Mono<CommandBO> update(CommandBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Flux<CommandBO> listByIds(Long tenantId, List<Long> ids);
    Flux<CommandBO> listByProfileId(Long tenantId, Long profileId);
    Flux<CommandBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<OffsetPage<CommandBO>> list(CommandFilter filter);
}
