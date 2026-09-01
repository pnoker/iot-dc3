package io.github.pnoker.common.facade.entity.query;

import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Canonical offset query for driver facade calls. */
public record FacadeDriverOffsetQuery(
        @Schema(description = "Tenant identifier") Long tenantId,
        @Schema(description = "Driver name filter") String driverName,
        @Schema(description = "Driver code filter") String driverCode,
        @Schema(description = "Service name filter") String serviceName,
        @Schema(description = "Service host filter") String serviceHost,
        @Schema(description = "Driver type filter") DriverTypeEnum driverTypeFlag,
        @Schema(description = "Enable flag filter") EnableFlagEnum enableFlag,
        @Schema(description = "Metadata version filter") Integer version,
        @Schema(description = "Group identifier filter") Long groupId,
        @Schema(description = "Label identifier filter") Long labelId,
        @Schema(description = "Zero-based result offset", example = "0") long offset,
        @Schema(description = "Maximum number of items", example = "50") int limit,
        @Schema(description = "Sort expressions") List<SortSpec> sort) {
    public FacadeDriverOffsetQuery {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid page bounds");
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
