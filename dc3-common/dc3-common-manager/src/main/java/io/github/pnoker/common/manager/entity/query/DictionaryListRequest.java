package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/** Canonical offset request for read-only manager options. */
@Schema(description = "Manager dictionary request using zero-based offset pagination")
public record DictionaryListRequest(
        @Schema(description = "Zero-based result offset", example = "0") Long offset,
        @Schema(description = "Maximum number of options", example = "50") Integer limit,
        @Schema(description = "Stable option sort fields: label or value") List<SortSpec> sort,
        @Schema(description = "Partial display-label filter") String label,
        @Schema(description = "Parent entity identifier for dependent options") Long parentId) {

    private static final Set<String> SORT_FIELDS = Set.of("label", "value");

    public DictionaryListRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported dictionary sort field");
        }
    }

    public DictionaryListRequest() {
        this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null);
    }

}
