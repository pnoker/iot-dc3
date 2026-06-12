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
 * Business service for user operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface UserService extends BaseService<UserBO, UserQuery> {

    /**
     * Get user by username.
     *
     * @param userName       username
     * @param throwException whether to throw exception when user does not exist
     * @return {@link UserBO} or {@code null} when not found and {@code throwException} is
     * false
     */
    UserBO getByUserName(String userName, boolean throwException);

    /**
     * Get user by phone number.
     *
     * @param phone          phone number
     * @param throwException whether to throw exception when user does not exist
     * @return {@link UserBO} or {@code null} when not found and {@code throwException} is
     * false
     */
    UserBO getByPhone(String phone, boolean throwException);

    /**
     * Get user by email.
     *
     * @param email          email address
     * @param throwException whether to throw exception when user does not exist
     * @return {@link UserBO} or {@code null} when not found and {@code throwException} is
     * false
     */
    UserBO getByEmail(String email, boolean throwException);

    /**
     * Get user by principal ID.
     *
     * @param principalId    principal ID
     * @param throwException whether to throw exception when user does not exist
     * @return {@link UserBO} or {@code null} when not found and {@code throwException}
     * is false
     */
    UserBO getByPrincipalId(Long principalId, boolean throwException);

}
