package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactivePointAttributeConfigStore {
    Mono<PointAttributeConfigBO> get(Long tenantId, Long id);
    Mono<PointAttributeConfigBO> getByAttributeDevicePoint(Long tenantId, Long attributeId, Long deviceId, Long pointId);
    Flux<PointAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<PointAttributeConfigBO> listByDeviceIdAndPointId(Long tenantId, Long deviceId, Long pointId);
    Mono<PointAttributeConfigBO> insert(PointAttributeConfigBO value);
    Mono<PointAttributeConfigBO> update(PointAttributeConfigBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<OffsetPage<PointAttributeConfigBO>> list(PointAttributeConfigFilter filter);
}
