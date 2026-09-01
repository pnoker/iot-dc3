package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

/** Reactive persistence port for tenant-scoped events. */
public interface ReactiveEventStore {
    Mono<EventBO> get(Long tenantId, Long id);
    Mono<Boolean> existsByNameOrCode(Long tenantId, Long profileId, String eventName, String eventCode, Long excludingId);
    Mono<EventBO> insert(EventBO value);
    Mono<EventBO> update(EventBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Flux<EventBO> listByIds(Long tenantId, List<Long> ids);
    Flux<EventBO> listByProfileId(Long tenantId, Long profileId);
    Flux<EventBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<OffsetPage<EventBO>> list(EventFilter filter);
}
