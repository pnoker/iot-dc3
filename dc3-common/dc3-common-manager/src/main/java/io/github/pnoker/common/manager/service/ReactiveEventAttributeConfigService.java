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

import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.repository.EventAttributeConfigFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveEventAttributeConfigService {
    Mono<EventAttributeConfigBO> add(EventAttributeConfigBO value);

    Mono<EventAttributeConfigBO> update(EventAttributeConfigBO value);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    Mono<EventAttributeConfigBO> getById(Long tenantId, Long id);

    Mono<EventAttributeConfigBO> getByAttributeIdAndDeviceIdAndEventId(
            Long tenantId, Long attributeId, Long deviceId, Long eventId);

    Flux<EventAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);

    Flux<EventAttributeConfigBO> listByDeviceIdAndEventId(Long tenantId, Long deviceId, Long eventId);

    Mono<OffsetPage<EventAttributeConfigBO>> list(EventAttributeConfigFilter filter);
}
