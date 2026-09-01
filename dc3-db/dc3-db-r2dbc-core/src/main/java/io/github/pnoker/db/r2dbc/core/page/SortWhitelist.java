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
