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

import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for event attribute config records. */

public interface ReactiveEventAttributeConfigStore {
    /** Load the event attribute config scoped to the tenant by id. */
    Mono<EventAttributeConfigBO> get(Long tenantId, Long id);

    /** Resolve the event attribute config by its attribute device event. */
    Mono<EventAttributeConfigBO> getByAttributeDeviceEvent(
            Long tenantId, Long attributeId, Long deviceId, Long eventId);

    /** List event attribute configs matched by device id. */
    Flux<EventAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);

    /** List event attribute configs matched by device id and event id. */
    Flux<EventAttributeConfigBO> listByDeviceIdAndEventId(Long tenantId, Long deviceId, Long eventId);

    /** Insert one event attribute config and emit the stored row. */
    Mono<EventAttributeConfigBO> insert(EventAttributeConfigBO value);

    /** Update one event attribute config and emit the updated row. */
    Mono<EventAttributeConfigBO> update(EventAttributeConfigBO value, int expectedVersion);

    /** Delete the event attribute config, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Page event attribute configs matching the tenant-scoped filters. */
    Mono<OffsetPage<EventAttributeConfigBO>> list(EventAttributeConfigFilter filter);
}
