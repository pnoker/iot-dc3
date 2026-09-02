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

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import java.util.Set;

/** Immutable, tenant-bound event list criteria. */
public record EventFilter(
        Long tenantId,
        String eventName,
        String eventCode,
        EventTypeFlagEnum eventTypeFlag,
        EventLevelEnum eventLevelFlag,
        Long profileId,
        EnableFlagEnum enableFlag,
        Integer version,
        Long deviceId,
        long offset,
        int limit,
        List<SortSpec> sort) {
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "eventName", "eventCode", "createTime", "operateTime", "version");

    public EventFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT)
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field())))
            throw new IllegalArgumentException("unsupported event sort field");
    }
}
