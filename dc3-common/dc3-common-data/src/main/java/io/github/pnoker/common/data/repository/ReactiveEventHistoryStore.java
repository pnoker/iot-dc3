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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped persistence port for event history. */
public interface ReactiveEventHistoryStore {

    /** Insert one event history entry and emit the stored row. */
    Mono<EventHistoryDO> insert(EventHistoryDO event);

    /** Resolve the event history entry by its record id. */
    Mono<EventHistoryDO> findByRecordId(Long tenantId, String recordId);

    /** Page event history entries matching the tenant-scoped filters. */
    Mono<OffsetPage<EventHistoryDO>> list(
            Long tenantId,
            Long deviceId,
            Long eventId,
            String eventCode,
            Byte eventTypeFlag,
            long offset,
            int limit,
            List<SortSpec> sort);
}
