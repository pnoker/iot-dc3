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
 * Exception-related common messages shared across modules.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class ExceptionConstant {

    /**
     * Utility class constructor message.
     */
    public static final String UTILITY_CLASS = BaseConstant.UTILITY_CLASS;

    /**
     * No available service instance for the requested client.
     */
    public static final String NO_AVAILABLE_SERVER = "No available server for client";

    /**
     * Tenant and user authentication context does not match.
     */
    public static final String NO_AVAILABLE_AUTH = "Tenant, user information does not match";

    private ExceptionConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
