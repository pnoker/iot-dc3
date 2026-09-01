package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Immutable tenant-bound point list criteria. */
public record PointFilter(Long tenantId, String pointName, String pointCode, PointTypeEnum pointTypeFlag,
                          RwTypeEnum rwFlag, Long profileId, EnableFlagEnum enableFlag,
                          Long groupId, Long labelId, Integer version, Long deviceId,
                          long offset, int limit, List<SortSpec> sort) {
    private static final Set<String> SORT_FIELDS = Set.of("id", "pointName", "pointCode", "createTime", "operateTime", "version");
    public PointFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
        if (offset < 0 || limit < 1 || limit > 200) throw new IllegalArgumentException("invalid page bounds");
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) throw new IllegalArgumentException("unsupported point sort field");
    }
}
