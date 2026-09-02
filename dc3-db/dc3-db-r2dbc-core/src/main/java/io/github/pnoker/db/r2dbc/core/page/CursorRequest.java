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
package io.github.pnoker.db.r2dbc.core.page;

import java.util.List;

/** Bounded keyset pagination request. A cursor is opaque to callers. */
public record CursorRequest(String cursor, int limit, List<SortSpec> sort) {

    public CursorRequest {
        if (cursor != null && cursor.isBlank()) {
            cursor = null;
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public CursorRequest(String cursor, int limit) {
        this(cursor, limit, List.of());
    }

    public static CursorRequest firstPage() {
        return new CursorRequest(null, PageRequest.DEFAULT_LIMIT);
    }
}
