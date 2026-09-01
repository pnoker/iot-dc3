package io.github.pnoker.common.auth.security;

/** Invalidates effective-permission snapshots after RBAC mutations. */
public interface PermissionCacheInvalidator {
    void invalidate(Long tenantId, Long principalId);
    void invalidateTenant(Long tenantId);
    void invalidateAll();
}
