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
/** Offset-paged result slice with total count. */

public record OffsetPage<T>(List<T> items, long offset, int limit, long total, boolean hasNext) {

    public OffsetPage {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must be non-negative");
        }
        if (items.size() > limit) {
            throw new IllegalArgumentException("items must not exceed limit");
        }
        boolean expectedHasNext = offset <= Long.MAX_VALUE - items.size() && offset + items.size() < total;
        if (hasNext != expectedHasNext) {
            throw new IllegalArgumentException("hasNext does not match page bounds");
        }
    }

    /** Create the page from the result window. */
    public static <T> OffsetPage<T> of(List<T> items, long offset, int limit, long total) {
        List<T> safeItems = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        boolean hasNext = offset <= Long.MAX_VALUE - safeItems.size() && offset + safeItems.size() < total;
        return new OffsetPage<>(safeItems, offset, limit, total, hasNext);
    }
}
