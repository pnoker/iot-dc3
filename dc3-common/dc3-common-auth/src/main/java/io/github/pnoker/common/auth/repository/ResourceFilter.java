package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.util.List;
import java.util.Objects;

public record ResourceFilter(String resourceName, String resourceCode, List<ResourceTypeEnum> resourceTypeFlags,
                             List<ResourceScopeTypeEnum> resourceScopeFlags, Long parentResourceId,
                             EnableFlagEnum enableFlag, PageRequest page) {
    public ResourceFilter {
        page = page == null ? PageRequest.firstPage() : page;
        resourceName = normalize(resourceName);
        resourceCode = normalize(resourceCode);
        resourceTypeFlags = resourceTypeFlags == null ? List.of()
                : resourceTypeFlags.stream().filter(Objects::nonNull).distinct().toList();
        resourceScopeFlags = resourceScopeFlags == null ? List.of()
                : resourceScopeFlags.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
