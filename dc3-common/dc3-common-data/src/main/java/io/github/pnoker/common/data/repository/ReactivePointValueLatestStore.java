package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive persistence port for the relational latest-value projection. */
public interface ReactivePointValueLatestStore {

    Mono<PointValueDO> latest(Long tenantId, Long deviceId, Long pointId);

    Flux<PointValueDO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds);

    Flux<PointValueDO> listLatestStream(Long tenantId, int limit);

    Mono<Integer> upsertBatch(List<PointValueDO> values);
}
