package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Canonical offset-based label list request. */
@Schema(description = "Label list request using zero-based offset pagination")
public record LabelListRequest(
        @Schema(description = "Zero-based number of records to skip", example = "0") long offset,
        @Schema(description = "Maximum number of records to return", example = "20") int limit,
        @Schema(description = "Ordered sort fields; each field must be supported by the endpoint") List<SortSpec> sort,
        @Schema(description = "Optional label name filter", example = "critical") String labelName,
        @Schema(description = "Optional label color filter", example = "#ff0000") String color,
        @Schema(description = "Optional entity type filter", example = "DEVICE") EntityTypeEnum entityTypeFlag,
        @Schema(description = "Optional enabled-state filter", example = "ENABLE") EnableFlagEnum enableFlag) {
    public LabelListRequest {
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public LabelListRequest() {
        this(0, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null);
    }
}
