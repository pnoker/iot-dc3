/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.constant.service;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 权限服务 相关常量
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class AuthServiceConstant {

    private AuthServiceConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 服务名
     */
    public static final String SERVICE_NAME = "DC3-CENTER-AUTH";

    public static final String USER_URL_PREFIX = "/auth/user";
    public static final String TENANT_URL_PREFIX = "/auth/tenant";
    public static final String TOKEN_URL_PREFIX = "/auth/token";
    public static final String BLACK_IP_URL_PREFIX = "/auth/black_ip";
    public static final String DICTIONARY_URL_PREFIX = "/auth/dictionary";
}
