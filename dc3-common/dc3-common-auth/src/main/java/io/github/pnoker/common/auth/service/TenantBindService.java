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

import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.query.TenantBindQuery;
import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.exception.NotFoundException;

import java.util.List;
import java.util.Objects;

/**
 * Business service for tenant binding operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface TenantBindService extends BaseService<TenantBindBO, TenantBindQuery> {

    /**
     * Tenant ID ID
     *
     * @param tenantId Tenant ID
     * @param userId   User ID
     * @return TenantBind
     */
    TenantBindBO getByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Check whether a user belongs to a tenant.
     *
     * @param tenantId Tenant ID
     * @param userId   User ID
     * @return true when the user is bound to the tenant
     */
    default boolean isTenantMember(Long tenantId, Long userId) {
        return Objects.nonNull(getByTenantIdAndUserId(tenantId, userId));
    }

    /**
     * Fail closed when a requested user is outside the caller's tenant. Returning 404
     * avoids revealing whether the user exists in another tenant.
     *
     * @param tenantId Tenant ID
     * @param userId   User ID
     */
    default void requireTenantMember(Long tenantId, Long userId) {
        if (!isTenantMember(tenantId, userId)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * List active user IDs bound to the given tenant.
     *
     * @param tenantId tenant scope; must be non-null
     * @return user IDs (empty if the tenant has no members yet)
     */
    List<Long> listUserIdsByTenantId(Long tenantId);

}
