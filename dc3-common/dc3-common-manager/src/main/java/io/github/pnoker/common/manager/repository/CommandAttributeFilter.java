package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Tenant-scoped offset query for command attributes. */
public record CommandAttributeFilter(Long tenantId, String attributeName, String attributeCode,
                                     AttributeTypeEnum attributeTypeFlag, Long driverId,
                                     EnableFlagEnum enableFlag, Integer version,
                                     long offset, int limit, List<SortSpec> sort) {
    private static final Set<String> SORT_FIELDS = Set.of("id", "attributeName", "attributeCode",
            "driverId", "createTime", "operateTime", "version");

    public CommandAttributeFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported command attribute sort field");
        }
    }
}
