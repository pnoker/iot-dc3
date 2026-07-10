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
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.query.RolePrincipalBindQuery;
import io.github.pnoker.common.base.service.BaseService;

import java.util.List;

/**
 * Business service for role-principal bindings.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface RolePrincipalBindService extends BaseService<RolePrincipalBindBO, RolePrincipalBindQuery> {

    /**
     * Paginated query filtered by tenant: only bindings whose role belongs to the given
     * tenant.
     *
     * @param entityQuery query conditions
     * @param tenantId    tenant id, null means no tenant filtering
     * @return paginated bindings
     */
    Page<RolePrincipalBindBO> list(RolePrincipalBindQuery entityQuery, Long tenantId);

    List<RoleBO> listRoleByTenantIdAndPrincipalId(Long tenantId, Long principalId);

    List<UserBO> listUserByRoleId(Long roleId);

}
