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

import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped driver attributes. */
public interface ReactiveDriverAttributeStore {
    /** Load the driver attribute scoped to the tenant by id. */
    Mono<DriverAttributeBO> get(Long tenantId, Long id);

    /** Resolve the driver attribute by its code and driver. */
    Mono<DriverAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId);

    /** List driver attributes matched by driver id. */
    Flux<DriverAttributeBO> listByDriverId(Long tenantId, Long driverId);

    /** Insert one driver attribute and emit the stored row. */
    Mono<DriverAttributeBO> insert(DriverAttributeBO value);

    /** Update one driver attribute and emit the updated row. */
    Mono<DriverAttributeBO> update(DriverAttributeBO value, int expectedVersion);

    /** Delete the driver attribute, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Delete the records matched by ids. */
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);

    /** Page driver attributes matching the tenant-scoped filters. */
    Mono<OffsetPage<DriverAttributeBO>> list(DriverAttributeFilter filter);
}
