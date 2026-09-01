package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.repository.PointFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/** Reactive application service for point metadata. */
public interface ReactivePointService {
    Mono<PointBO> getById(Long tenantId, Long id);
    Mono<PointBO> add(PointBO value);
    Mono<PointBO> update(PointBO value);
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);
    Mono<OffsetPage<PointBO>> list(PointFilter filter);
    Flux<PointBO> listByIds(Long tenantId, List<Long> ids);
    Flux<PointBO> listByProfileId(Long tenantId, Long profileId);
    Flux<PointBO> listByDeviceId(Long tenantId, Long deviceId);
    Mono<Map<String, String>> listUnits(Long tenantId, List<Long> ids);
    Mono<DeviceByPointBO> getDeviceStatisticsByPointId(Long tenantId, Long pointId);
    Mono<Long> getCountByDeviceId(Long tenantId, Long deviceId);
    Mono<PointConfigByDeviceBO> getPointConfigByDeviceId(Long tenantId, Long deviceId);
}
