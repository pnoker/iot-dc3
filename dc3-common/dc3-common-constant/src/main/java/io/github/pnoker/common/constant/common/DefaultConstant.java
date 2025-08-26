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

package io.github.pnoker.common.constant.common;

/**
 * 默认 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class DefaultConstant {

    /**
     * 默认 Integer 空值: -1
     */
    public static final Integer NULL_INT = -1;

    /**
     * 默认 Integer 零值: 0
     */
    public static final Integer DEFAULT_INT = 0;

    /**
     * 零: 0
     */
    public static final Integer ZERO = 0;

    /**
     * 一: 1
     */
    public static final Integer ONE = 1;

    /**
     * 默认 String 空值: nil
     */
    public static final String USER_NAME = "pnoker";

    /**
     * 默认 String 空值: nil
     */
    public static final String NULL_STRING = "nil";

    /**
     * 默认分页数
     */
    public static final Integer PAGE_SIZE = 20;

    /**
     * 默认最大分页数
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    private DefaultConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
