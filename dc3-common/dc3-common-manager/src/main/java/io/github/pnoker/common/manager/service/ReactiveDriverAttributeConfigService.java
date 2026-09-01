package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.repository.DriverAttributeConfigFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveDriverAttributeConfigService {
    Mono<DriverAttributeConfigBO> add(DriverAttributeConfigBO value);
    Mono<DriverAttributeConfigBO> update(DriverAttributeConfigBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<DriverAttributeConfigBO> getById(Long tenantId, Long id);
    Mono<DriverAttributeConfigBO> getByAttributeIdAndDeviceId(Long tenantId, Long attributeId, Long deviceId);
    Flux<DriverAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<OffsetPage<DriverAttributeConfigBO>> list(DriverAttributeConfigFilter filter);
}
