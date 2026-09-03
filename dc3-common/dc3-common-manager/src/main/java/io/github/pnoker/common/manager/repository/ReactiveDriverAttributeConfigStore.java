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

import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for driver attribute config records. */

public interface ReactiveDriverAttributeConfigStore {
    /** Load the driver attribute config scoped to the tenant by id. */
    Mono<DriverAttributeConfigBO> get(Long tenantId, Long id);

    /** Resolve the driver attribute config by its attribute and device. */
    Mono<DriverAttributeConfigBO> getByAttributeAndDevice(Long tenantId, Long attributeId, Long deviceId);

    /** List driver attribute configs matched by device id. */
    Flux<DriverAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);

    /** Insert one driver attribute config and emit the stored row. */
    Mono<DriverAttributeConfigBO> insert(DriverAttributeConfigBO value);

    /** Update one driver attribute config and emit the updated row. */
    Mono<DriverAttributeConfigBO> update(DriverAttributeConfigBO value, int expectedVersion);

    /** Delete the driver attribute config, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Page driver attribute configs matching the tenant-scoped filters. */
    Mono<OffsetPage<DriverAttributeConfigBO>> list(DriverAttributeConfigFilter filter);
}
