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

import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped events. */
public interface ReactiveEventStore {
    /** Load the event scoped to the tenant by id. */
    Mono<EventBO> get(Long tenantId, Long id);

    /** Check whether a record exists for the given name or code. */
    Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long profileId, String eventName, String eventCode, Long excludingId);

    /** Insert one event and emit the stored row. */
    Mono<EventBO> insert(EventBO value);

    /** Update one event and emit the updated row. */
    Mono<EventBO> update(EventBO value, int expectedVersion);

    /** Delete the event, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** List events matched by ids. */
    Flux<EventBO> listByIds(Long tenantId, List<Long> ids);

    /** List events matched by profile id. */
    Flux<EventBO> listByProfileId(Long tenantId, Long profileId);

    /** List events matched by device id. */
    Flux<EventBO> listByDeviceId(Long tenantId, Long deviceId);

    /** Page events matching the tenant-scoped filters. */
    Mono<OffsetPage<EventBO>> list(EventFilter filter);
}
