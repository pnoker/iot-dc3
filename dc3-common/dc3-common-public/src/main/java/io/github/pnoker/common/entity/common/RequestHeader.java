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

package io.github.pnoker.common.entity.common;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request Header
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
public class RequestHeader {

    private RequestHeader() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenHeader {
        /**
         * 盐值
         */
        private String salt;

        /**
         * JWT Token
         */
        private String token;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserHeader {

        /**
         * 用户 ID
         */
        private Long userId;

        /**
         * 用户别名
         */
        private String nickName;

        /**
         * 用户名称
         */
        private String userName;

        /**
         * 租户ID
         */
        private Long tenantId;
    }

}
