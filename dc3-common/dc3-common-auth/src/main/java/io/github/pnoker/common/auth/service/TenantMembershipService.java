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

package io.github.pnoker.common.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.query.TenantMembershipQuery;
import io.github.pnoker.common.exception.NotFoundException;

import java.util.List;
import java.util.Objects;

/**
 * Business service for tenant memberships.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface TenantMembershipService {

    void add(TenantMembershipBO membership);

    void delete(Long id);

    TenantMembershipBO getByTenantIdAndPrincipalId(Long tenantId, Long principalId);

    List<Long> listPrincipalIdsByTenantId(Long tenantId);

    Page<TenantMembershipBO> list(TenantMembershipQuery entityQuery);

    TenantMembershipBO getById(Long id);

    default boolean isTenantMember(Long tenantId, Long principalId) {
        return Objects.nonNull(getByTenantIdAndPrincipalId(tenantId, principalId));
    }

    default void requireTenantMember(Long tenantId, Long principalId) {
        if (!isTenantMember(tenantId, principalId)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}
