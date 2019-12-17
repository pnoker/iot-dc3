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

package com.pnoker.center.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.User;

/**
 * <p>UserAuthService
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public interface UserAuthService {

    /**
     * 新增 User 记录
     *
     * @param user
     * @return User
     */
    Response<User> add(User user);

    /**
     * 删除 User 记录
     *
     * @param id
     * @return
     */
    Response<Boolean> delete(Long id);

    /**
     * 更新 User 记录
     *
     * @param user
     * @return User
     */
    Response<User> update(User user);

    /**
     * 通过 ID 查询 User 记录
     *
     * @param id
     * @return
     */
    Response<User> selectById(Long id);

    /**
     * 获取带分页、排序的 User 记录
     *
     * @param userDto
     * @return Page<User>
     */
    Response<Page<User>> list(UserDto userDto);

    /**
     * 根据 username 判断 User 是否存在
     *
     * @param username
     * @return Boolean
     */
    Response<Boolean> checkUserExist(String username);
}
