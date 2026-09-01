package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Canonical offset query for topic projections. */
public record TopicOffsetQuery(Long tenantId, String topic, String deviceName, long offset, int limit) {
    public TopicOffsetQuery {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT) throw new IllegalArgumentException("invalid page bounds");
    }
}
