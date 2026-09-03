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

import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for point attribute config records. */

public interface ReactivePointAttributeConfigStore {
    /** Load the point attribute config scoped to the tenant by id. */
    Mono<PointAttributeConfigBO> get(Long tenantId, Long id);

    /** Resolve the point attribute config by its attribute device point. */
    Mono<PointAttributeConfigBO> getByAttributeDevicePoint(
            Long tenantId, Long attributeId, Long deviceId, Long pointId);

    /** List point attribute configs matched by device id. */
    Flux<PointAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);

    /** List point attribute configs matched by device id and point id. */
    Flux<PointAttributeConfigBO> listByDeviceIdAndPointId(Long tenantId, Long deviceId, Long pointId);

    /** Insert one point attribute config and emit the stored row. */
    Mono<PointAttributeConfigBO> insert(PointAttributeConfigBO value);

    /** Update one point attribute config and emit the updated row. */
    Mono<PointAttributeConfigBO> update(PointAttributeConfigBO value, int expectedVersion);

    /** Delete the point attribute config, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Page point attribute configs matching the tenant-scoped filters. */
    Mono<OffsetPage<PointAttributeConfigBO>> list(PointAttributeConfigFilter filter);
}
