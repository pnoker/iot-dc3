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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Validates client supplied sort fields before they are interpolated into SQL. */
public final class SortWhitelist {

    private final Set<String> fields;
    private final SortSpec defaultSort;

    public SortWhitelist(Set<String> fields, SortSpec defaultSort) {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("sort whitelist must not be empty");
        }
        this.fields = Set.copyOf(new LinkedHashSet<>(fields));
        this.defaultSort = Objects.requireNonNull(defaultSort, "default sort must not be null");
        if (!this.fields.contains(defaultSort.field())) {
            throw new IllegalArgumentException("default sort field is not whitelisted");
        }
    }

    public List<SortSpec> validate(List<SortSpec> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of(defaultSort);
        }
        Set<String> seen = new LinkedHashSet<>();
        for (SortSpec sort : requested) {
            if (sort == null || !fields.contains(sort.field())) {
                throw new IllegalArgumentException("sort field is not allowed");
            }
            if (!seen.add(sort.field())) {
                throw new IllegalArgumentException("sort field must not be repeated");
            }
        }
        return List.copyOf(requested);
    }
}
