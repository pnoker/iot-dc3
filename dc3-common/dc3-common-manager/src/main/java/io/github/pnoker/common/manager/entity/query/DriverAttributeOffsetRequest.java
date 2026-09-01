package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/** Canonical offset-based driver attribute list request. */
@Schema(description = "Command attribute list request using zero-based offset pagination")
public record DriverAttributeOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Partial attribute name filter") String attributeName,
        @Schema(description = "Exact attribute code filter") String attributeCode,
        @Schema(description = "Attribute data type filter") AttributeTypeEnum attributeTypeFlag,
        @Schema(description = "Driver identifier filter") Long driverId,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag,
        @Schema(description = "Optimistic-lock version filter") Integer version) {
    public DriverAttributeOffsetRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        Set<String> fields = Set.of("id", "attributeName", "attributeCode", "driverId", "createTime", "operateTime", "version");
        if (sort.stream().anyMatch(spec -> spec == null || !fields.contains(spec.field()))) throw new IllegalArgumentException("unsupported sort field");
    }
    public DriverAttributeOffsetRequest() { this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null, null); }
}
