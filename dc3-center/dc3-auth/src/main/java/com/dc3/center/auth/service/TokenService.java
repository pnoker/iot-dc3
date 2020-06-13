/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.center.auth.service;

import com.dc3.common.model.User;

/**
 * Token Interface
 *
 * @author pnoker
 */
public interface TokenService {
    /**
     * 生成用户的随机 salt，5分钟失效
     *
     * @param username
     * @return
     */
    String generateSalt(String username);

    /**
     * 生成用户的Token令牌，5小时失效
     *
     * @param user
     * @return
     */
    String generateToken(User user);

    /**
     * 校验用户的Token令牌是否有效
     *
     * @param username
     * @param token
     * @return
     */
    boolean checkTokenValid(String username, String token);

    /**
     * 注销用户的Token令牌
     *
     * @param username
     */
    boolean cancelToken(String username);
}
