package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.repository.PointAttributeConfigFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactivePointAttributeConfigService {
    Mono<PointAttributeConfigBO> add(PointAttributeConfigBO value);
    Mono<PointAttributeConfigBO> update(PointAttributeConfigBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<PointAttributeConfigBO> getById(Long tenantId, Long id);
    Mono<PointAttributeConfigBO> getByAttributeIdAndDeviceIdAndPointId(Long tenantId, Long attributeId, Long deviceId, Long pointId);
    Flux<PointAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<PointAttributeConfigBO> listByDeviceIdAndPointId(Long tenantId, Long deviceId, Long pointId);
    Mono<OffsetPage<PointAttributeConfigBO>> list(PointAttributeConfigFilter filter);
}
