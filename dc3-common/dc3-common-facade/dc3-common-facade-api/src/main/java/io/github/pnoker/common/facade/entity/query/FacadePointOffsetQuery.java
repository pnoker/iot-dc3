/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.facade.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Tenant-bound, offset-based point query shared by local and gRPC facades. */
public record FacadePointOffsetQuery(
        @Schema(description = "Tenant identifier") Long tenantId,
        @Schema(description = "Point name filter") String pointName,
        @Schema(description = "Point code filter") String pointCode,
        @Schema(description = "Point type filter") PointTypeEnum pointTypeFlag,
        @Schema(description = "Read/write flag filter") RwTypeEnum rwFlag,
        @Schema(description = "Profile identifier filter") Long profileId,
        @Schema(description = "Enable flag filter") EnableFlagEnum enableFlag,
        @Schema(description = "Group identifier filter") Long groupId,
        @Schema(description = "Label identifier filter") Long labelId,
        @Schema(description = "Metadata version filter") Integer version,
        @Schema(description = "Device identifier filter") Long deviceId,
        @Schema(description = "Zero-based result offset", example = "0") long offset,
        @Schema(description = "Maximum number of items", example = "50") int limit,
        @Schema(description = "Sort expressions") List<SortSpec> sort) {

    public FacadePointOffsetQuery {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public FacadePointOffsetQuery(Long tenantId, long offset, int limit) {
        this(tenantId, null, null, null, null, null, null, null, null, null, null, offset, limit, List.of());
    }
}
