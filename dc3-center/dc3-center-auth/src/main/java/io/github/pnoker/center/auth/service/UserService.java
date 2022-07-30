/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service;

import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.dto.UserDto;
import io.github.pnoker.common.model.User;

/**
 * User Interface
 *
 * @author pnoker
 */
public interface UserService extends Service<User, UserDto> {

    /**
     * 根据用户名查询用户
     *
     * @param name Username
     * @param isEx Throw Exception
     * @return User
     */
    User selectByName(String name, boolean isEx);

    /**
     * 根据手机号查询用户
     *
     * @param phone Phone
     * @param isEx  Throw Exception
     * @return User
     */
    User selectByPhone(String phone, boolean isEx);

    /**
     * 根据邮箱查询用户
     *
     * @param email Email
     * @param isEx  Throw Exception
     * @return User
     */
    User selectByEmail(String email, boolean isEx);

    /**
     * 根据用户名判断用户是否存在
     *
     * @param name Username
     * @return boolean
     */
    boolean checkUserValid(String name);

    /**
     * 重置密码
     *
     * @param id Id
     * @return boolean
     */
    boolean restPassword(String id);
}
