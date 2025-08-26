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

package io.github.pnoker.common.constant.service;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 权限服务 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class AuthConstant {

    /**
     * 服务名
     */
    public static final String SERVICE_NAME = "dc3-center-auth";
    public static final String USER_URL_PREFIX = "/user";
    public static final String TENANT_URL_PREFIX = "/tenant";
    public static final String TOKEN_URL_PREFIX = "/token";
    public static final String DICTIONARY_URL_PREFIX = "/dictionary";

    private AuthConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
