package io.github.pnoker.common.auth.repository;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Tenant-scoped filters for role-resource bindings. */
public record RoleResourceBindFilter(Long tenantId, Long roleId, Long resourceId, PageRequest page) {
    public RoleResourceBindFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenant id is required");
        page = page == null ? PageRequest.firstPage() : page;
    }
}
