package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "API list request using zero-based offset pagination")
public record ApiOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Owning service name") String serviceName,
        @Schema(description = "HTTP method type filter") ApiTypeEnum apiTypeFlag,
        @Schema(description = "Partial API name") String apiName,
        @Schema(description = "Exact API code") String apiCode,
        @Schema(description = "API group filter") String apiGroup,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag) {
    public ApiOffsetRequest {
        offset = offset == null ? 0L : offset; limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid limit");
    }
    public ApiOffsetRequest() { this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null, null); }
}
