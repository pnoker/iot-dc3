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

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.base.service.BaseService;

import java.util.List;

/**
 * RoleResourceBind Interface
 *
 * @author linys
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface RoleResourceBindService extends BaseService<RoleResourceBindBO, RoleResourceBindQuery> {

    /**
     * 根据TenantId与UserId查询资源
     *
     * @param roleId 角色id
     * @return 资源列表
     */
    List<ResourceBO> listResourceByRoleId(Long roleId);
}
