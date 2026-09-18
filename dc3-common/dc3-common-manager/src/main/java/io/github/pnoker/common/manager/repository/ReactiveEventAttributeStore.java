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

import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped event attributes. */
public interface ReactiveEventAttributeStore {
    /** Load the event attribute scoped to the tenant by id. */
    Mono<EventAttributeBO> get(Long tenantId, Long id);

    /** Resolve the event attribute by its code and driver. */
    Mono<EventAttributeBO> getByCodeAndDriver(Long tenantId, String attributeCode, Long driverId);

    /** List event attributes matched by driver id. */
    Flux<EventAttributeBO> listByDriverId(Long tenantId, Long driverId);

    /** Insert one event attribute and emit the stored row. */
    Mono<EventAttributeBO> insert(EventAttributeBO value);

    /** Update one event attribute and emit the updated row. */
    Mono<EventAttributeBO> update(EventAttributeBO value, int expectedVersion);

    /** Delete the event attribute, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Delete the records matched by ids. */
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);

    /** Page event attributes matching the tenant-scoped filters. */
    Mono<OffsetPage<EventAttributeBO>> list(EventAttributeFilter filter);
}
