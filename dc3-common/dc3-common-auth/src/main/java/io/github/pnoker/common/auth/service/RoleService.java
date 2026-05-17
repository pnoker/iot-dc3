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
import io.github.pnoker.common.auth.entity.bo.RoleTreeBO;
import io.github.pnoker.common.auth.entity.query.RoleQuery;
import io.github.pnoker.common.base.service.BaseService;

import java.util.List;

/**
 * Role Interface
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface RoleService extends BaseService<RoleBO, RoleQuery> {

    /**
     * Assemble the tenant's role hierarchy as a single nested tree. Top-level roles
     * (parent_role_id == 0 or null) land at the root.
     *
     * @param entityQuery optional filters (tenantId is populated by the controller)
     * @return root nodes, each node carrying its children
     */
    List<RoleTreeBO> selectTree(RoleQuery entityQuery);

}
