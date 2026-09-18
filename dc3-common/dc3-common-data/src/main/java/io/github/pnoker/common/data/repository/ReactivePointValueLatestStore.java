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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for the relational latest-value projection. */
public interface ReactivePointValueLatestStore {

    /** Load the latest point value for the series. */
    Mono<PointValueDO> latest(Long tenantId, Long deviceId, Long pointId);

    /** Stream the latest point values for the tenant. */
    Flux<PointValueDO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds);

    /** Stream the latest point values as a continuous feed. */
    Flux<PointValueDO> listLatestStream(Long tenantId, int limit);

    /** Save the batch, inserting or updating as needed. */
    Mono<Integer> upsertBatch(List<PointValueDO> values);
}
