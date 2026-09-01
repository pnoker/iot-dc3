/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Immutable, tenant-bound device list criteria. */
public record DeviceFilter(Long tenantId, String deviceName, String deviceCode, Long driverId,
                           Long profileId, EnableFlagEnum enableFlag, Integer version,
                           Long groupId, Long labelId, long offset, int limit,
                           List<SortSpec> sort) {

    private static final Set<String> SORT_FIELDS = Set.of("id", "deviceName", "deviceCode", "createTime", "operateTime", "version");

    public DeviceFilter(Long tenantId, String deviceName, String deviceCode, Long driverId,
                        Long profileId, EnableFlagEnum enableFlag, long offset, int limit,
                        List<SortSpec> sort) {
        this(tenantId, deviceName, deviceCode, driverId, profileId, enableFlag, null, null, null,
                offset, limit, sort);
    }

    public DeviceFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported device sort field");
        }
    }
}
