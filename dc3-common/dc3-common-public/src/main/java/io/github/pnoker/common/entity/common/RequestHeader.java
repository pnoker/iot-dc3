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
 * @version 2025.6.0
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
