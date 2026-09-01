/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Canonical offset-based driver list request. */
@Schema(description = "Driver list request using zero-based offset pagination")
public record DriverListRequest(
        @Schema(description = "Zero-based result offset", example = "0") long offset,
        @Schema(description = "Maximum number of records", example = "50") int limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Partial driver name filter") String driverName,
        @Schema(description = "Exact driver code filter") String driverCode,
        @Schema(description = "Exact service name filter") String serviceName,
        @Schema(description = "Exact service host filter") String serviceHost,
        @Schema(description = "Driver type filter") DriverTypeEnum driverTypeFlag,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag,
        @Schema(description = "Optimistic-lock version filter") Integer version,
        @Schema(description = "Group identifier filter") Long groupId,
        @Schema(description = "Label identifier filter") Long labelId) {

    public DriverListRequest {
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public DriverListRequest() { this(0, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null, null, null, null, null); }
}
