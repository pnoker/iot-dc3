package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PrincipalSourceTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;

import java.util.List;
import java.util.Set;

/** Immutable filter for the global principal catalog. */
public record PrincipalFilter(PrincipalTypeEnum principalType, String principalName, String displayName,
                              PrincipalSourceTypeEnum sourceType, EnableFlagEnum enableFlag,
                              PageRequest page) {

    private static final Set<String> SORT_FIELDS = Set.of("id", "principalName", "displayName", "principalType",
            "sourceType", "lastLoginTime", "createTime", "operateTime");

    public PrincipalFilter {
        page = page == null ? PageRequest.firstPage() : page;
        if (page.sort().stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported principal sort field");
        }
        principalName = normalize(principalName);
        displayName = normalize(displayName);
    }

    public PrincipalFilter(PrincipalTypeEnum principalType, String principalName, String displayName,
                           PrincipalSourceTypeEnum sourceType, EnableFlagEnum enableFlag,
                           long offset, int limit, List<SortSpec> sort) {
        this(principalType, principalName, displayName, sourceType, enableFlag,
                new PageRequest(offset, limit, sort));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
