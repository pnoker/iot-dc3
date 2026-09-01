package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Tenant-scoped filters for role-principal bindings. */
public record RolePrincipalBindFilter(Long tenantId, Long roleId, Long principalId,
                                      PrincipalTypeEnum principalType, PageRequest page) {
    public RolePrincipalBindFilter {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenant id is required");
        }
        page = page == null ? PageRequest.firstPage() : page;
    }
}
