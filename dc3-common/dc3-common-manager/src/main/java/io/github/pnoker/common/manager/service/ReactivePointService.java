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
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.repository.PointFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
