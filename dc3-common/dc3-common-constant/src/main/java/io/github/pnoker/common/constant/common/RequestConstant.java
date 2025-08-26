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
 * 请求 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class RequestConstant {

    /**
     * 最大请求次数限制
     */
    public static final int DEFAULT_MAX_REQUEST_SIZE = 100;

    private RequestConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 自定义请求 Header 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Header {

        /**
         * 自定义 租户编号 请求头
         * 用于前端请求头
         */
        public static final String X_AUTH_TENANT = "X-Auth-Tenant";

        /**
         * 自定义 用户登陆名称 请求头
         * 用于前端请求头
         */
        public static final String X_AUTH_LOGIN = "X-Auth-Login";

        /**
         * 自定义 Token 请求头
         * 用于前端请求头
         */
        public static final String X_AUTH_TOKEN = "X-Auth-Token";

        /**
         * 自定义 用户 请求头
         * 用于向其他服务传递用户信息, 其中包括: 租户ID, 用户ID, 用户昵称, 用户名称
         */
        public static final String X_AUTH_USER = "X-Auth-User";

        private Header() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

    }

    /**
     * 自定义请求 Message 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Message {

        /**
         * 无效的权限请求头
         */
        public static final String INVALID_REQUEST = "Invalid request auth header";

        private Message() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 自定义请求 Key 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Key {

        /**
         * 用户请求头关键字
         */
        public static final String USER_HEADER = "USER_HEADER_KEY";

        private Key() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }
}
