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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped points. */
public interface ReactivePointStore {
    /** Load the point scoped to the tenant by id. */
    Mono<PointBO> get(Long tenantId, Long id);

    /** Check whether a record exists for the given name or code. */
    Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long profileId, String pointName, String pointCode, Long excludingId);

    /** Insert one point and emit the stored row. */
    Mono<PointBO> insert(PointBO value);

    /** Update one point and emit the updated row. */
    Mono<PointBO> update(PointBO value, int expectedVersion);

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

    /** List points matched by point id. */
    Flux<Long> listConfiguredDeviceIdsByPointId(Long tenantId, Long pointId);

    /** Count records matched by device id. */
    Mono<Long> countByDeviceId(Long tenantId, Long deviceId);
}
