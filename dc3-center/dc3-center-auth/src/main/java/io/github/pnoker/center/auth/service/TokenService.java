/*
 * Copyright 2016-present the original author or authors.
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

import io.github.pnoker.center.auth.entity.bean.TokenValid;

/**
 * Token Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface TokenService {
    /**
     * 生成用户的随机 salt
     *
     * @param username   用户名称
     * @param tenantName 租户名称
     * @return String
     */
    String generateSalt(String username, String tenantName);

    /**
     * 生成用户的Token令牌
     *
     * @param username   用户名称
     * @param salt       User Salt
     * @param password   User Password
     * @param tenantName 租户名称
     * @return String
     */
    String generateToken(String username, String salt, String password, String tenantName);

    /**
     * 校验用户的Token令牌是否有效
     *
     * @param username   用户名称
     * @param salt       盐值
     * @param token      Token
     * @param tenantName 租户名称
     * @return TokenValid
     */
    TokenValid checkTokenValid(String username, String salt, String token, String tenantName);

    /**
     * 注销用户的Token令牌
     *
     * @param username   用户名称
     * @param tenantName 租户名称
     * @return 是否注销
     */
    Boolean cancelToken(String username, String tenantName);
}
