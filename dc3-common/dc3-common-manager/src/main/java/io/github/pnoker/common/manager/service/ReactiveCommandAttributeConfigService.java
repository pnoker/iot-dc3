package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.repository.CommandAttributeConfigFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCommandAttributeConfigService {
    Mono<CommandAttributeConfigBO> add(CommandAttributeConfigBO value);
    Mono<CommandAttributeConfigBO> update(CommandAttributeConfigBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<CommandAttributeConfigBO> getById(Long tenantId, Long id);
    Mono<CommandAttributeConfigBO> getByAttributeIdAndDeviceIdAndCommandId(Long tenantId, Long attributeId, Long deviceId, Long commandId);
    Flux<CommandAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long tenantId, Long deviceId, Long commandId);
    Mono<OffsetPage<CommandAttributeConfigBO>> list(CommandAttributeConfigFilter filter);
}
