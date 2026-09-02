/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.util.List;
import java.util.Objects;

public record ResourceFilter(
        String resourceName,
        String resourceCode,
        List<ResourceTypeEnum> resourceTypeFlags,
        List<ResourceScopeTypeEnum> resourceScopeFlags,
        Long parentResourceId,
        EnableFlagEnum enableFlag,
        PageRequest page) {
    public ResourceFilter {
        page = page == null ? PageRequest.firstPage() : page;
        resourceName = normalize(resourceName);
        resourceCode = normalize(resourceCode);
        resourceTypeFlags = resourceTypeFlags == null
                ? List.of()
                : resourceTypeFlags.stream().filter(Objects::nonNull).distinct().toList();
        resourceScopeFlags = resourceScopeFlags == null
                ? List.of()
                : resourceScopeFlags.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
