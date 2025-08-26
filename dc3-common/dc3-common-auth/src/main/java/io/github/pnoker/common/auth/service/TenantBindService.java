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

/**
 * TenantBind Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface TenantBindService extends BaseService<TenantBindBO, TenantBindQuery> {

    /**
     * 根据 租户ID 和 关联的用户ID 查询
     *
     * @param tenantId 租户ID
     * @param userId   User ID
     * @return TenantBind
     */
    TenantBindBO selectByTenantIdAndUserId(Long tenantId, Long userId);
}
