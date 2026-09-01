package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resource list request using zero-based offset pagination")
public record ResourceOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Partial resource name") String resourceName,
        @Schema(description = "Exact resource code") String resourceCode,
        @Schema(description = "Resource type filter") ResourceTypeEnum resourceTypeFlag,
        @Schema(description = "Resource type multi-select filter") List<ResourceTypeEnum> resourceTypeFlags,
        @Schema(description = "Resource scope filter") ResourceScopeTypeEnum resourceScopeFlag,
        @Schema(description = "Resource scope multi-select filter") List<ResourceScopeTypeEnum> resourceScopeFlags,
        @Schema(description = "Parent resource identifier") Long parentResourceId,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag) {

    public ResourceOffsetRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        resourceTypeFlags = resourceTypeFlags == null ? List.of() : List.copyOf(resourceTypeFlags);
        resourceScopeFlags = resourceScopeFlags == null ? List.of() : List.copyOf(resourceScopeFlags);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
    }

    public ResourceOffsetRequest() {
        this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, List.of(), null, List.of(), null, null);
    }
}
