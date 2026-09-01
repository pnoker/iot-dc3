package io.github.pnoker.common.auth.repository;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Tenant-scoped filters for identity audit cursor reads. */
public record IdentityAuditLogFilter(Long tenantId, Long principalId, String action,
                                     String resourceType, Long resourceId, String status,
                                     String cursor, int limit) {

    public IdentityAuditLogFilter {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenant id is required");
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
    }
}
