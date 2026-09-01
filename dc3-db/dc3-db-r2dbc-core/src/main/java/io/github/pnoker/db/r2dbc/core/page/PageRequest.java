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
