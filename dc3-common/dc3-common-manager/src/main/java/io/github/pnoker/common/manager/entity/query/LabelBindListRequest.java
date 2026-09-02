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

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Canonical offset list request for label bindings. */
public record LabelBindListRequest(
        @Schema(description = "Zero-based number of records to skip", example = "0")
        long offset,

        @Schema(description = "Maximum number of records to return", example = "20")
        int limit,

        @Schema(description = "Ordered sort fields; each field must be supported by the endpoint")
        List<SortSpec> sort,

        @Schema(description = "Optional entity type filter", example = "DEVICE")
        EntityTypeEnum entityTypeFlag,

        @Schema(description = "Optional label identifier filter", example = "100")
        Long labelId,

        @Schema(description = "Optional bound entity identifier filter", example = "200")
        Long entityId) {
    public LabelBindListRequest {
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public LabelBindListRequest() {
        this(0, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null);
    }
}
