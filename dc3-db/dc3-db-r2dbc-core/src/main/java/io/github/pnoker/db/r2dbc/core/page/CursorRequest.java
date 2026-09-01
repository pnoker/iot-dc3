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
