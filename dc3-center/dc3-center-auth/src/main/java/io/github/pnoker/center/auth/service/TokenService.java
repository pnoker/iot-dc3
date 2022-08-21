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

import io.github.pnoker.center.auth.bean.TokenValid;

/**
 * Token Interface
 *
 * @author pnoker
 */
public interface TokenService {
    /**
     * 生成用户的随机 salt
     *
     * @param username   Username
     * @param tenantName Tenant Name
     * @return String
     */
    String generateSalt(String username, String tenantName);

    /**
     * 生成用户的Token令牌
     *
     * @param username       User Name
     * @param salt       User Salt
     * @param password   User Password
     * @param tenantName Tenant Name
     * @return String
     */
    String generateToken(String username, String salt, String password, String tenantName);

    /**
     * 校验用户的Token令牌是否有效
     *
     * @param username   Username
     * @param token      Token
     * @param tenantName Tenant Name
     * @return TokenValid
     */
    TokenValid checkTokenValid(String username, String salt, String token, String tenantName);

    /**
     * 注销用户的Token令牌
     *
     * @param username   Username
     * @param tenantName Tenant Name
     */
    boolean cancelToken(String username, String tenantName);
}
