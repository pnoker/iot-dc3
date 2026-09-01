package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Tenant-scoped role filters with offset pagination. */
public record RoleFilter(Long tenantId, String roleName, String roleCode, EnableFlagEnum enableFlag, PageRequest page) {
    public RoleFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenant id is required");
        page = page == null ? PageRequest.firstPage() : page;
        roleName = normalize(roleName); roleCode = normalize(roleCode);
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
