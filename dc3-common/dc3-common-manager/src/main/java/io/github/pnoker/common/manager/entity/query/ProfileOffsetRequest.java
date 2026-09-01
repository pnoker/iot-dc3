/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

/** Canonical offset-based profile list request. */
@Schema(description = "Profile list request using zero-based offset pagination")
public record ProfileOffsetRequest(
        @Schema(description = "Zero-based result offset") Long offset,
        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Stable, whitelisted sort fields") List<SortSpec> sort,
        @Schema(description = "Partial profile name filter") String profileName,
        @Schema(description = "Exact profile code filter") String profileCode,
        @Schema(description = "Profile sharing scope filter") ProfileShareTypeEnum profileShareFlag,
        @Schema(description = "Profile origin type filter") ProfileTypeEnum profileTypeFlag,
        @Schema(description = "Enable state filter") EnableFlagEnum enableFlag,
        @Schema(description = "Group identifier filter") Long groupId,
        @Schema(description = "Label identifier filter") Long labelId,
        @Schema(description = "Optimistic-lock version filter") Integer version,
        @Schema(description = "Device identifier filter") Long deviceId) {

    public ProfileOffsetRequest {
        offset = offset == null ? 0L : offset;
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        Set<String> fields = Set.of("id", "profileName", "profileCode", "createTime", "operateTime", "version");
        if (sort.stream().anyMatch(spec -> spec == null || !fields.contains(spec.field()))) throw new IllegalArgumentException("unsupported sort field");
    }

    public ProfileOffsetRequest() {
        this(0L, PageRequest.DEFAULT_LIMIT, List.of(), null, null, null, null, null, null, null, null, null);
    }
}
