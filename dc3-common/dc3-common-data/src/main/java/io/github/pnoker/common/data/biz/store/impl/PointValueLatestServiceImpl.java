package io.github.pnoker.common.data.biz.store.impl;

import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.repository.ReactivePointValueLatestStore;
import io.github.pnoker.common.entity.bo.PointValueBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive latest-value projection service. */
@Service
@RequiredArgsConstructor
public class PointValueLatestServiceImpl implements PointValueLatestService {

    private final ReactivePointValueLatestStore store;
    private final PointValueBuilder pointValueBuilder;

    @Override
    public Mono<PointValueBO> latest(Long tenantId, Long deviceId, Long pointId) {
        return store.latest(tenantId, deviceId, pointId).map(pointValueBuilder::buildBOByDO);
    }

    @Override
    public Flux<PointValueBO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds) {
        return store.listLatest(tenantId, deviceId, pointIds).map(pointValueBuilder::buildBOByDO);
    }

    @Override
    public Flux<PointValueBO> listLatestStream(Long tenantId, int limit) {
        return store.listLatestStream(tenantId, limit).map(pointValueBuilder::buildBOByDO);
    }
}
