package io.github.pnoker.common.data.biz.store;

import io.github.pnoker.common.entity.bo.PointValueBO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive latest-value read/write boundary over the relational projection. */
public interface PointValueLatestService {

    Mono<PointValueBO> latest(Long tenantId, Long deviceId, Long pointId);

    Flux<PointValueBO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds);

    Flux<PointValueBO> listLatestStream(Long tenantId, int limit);
}
