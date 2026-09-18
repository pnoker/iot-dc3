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

import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.repository.EventAttributeFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive event attribute application service. */
public interface ReactiveEventAttributeService {
    /** Add one event attribute. */
    Mono<EventAttributeBO> add(EventAttributeBO value);

    /** Update one event attribute and emit the updated row. */
    Mono<EventAttributeBO> update(EventAttributeBO value);

    /** Delete the event attribute, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Resolve the event attribute by its id. */
    Mono<EventAttributeBO> getById(Long tenantId, Long id);

    /** Resolve the event attribute by its name and driver id. */
    Mono<EventAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId);

    /** List event attributes matched by driver id. */
    Flux<EventAttributeBO> listByDriverId(Long tenantId, Long driverId);

    /** Page event attributes matching the tenant-scoped filters. */
    Mono<OffsetPage<EventAttributeBO>> list(EventAttributeFilter filter);

    /** Delete the records matched by ids. */
    Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName);

    /** Save the batch, inserting or updating as needed. */
    Mono<List<EventAttributeBO>> saveBatch(List<EventAttributeBO> values);

    /** Update one batch and emit the updated row. */
    Mono<List<EventAttributeBO>> updateBatch(List<EventAttributeBO> values);
}
