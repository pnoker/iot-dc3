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

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.base.service.BaseService;

/**
 * User Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface UserService extends BaseService<UserBO, UserQuery> {

    /**
     * 根据用户名称查询用户
     *
     * @param userName       用户名称
     * @param throwException Throw Exception
     * @return User
     */
    UserBO selectByUserName(String userName, boolean throwException);

    /**
     * 根据手机号查询用户
     *
     * @param phone          Phone
     * @param throwException Throw Exception
     * @return User
     */
    UserBO selectByPhone(String phone, boolean throwException);

    /**
     * 根据邮箱查询用户
     *
     * @param email          Email
     * @param throwException Throw Exception
     * @return User
     */
    UserBO selectByEmail(String email, boolean throwException);
}
