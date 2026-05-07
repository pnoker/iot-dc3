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
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class ExceptionConstant {

    /**
     *
     */
    public static final String UTILITY_CLASS = "Utility class";

    /**
     *
     */
    public static final String NO_AVAILABLE_SERVER = "No available server for client";

    /**
     * Tenant,
     */
    public static final String NO_AVAILABLE_AUTH = "Tenant, user information does not match";

    private ExceptionConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

}
