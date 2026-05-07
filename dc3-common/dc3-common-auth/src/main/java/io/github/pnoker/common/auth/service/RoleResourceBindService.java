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
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.base.service.BaseService;

import java.util.List;

/**
 * RoleResourceBind Interface
 *
 * @author linys
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface RoleResourceBindService extends BaseService<RoleResourceBindBO, RoleResourceBindQuery> {

	/**
	 * Paginated query filtered by tenant: only bindings whose role belongs to the given
	 * tenant.
	 * @param entityQuery Query conditions
	 * @param tenantId Tenant ID, null means no tenant filtering
	 * @return Paginated bindings
	 */
	Page<RoleResourceBindBO> selectByPage(RoleResourceBindQuery entityQuery, Long tenantId);

	/**
	 * TenantIdUserId
	 * @param roleId id
	 * @return
	 */
	List<ResourceBO> listResourceByRoleId(Long roleId);

	/**
	 * List resources reachable by the given user, via dc3_role_user_bind joined onto
	 * dc3_role_resource_bind. Disabled resources are filtered out.
	 * @param userId target user
	 * @param tenantId tenant scope (constrains the user's roles)
	 * @return distinct enabled resources, empty list if the user has no roles
	 */
	List<ResourceBO> listResourceByUserId(Long userId, Long tenantId);

	/**
	 * Reverse of {@link #listResourceByRoleId} — given a resource, list the enabled roles
	 * that currently grant it. Used by the resource detail page's "Assigned Roles" tab.
	 * @param resourceId target resource
	 * @param tenantId tenant scope (constrains which roles are returned)
	 * @return enabled roles within the tenant; empty list if none
	 */
	List<RoleBO> listRoleByResourceId(Long resourceId, Long tenantId);

}
