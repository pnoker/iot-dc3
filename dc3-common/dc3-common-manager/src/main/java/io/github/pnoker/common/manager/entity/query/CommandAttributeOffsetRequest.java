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
package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

/** Canonical offset-based command attribute list request. */
@Schema(description = "Command attribute list request using zero-based offset pagination")
public record CommandAttributeOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,

        @Schema(description = "Stable, whitelisted sort fields")
        List<SortSpec> sort,

        @Schema(description = "Partial attribute name filter")
        String attributeName,

        @Schema(description = "Exact attribute code filter") String attributeCode,
        @Schema(description = "Attribute data type filter") AttributeTypeEnum attributeTypeFlag,
        @Schema(description = "Driver identifier filter") Long driverId,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag,

        @Schema(description = "Optimistic-lock version filter")
        Integer version) {
    public CommandAttributeOffsetRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT)
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        Set<String> fields =
                Set.of("id", "attributeName", "attributeCode", "driverId", "createTime", "operateTime", "version");
        if (sort.stream().anyMatch(spec -> spec == null || !fields.contains(spec.field())))
            throw new IllegalArgumentException("unsupported sort field");
    }

    public CommandAttributeOffsetRequest() {
        this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null, null);
    }
}
