package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Tenant-scoped immutable user filter. */
public record UserFilter(Long tenantId, Long principalId, String nickName, String userName, String phone,
                         String email, EnableFlagEnum enableFlag, PageRequest page) {

    private static final Set<String> SORT_FIELDS = Set.of("id", "userName", "nickName", "phone", "email",
            "createTime", "operateTime");

    public UserFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenant id is required");
        page = page == null ? PageRequest.firstPage() : page;
        if (page.sort().stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported user sort field");
        }
        nickName = normalize(nickName);
        userName = normalize(userName);
        phone = normalize(phone);
        email = normalize(email);
    }

    public UserFilter(Long tenantId, Long principalId, String nickName, String userName, String phone,
                      String email, EnableFlagEnum enableFlag, long offset, int limit, List<SortSpec> sort) {
        this(tenantId, principalId, nickName, userName, phone, email, enableFlag,
                new PageRequest(offset, limit, sort));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
