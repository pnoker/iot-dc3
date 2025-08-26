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

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.common.auth.entity.query.RoleUserBindQuery;
import io.github.pnoker.common.base.service.BaseService;

import java.util.List;

/**
 * RoleUserBind Interface
 *
 * @author linys
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface RoleUserBindService extends BaseService<RoleUserBindBO, RoleUserBindQuery> {

    /**
     * 根据 租户id 和 用户id 查询
     *
     * @param tenantId 租户id
     * @param userId   用户id
     * @return Role list
     */
    List<RoleBO> listRoleByTenantIdAndUserId(Long tenantId, Long userId);
}
