package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Tenant-scoped local credential list filter. */
public record LocalCredentialFilter(Long tenantId, Long principalId, String loginName,
                                    CredentialTypeEnum credentialType, EnableFlagEnum enableFlag,
                                    PageRequest page) {

    private static final Set<String> SORT_FIELDS = Set.of("id", "loginName", "credentialType", "enableFlag",
            "passwordUpdatedTime", "createTime", "operateTime");

    public LocalCredentialFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenant id is required");
        page = page == null ? PageRequest.firstPage() : page;
        if (page.sort().stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported local credential sort field");
        }
        loginName = normalize(loginName);
    }

    public LocalCredentialFilter(Long tenantId, Long principalId, String loginName,
                                 CredentialTypeEnum credentialType, EnableFlagEnum enableFlag,
                                 long offset, int limit, List<SortSpec> sort) {
        this(tenantId, principalId, loginName, credentialType, enableFlag,
                new PageRequest(offset, limit, sort));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
