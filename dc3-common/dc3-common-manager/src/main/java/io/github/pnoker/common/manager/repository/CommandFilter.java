package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Immutable, tenant-bound command list criteria. */
public record CommandFilter(Long tenantId, String commandName, String commandCode,
                            CommandTypeEnum commandTypeFlag, CallTypeEnum callTypeFlag,
                            Long profileId, EnableFlagEnum enableFlag, Integer version,
                            Long deviceId, long offset, int limit, List<SortSpec> sort) {
    private static final Set<String> SORT_FIELDS = Set.of("id", "commandName", "commandCode", "createTime", "operateTime", "version");

    public CommandFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        sort = sort == null ? List.of() : List.copyOf(sort);
        if (sort.stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) throw new IllegalArgumentException("unsupported command sort field");
    }
}
