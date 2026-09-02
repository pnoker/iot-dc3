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

import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.util.List;
import java.util.Set;

/** Tenant-scoped membership list filter. */
public record TenantMembershipFilter(
        Long tenantId,
        Long principalId,
        PrincipalTypeEnum principalType,
        MembershipStatusEnum membershipStatus,
        PageRequest page) {
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "principalId", "principalType", "membershipStatus", "joinedTime", "createTime", "operateTime");

    public TenantMembershipFilter {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenant id is required");
        page = page == null ? PageRequest.firstPage() : page;
        if (page.sort().stream().anyMatch(spec -> spec == null || !SORT_FIELDS.contains(spec.field()))) {
            throw new IllegalArgumentException("unsupported tenant membership sort field");
        }
    }

    public TenantMembershipFilter(
            Long tenantId,
            Long principalId,
            PrincipalTypeEnum principalType,
            MembershipStatusEnum membershipStatus,
            long offset,
            int limit,
            List<SortSpec> sort) {
        this(tenantId, principalId, principalType, membershipStatus, new PageRequest(offset, limit, sort));
    }
}
