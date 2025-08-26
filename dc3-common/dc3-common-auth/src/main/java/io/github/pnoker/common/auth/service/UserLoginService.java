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
 * UserLogin Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface UserLoginService extends BaseService<UserLoginBO, UserLoginQuery> {

    /**
     * 根据登录名称查询用户
     *
     * @param loginName      登录名称
     * @param throwException Throw Exception
     * @return User
     */
    UserLoginBO selectByLoginName(String loginName, boolean throwException);

    /**
     * 判断登录名称是否有效
     *
     * @param loginName 登录名称
     * @return Boolean
     */
    boolean checkLoginNameValid(String loginName);
}
