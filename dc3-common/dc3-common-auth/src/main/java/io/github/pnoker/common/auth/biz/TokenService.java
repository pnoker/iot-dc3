/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bean.TokenValid;

/**
 * Token Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface TokenService {
    /**
     * 生成盐值
     *
     * @param loginName  登录名称
     * @param tenantCode 租户编号
     * @return R of String
     */
    String generateSalt(String loginName, String tenantCode);

    /**
     * 生成令牌
     *
     * @param loginName  登录名称
     * @param salt       User Salt
     * @param password   User Password
     * @param tenantCode 租户编号
     * @return R of String
     */
    String generateToken(String loginName, String salt, String password, String tenantCode);

    /**
     * 校验令牌
     *
     * @param loginName  登录名称
     * @param salt       盐值
     * @param token      Token
     * @param tenantCode 租户编号
     * @return TokenValid
     */
    TokenValid checkValid(String loginName, String salt, String token, String tenantCode);

    /**
     * 注销令牌
     *
     * @param loginName  登录名称
     * @param tenantCode 租户编号
     * @return 是否注销
     */
    Boolean cancelToken(String loginName, String tenantCode);
}
