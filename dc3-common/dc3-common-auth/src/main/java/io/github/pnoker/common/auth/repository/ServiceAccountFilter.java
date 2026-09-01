package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Tenant-scoped service-account filters using offset pagination. */
public record ServiceAccountFilter(Long tenantId, Long principalId, String serviceAccountName,
                                   Long ownerPrincipalId, EnableFlagEnum enableFlag,
                                   PageRequest page) {
    public ServiceAccountFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenant id is required");
        page = page == null ? PageRequest.firstPage() : page;
        serviceAccountName = normalize(serviceAccountName);
    }
    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
