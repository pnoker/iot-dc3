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
 * Request Header Configuration Class
 * <p>
 * Configuration class for HTTP request headers in IoT DC3 platform.
 * Contains static inner classes for token and user headers
 * with authentication and user identification information.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
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
         * Salt value for token encryption
         */
        private String salt;

        /**
         * JWT token string
         */
        private String token;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserHeader {

        /**
         * User ID for identification
         */
        private Long userId;

        /**
         * User nickname or alias
         */
        private String nickName;

        /**
         * User account name
         */
        private String userName;

        /**
         * Tenant ID for multi-tenant isolation
         */
        private Long tenantId;
    }

}
