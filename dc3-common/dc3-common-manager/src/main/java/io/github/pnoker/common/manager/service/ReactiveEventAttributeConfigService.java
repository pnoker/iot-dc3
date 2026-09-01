package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.repository.EventAttributeConfigFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveEventAttributeConfigService {
    Mono<EventAttributeConfigBO> add(EventAttributeConfigBO value);
    Mono<EventAttributeConfigBO> update(EventAttributeConfigBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<EventAttributeConfigBO> getById(Long tenantId, Long id);
    Mono<EventAttributeConfigBO> getByAttributeIdAndDeviceIdAndEventId(Long tenantId, Long attributeId, Long deviceId, Long eventId);
    Flux<EventAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<EventAttributeConfigBO> listByDeviceIdAndEventId(Long tenantId, Long deviceId, Long eventId);
    Mono<OffsetPage<EventAttributeConfigBO>> list(EventAttributeConfigFilter filter);
}
