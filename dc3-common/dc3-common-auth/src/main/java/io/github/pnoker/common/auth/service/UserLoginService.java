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

import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.query.UserLoginQuery;
import io.github.pnoker.common.base.service.BaseService;

/**
 * Business service for user login record operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface UserLoginService extends BaseService<UserLoginBO, UserLoginQuery> {

    /**
     * Name
     *
     * @param loginName      Name
     * @param throwException Throw Exception
     * @return User
     */
    UserLoginBO getByLoginName(String loginName, boolean throwException);

    /**
     * Name
     *
     * @param loginName Name
     * @return Boolean
     */
    boolean isLoginNameValid(String loginName);

    /**
     * Check whether the login name is available (not yet taken) within
     * the given tenant. Returns {@code true} when the name is free to use.
     *
     * @param loginName login name to check
     * @param tenantId  tenant scope
     * @return {@code true} if the name is not yet used in this tenant
     */
    boolean isLoginNameAvailable(String loginName, Long tenantId);

}
