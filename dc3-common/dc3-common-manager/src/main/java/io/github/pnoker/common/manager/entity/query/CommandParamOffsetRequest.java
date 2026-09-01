package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ParamDirectionTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/** Canonical offset-based command parameter list request. */
@Schema(description = "Command parameter list request using zero-based offset pagination")
public record CommandParamOffsetRequest(
        @Schema(description = "Zero-based result offset.") Long offset,
        @Schema(description = "Maximum result count between 1 and 200.") Integer limit,
        @Schema(description = "Stable sort fields and directions.") List<SortSpec> sort,
        @Schema(description = "Partial parameter name filter.") String paramName,
        @Schema(description = "Exact stable parameter code filter.") String paramCode,
        @Schema(description = "Input or output direction filter.") ParamDirectionTypeEnum paramDirection,
        @Schema(description = "Parameter data type filter.") PointTypeEnum paramTypeFlag,
        @Schema(description = "Parent command identifier filter.") Long commandId,
        @Schema(description = "Enable state filter.") EnableFlagEnum enableFlag,
        @Schema(description = "Optimistic-lock version filter.") Integer version) {
    public CommandParamOffsetRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        Set<String> fields = Set.of("id", "paramName", "paramCode", "commandId", "createTime", "operateTime", "version");
        if (sort.stream().anyMatch(spec -> spec == null || !fields.contains(spec.field()))) throw new IllegalArgumentException("unsupported sort field");
    }
    public CommandParamOffsetRequest() { this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null, null, null); }
}
