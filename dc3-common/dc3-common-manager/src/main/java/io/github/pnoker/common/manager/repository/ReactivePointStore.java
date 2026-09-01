package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive persistence port for tenant-scoped points. */
public interface ReactivePointStore {
    Mono<PointBO> get(Long tenantId, Long id);
    Mono<Boolean> existsByNameOrCode(Long tenantId, Long profileId, String pointName, String pointCode, Long excludingId);
    Mono<PointBO> insert(PointBO value);
    Mono<PointBO> update(PointBO value, int expectedVersion);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<OffsetPage<PointBO>> list(PointFilter filter);
    Flux<PointBO> listByIds(Long tenantId, List<Long> ids);
    Flux<PointBO> listByProfileId(Long tenantId, Long profileId);
    Flux<PointBO> listByDeviceId(Long tenantId, Long deviceId);
    Flux<Long> listConfiguredDeviceIdsByPointId(Long tenantId, Long pointId);
    Mono<Long> countByDeviceId(Long tenantId, Long deviceId);
}
