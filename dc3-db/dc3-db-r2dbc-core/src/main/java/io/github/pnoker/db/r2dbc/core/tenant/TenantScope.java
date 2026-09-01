package io.github.pnoker.db.r2dbc.core.tenant;

public record TenantScope(Long tenantId) {

    public TenantScope {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
    }
}
