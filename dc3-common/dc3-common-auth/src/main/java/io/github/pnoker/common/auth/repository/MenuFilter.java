package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MenuTypeFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Normalized menu filter with canonical offset pagination. */
public record MenuFilter(Long parentMenuId, MenuTypeFlagEnum menuTypeFlag, String menuName,
                         String menuCode, EnableFlagEnum enableFlag, PageRequest page) {
    public MenuFilter {
        page = page == null ? PageRequest.firstPage() : page;
        menuName = normalize(menuName);
        menuCode = normalize(menuCode);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
