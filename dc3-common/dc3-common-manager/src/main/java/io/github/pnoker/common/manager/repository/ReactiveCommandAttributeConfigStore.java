package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCommandAttributeConfigStore {
    Mono<CommandAttributeConfigBO> get(Long tenantId, Long id);
    Mono<CommandAttributeConfigBO> getByAttributeDeviceCommand(Long tenantId, Long attributeId, Long deviceId, Long commandId);
    Flux<CommandAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long tenantId, Long deviceId, Long commandId);
    Mono<CommandAttributeConfigBO> insert(CommandAttributeConfigBO value);
    Mono<CommandAttributeConfigBO> update(CommandAttributeConfigBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<OffsetPage<CommandAttributeConfigBO>> list(CommandAttributeConfigFilter filter);
}
