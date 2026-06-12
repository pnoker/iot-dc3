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
 * HTTP request header containers for token and user context.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
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
    public static class PrincipalHeader {

        /**
         * Principal ID for authentication, authorization, and audit.
         */
        private Long principalId;

        /**
         * Principal type, for example USER or SERVICE_ACCOUNT.
         */
        private String principalType;

        /**
         * Display name.
         */
        private String displayName;

        /**
         * Principal account name.
         */
        private String principalName;

        /**
         * Tenant ID for multi-tenant isolation
         */
        private Long tenantId;

        /**
         * OAuth client ID when the request is delegated through OAuth or MCP.
         */
        private String clientId;

        /**
         * MCP connection ID when the request originates from the MCP runtime.
         */
        private Long connectionId;

        /**
         * Audit accessor. The value is the principal ID, not dc3_user.id.
         *
         * @return principal ID
         */
        public Long getUserId() {
            return principalId;
        }

        /**
         * Audit display-name accessor.
         *
         * @return display name
         */
        public String getNickName() {
            return displayName;
        }

        /**
         * Audit account-name accessor.
         *
         * @return principal name
         */
        public String getUserName() {
            return principalName;
        }

    }

}
