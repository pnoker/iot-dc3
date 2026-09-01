package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveEventAttributeConfigStore {
    Mono<EventAttributeConfigBO> get(Long tenantId, Long id);
    Mono<EventAttributeConfigBO> getByAttributeDeviceEvent(Long tenantId, Long attributeId, Long deviceId, Long eventId);
    Flux<EventAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<EventAttributeConfigBO> listByDeviceIdAndEventId(Long tenantId, Long deviceId, Long eventId);
    Mono<EventAttributeConfigBO> insert(EventAttributeConfigBO value);
    Mono<EventAttributeConfigBO> update(EventAttributeConfigBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<OffsetPage<EventAttributeConfigBO>> list(EventAttributeConfigFilter filter);
}
