package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/** Canonical offset-based point query. Tenant scope is always supplied by the request context. */
public record PointOffsetQuery(
        @Schema(description = "Zero-based result offset", minimum = "0") Long offset,
        @Schema(description = "Maximum number of items", minimum = "1", maximum = "200") Integer limit,
        @Schema(description = "Stable sort fields and directions") List<SortSpec> sort,
        @Schema(description = "Partial point name filter") String pointName,
        @Schema(description = "Exact point code filter") String pointCode,
        @Schema(description = "Point value type") PointTypeEnum pointTypeFlag,
        @Schema(description = "Read/write capability") RwTypeEnum rwFlag,
        @Schema(description = "Profile identifier") Long profileId,
        @Schema(description = "Enable state") EnableFlagEnum enableFlag,
        @Schema(description = "Group identifier") Long groupId,
        @Schema(description = "Label identifier") Long labelId,
        @Schema(description = "Optimistic-lock version") Integer version,
        @Schema(description = "Device identifier") Long deviceId) {

    public PointOffsetQuery {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? 50 : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }
        Set<String> allowedSortFields = Set.of("id", "pointName", "pointCode", "createTime");
        if (sort.stream().anyMatch(spec -> !allowedSortFields.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported sort field");
        }
    }
}
