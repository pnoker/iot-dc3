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
package io.github.pnoker.common.facade.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import java.util.Set;

/** Tenant-bound offset query for profile metadata. */
public record FacadeProfileOffsetQuery(
        Long tenantId,
        String profileName,
        String profileCode,
        ProfileShareTypeEnum profileShareFlag,
        ProfileTypeEnum profileTypeFlag,
        EnableFlagEnum enableFlag,
        Long groupId,
        Long labelId,
        Integer version,
        Long deviceId,
        long offset,
        int limit,
        List<SortSpec> sort) {
    public FacadeProfileOffsetQuery {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid page bounds");
        sort = sort == null ? List.of() : List.copyOf(sort);
        Set<String> fields = Set.of("id", "profileName", "profileCode", "createTime", "operateTime", "version");
        if (sort.stream().anyMatch(spec -> spec == null || !fields.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported profile sort field");
        }
    }

    public FacadeProfileOffsetQuery(Long tenantId, long offset, int limit) {
        this(tenantId, null, null, null, null, null, null, null, null, null, offset, limit, List.of());
    }
}
