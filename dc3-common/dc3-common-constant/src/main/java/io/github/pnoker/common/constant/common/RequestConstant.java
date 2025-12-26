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
 * Constants related to requests
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class RequestConstant {

    /**
     * Maximum request size limit
     */
    public static final int DEFAULT_MAX_REQUEST_SIZE = 100;

    private RequestConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Constants related to custom request headers
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    public static class Header {

        /**
         * Custom tenant ID header
         * Used in frontend request headers
         */
        public static final String X_AUTH_TENANT = "X-Auth-Tenant";

        /**
         * Custom user login name header
         * Used in frontend request headers
         */
        public static final String X_AUTH_LOGIN = "X-Auth-Login";

        /**
         * Custom token header
         * Used in frontend request headers
         */
        public static final String X_AUTH_TOKEN = "X-Auth-Token";

        /**
         * Custom user header
         * Used to pass user information to other services, including: tenant ID, user ID, user nickname, user name
         */
        public static final String X_AUTH_USER = "X-Auth-User";

        private Header() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

    }

    /**
     * Constants related to custom request messages
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    public static class Message {

        /**
         * Invalid authorization request header
         */
        public static final String INVALID_REQUEST = "Invalid request auth header";

        private Message() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * Constants related to custom request keys
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    public static class Key {

        /**
         * User header key
         */
        public static final String USER_HEADER = "USER_HEADER_KEY";

        private Key() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }
}
