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

import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.repository.PointAttributeConfigFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Business service covering point attribute config use cases. */

public interface ReactivePointAttributeConfigService {
    /** Add one point attribute config. */
    Mono<PointAttributeConfigBO> add(PointAttributeConfigBO value);

    /** Update one point attribute config and emit the updated row. */
    Mono<PointAttributeConfigBO> update(PointAttributeConfigBO value);

    /** Delete the point attribute config, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Resolve the point attribute config by its id. */
    Mono<PointAttributeConfigBO> getById(Long tenantId, Long id);

    /** Resolve the point attribute config by its attribute id and device id and point id. */
    Mono<PointAttributeConfigBO> getByAttributeIdAndDeviceIdAndPointId(
            Long tenantId, Long attributeId, Long deviceId, Long pointId);

    /** List point attribute configs matched by device id. */
    Flux<PointAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);

    /** List point attribute configs matched by device id and point id. */
    Flux<PointAttributeConfigBO> listByDeviceIdAndPointId(Long tenantId, Long deviceId, Long pointId);

    /** Page point attribute configs matching the tenant-scoped filters. */
    Mono<OffsetPage<PointAttributeConfigBO>> list(PointAttributeConfigFilter filter);
}
