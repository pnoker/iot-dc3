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
import java.util.Objects;

public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

    public CursorPage {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (hasNext && (nextCursor == null || nextCursor.isBlank())) {
            throw new IllegalArgumentException("nextCursor is required when hasNext is true");
        }
        if (hasNext && items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty when hasNext is true");
        }
        if (!hasNext && nextCursor != null) {
            throw new IllegalArgumentException("nextCursor must be null when hasNext is false");
        }
    }

    public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
        if (nextCursor == null || nextCursor.isBlank()) {
            return new CursorPage<>(items, null, false);
        }
        return new CursorPage<>(items, nextCursor, true);
    }
}
