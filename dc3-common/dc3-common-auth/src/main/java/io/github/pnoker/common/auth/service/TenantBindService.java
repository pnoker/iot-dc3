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

import java.util.List;

/**
 * TenantBind Interface
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
     * List active user IDs bound to the given tenant.
     *
     * @param tenantId tenant scope; must be non-null
     * @return user IDs (empty if the tenant has no members yet)
     */
    List<Long> listUserIdsByTenantId(Long tenantId);

}
