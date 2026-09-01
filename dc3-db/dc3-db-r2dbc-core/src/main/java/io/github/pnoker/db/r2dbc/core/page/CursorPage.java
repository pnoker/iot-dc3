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
