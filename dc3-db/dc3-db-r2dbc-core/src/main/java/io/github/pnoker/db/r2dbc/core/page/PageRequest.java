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

/** Canonical offset pagination request shared by HTTP, gRPC and repositories. */
public record PageRequest(long offset, int limit, List<SortSpec> sort) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    public PageRequest {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        if (offset > Long.MAX_VALUE - limit) {
            throw new IllegalArgumentException("offset plus limit exceeds maximum value");
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public PageRequest(long offset, int limit) {
        this(offset, limit, List.of());
    }

    public static PageRequest firstPage() {
        return new PageRequest(0, DEFAULT_LIMIT);
    }
}
