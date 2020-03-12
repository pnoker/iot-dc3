/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pnoker.center.auth.service;

import com.github.pnoker.common.base.Service;
import com.github.pnoker.common.dto.UserDto;
import com.github.pnoker.common.model.User;

/**
 * User Interface
 *
 * @author pnoker
 */
public interface UserService extends Service<User, UserDto> {

    /**
     * 根据用户名查询用户
     *
     * @param nama
     * @return User
     */
    User selectByName(String nama);

    /**
     * 根据用户名判断用户是否存在
     *
     * @param name
     * @return boolean
     */
    boolean checkUserValid(String name);

    /**
     * 重置密码
     *
     * @param id
     * @return boolean
     */
    boolean restPassword(Long id);
}
