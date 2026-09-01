package io.github.pnoker.common.facade.entity.query;

import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;

/** Canonical offset query for command facade calls. */
public record FacadeCommandOffsetQuery(Long tenantId, String commandName, String commandCode,
                                       CommandTypeEnum commandTypeFlag, CallTypeEnum callTypeFlag,
                                       Long profileId, EnableFlagEnum enableFlag, Integer version,
                                       Long deviceId, long offset, int limit, List<SortSpec> sort) {
    public FacadeCommandOffsetQuery {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid page bounds");
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
