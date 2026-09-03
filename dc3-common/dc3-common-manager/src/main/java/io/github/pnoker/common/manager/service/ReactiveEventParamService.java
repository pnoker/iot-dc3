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

import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.repository.EventParamFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for event parameters. */
public interface ReactiveEventParamService {
    /** Add one event param. */
    Mono<EventParamBO> add(EventParamBO value);

    /** Update one event param and emit the updated row. */
    Mono<EventParamBO> update(EventParamBO value);

    /** Delete the event param, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Resolve the event param by its id. */
    Mono<EventParamBO> getById(Long tenantId, Long id);

    /** List event params matched by event id. */
    Flux<EventParamBO> listByEventId(Long tenantId, Long eventId);

    /** List event params matched by ids. */
    Flux<EventParamBO> listByIds(Long tenantId, Collection<Long> ids);

    /** Page event params matching the tenant-scoped filters. */
    Mono<OffsetPage<EventParamBO>> list(EventParamFilter filter);
}
