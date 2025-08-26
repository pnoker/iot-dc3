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

package io.github.pnoker.common.constant.cache;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 超时 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class TimeoutConstant {

    /**
     * salt 在 redis 中的失效时间, 分钟
     */
    public static final int SALT_CACHE_TIMEOUT = 5;
    /**
     * user 登陆限制失效时间, 分钟
     */
    public static final int USER_LIMIT_TIMEOUT = 5;
    /**
     * token 在 redis 中的失效时间, 小时
     */
    public static final int TOKEN_CACHE_TIMEOUT = 12;

    private TimeoutConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
