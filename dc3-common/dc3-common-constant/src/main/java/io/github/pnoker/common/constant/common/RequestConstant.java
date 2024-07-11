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

package io.github.pnoker.common.constant.common;

/**
 * 请求 相关常量
 *
 * @author pnoker
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
}
