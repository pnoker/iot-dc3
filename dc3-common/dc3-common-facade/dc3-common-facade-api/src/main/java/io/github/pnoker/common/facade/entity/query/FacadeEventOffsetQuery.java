package io.github.pnoker.common.facade.entity.query;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;

/** Canonical offset query for event facade calls. */
public record FacadeEventOffsetQuery(Long tenantId, String eventName, String eventCode,
                                     EventTypeFlagEnum eventTypeFlag, EventLevelEnum eventLevelFlag,
                                     Long profileId, EnableFlagEnum enableFlag, Integer version,
                                     Long deviceId, long offset, int limit, List<SortSpec> sort) {
    public FacadeEventOffsetQuery {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid page bounds");
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
