/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.data.biz.store.impl;

import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.repository.ReactivePointValueLatestStore;
import io.github.pnoker.common.entity.bo.PointValueBO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
