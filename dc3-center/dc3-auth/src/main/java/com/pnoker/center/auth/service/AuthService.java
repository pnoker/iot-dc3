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
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.dto.auth.UserDto;
import com.pnoker.common.entity.auth.Token;
import com.pnoker.common.entity.auth.User;

/**
 * <p>Auth 服务接口
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public interface AuthService {

    /**
     * 新增记录
     *
     * @param user
     * @return true/false
     */
    User add(User user);

    /**
     * 删除记录
     *
     * @param id
     * @return true/false
     */
    boolean delete(Long id);

    /**
     * 更新记录
     *
     * @param user
     * @return true/false
     */
    boolean update(User user);

    /**
     * 通过ID查询记录
     *
     * @param id
     * @return type
     */
    User selectById(Long id);

    /**
     * 获取带分页、排序的记录
     *
     * @param userDto
     * @return list
     */
    Page<User> list(UserDto userDto);

    /**
     * 判断是用户否存在
     *
     * @param username
     * @return true/false
     */
    boolean checkUserExist(String username);

    /**
     * 判断Token令牌是否有效
     *
     * @param token
     * @return true/false
     */
    boolean checkTokenValid(Token token);

    /**
     * 生成Token令牌
     *
     * @param user
     * @return tokenDto
     */
    TokenDto generateToken(User user);

}
