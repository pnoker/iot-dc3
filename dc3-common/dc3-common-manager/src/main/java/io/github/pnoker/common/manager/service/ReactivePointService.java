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
    /** Resolve the point by its id. */
    Mono<PointBO> getById(Long tenantId, Long id);

    /** Add one point. */
    Mono<PointBO> add(PointBO value);

    /** Update one point and emit the updated row. */
    Mono<PointBO> update(PointBO value);

    /** Delete the point, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Page points matching the tenant-scoped filters. */
    Mono<OffsetPage<PointBO>> list(PointFilter filter);

    /** List points matched by ids. */
    Flux<PointBO> listByIds(Long tenantId, List<Long> ids);

    /** List points matched by profile id. */
    Flux<PointBO> listByProfileId(Long tenantId, Long profileId);

    /** List points matched by device id. */
    Flux<PointBO> listByDeviceId(Long tenantId, Long deviceId);

    /** List units matching the request. */
    Mono<Map<String, String>> listUnits(Long tenantId, List<Long> ids);

    /** Resolve the point by its point id. */
    Mono<DeviceByPointBO> getDeviceStatisticsByPointId(Long tenantId, Long pointId);

    /** Resolve the point by its device id. */
    Mono<Long> getCountByDeviceId(Long tenantId, Long deviceId);

    /** Resolve the point by its device id. */
    Mono<PointConfigByDeviceBO> getPointConfigByDeviceId(Long tenantId, Long deviceId);
}
