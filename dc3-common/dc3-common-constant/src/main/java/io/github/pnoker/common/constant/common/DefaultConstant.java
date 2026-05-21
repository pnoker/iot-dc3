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
 * Default value related constants.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class DefaultConstant {

    /**
     * Default {@link Integer} null value: {@code -1}.
     */
    public static final Integer NULL_INT = -1;

    /**
     * Default {@link Integer} zero value: {@code 0}.
     */
    public static final Integer DEFAULT_INT = 0;

    /**
     * Default placeholder for an absent or unset {@code Long} identifier:
     * {@code 0L}. Use with primitive {@code ==} comparison after a
     * {@link java.util.Objects#isNull(Object)} check on the boxed value.
     */
    public static final long DEFAULT_ID = 0L;

    /**
     * Zero constant: {@code 0}.
     */
    public static final Integer ZERO = 0;

    /**
     * One constant: {@code 1}.
     */
    public static final Integer ONE = 1;

    /**
     * Default username.
     */
    public static final String USER_NAME = "pnoker";

    /**
     * Default {@link String} null value: {@code "nil"}.
     */
    public static final String NULL_STRING = "nil";

    /**
     * Default page size.
     */
    public static final Integer PAGE_SIZE = 20;

    /**
     * Default maximum page size.
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    private DefaultConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
