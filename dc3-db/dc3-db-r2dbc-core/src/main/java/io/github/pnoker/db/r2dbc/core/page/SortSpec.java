package io.github.pnoker.db.r2dbc.core.page;

import java.util.Objects;

public record SortSpec(String field, Direction direction) {

    public enum Direction {
        ASC,
        DESC
    }

    public SortSpec {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("sort field must not be blank");
        }
        direction = Objects.requireNonNull(direction, "sort direction must not be null");
    }
}
