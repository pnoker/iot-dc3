package io.github.pnoker.db.r2dbc.core.page;

import java.util.List;
import java.util.Objects;

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
        boolean expectedHasNext = offset <= Long.MAX_VALUE - items.size()
                && offset + items.size() < total;
        if (hasNext != expectedHasNext) {
            throw new IllegalArgumentException("hasNext does not match page bounds");
        }
    }

    public static <T> OffsetPage<T> of(List<T> items, long offset, int limit, long total) {
        List<T> safeItems = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        boolean hasNext = offset <= Long.MAX_VALUE - safeItems.size()
                && offset + safeItems.size() < total;
        return new OffsetPage<>(safeItems, offset, limit, total, hasNext);
    }
}
