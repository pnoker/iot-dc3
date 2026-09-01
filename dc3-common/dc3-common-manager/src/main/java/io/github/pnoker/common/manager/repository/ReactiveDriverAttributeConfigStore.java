package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveDriverAttributeConfigStore {
    Mono<DriverAttributeConfigBO> get(Long tenantId, Long id);
    Mono<DriverAttributeConfigBO> getByAttributeAndDevice(Long tenantId, Long attributeId, Long deviceId);
    Flux<DriverAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<DriverAttributeConfigBO> insert(DriverAttributeConfigBO value);
    Mono<DriverAttributeConfigBO> update(DriverAttributeConfigBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<OffsetPage<DriverAttributeConfigBO>> list(DriverAttributeConfigFilter filter);
}
