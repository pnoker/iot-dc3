/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Immutable, tenant-bound driver list criteria. */
public record DriverFilter(Long tenantId, String driverName, String driverCode, String serviceName,
                           String serviceHost, DriverTypeEnum driverTypeFlag, EnableFlagEnum enableFlag,
                           Integer version, Long groupId, Long labelId, long offset, int limit,
                           List<SortSpec> sort) {

    private static final Set<String> SORT_FIELDS = Set.of("id", "driverName", "driverCode", "serviceName", "serviceHost", "createTime", "operateTime", "version");

    public DriverFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported driver sort field");
        }
    }
}
