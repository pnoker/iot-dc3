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
 * HTTP request related constants.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class RequestConstant {

    /**
     * Default maximum request count limit.
     */
    public static final int DEFAULT_MAX_REQUEST_SIZE = 100;

    /**
     * Default freshness window for signed internal requests, in milliseconds.
     */
    public static final long DEFAULT_INTERNAL_SIGNATURE_TTL_MS = 300_000L;

    private RequestConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Custom request header related constants.
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2016.10.1
     */
    public static class Header {

        /**
         * Custom tenant code request header, used by frontend requests.
         */
        public static final String X_AUTH_TENANT = "X-Auth-Tenant";

        /**
         * Custom user login name request header, used by frontend requests.
         */
        public static final String X_AUTH_LOGIN = "X-Auth-Login";

        /**
         * Custom token request header, used by frontend requests.
         */
        public static final String X_AUTH_TOKEN = "X-Auth-Token";

        /**
         * Custom principal request header. Used to pass authenticated caller information
         * to backend services.
         */
        public static final String X_AUTH_PRINCIPAL = "X-Auth-Principal";

        /**
         * HMAC-SHA256 signature of the {@link #X_AUTH_PRINCIPAL} JSON payload,
         * hex-encoded.
         * Set by the gateway when {@code dc3.auth.hmac.secret} is configured; backend
         * services reject the request if the signature does not match.
         */
        public static final String X_AUTH_SIGN = "X-Auth-Sign";

        public static final String X_INTERNAL_CALLER = "X-Internal-Caller";

        public static final String X_INTERNAL_TIMESTAMP = "X-Internal-Timestamp";

        public static final String X_INTERNAL_NONCE = "X-Internal-Nonce";

        public static final String X_INTERNAL_SIGN = "X-Internal-Sign";

        /**
         * Standard idempotency key header forwarded to backend write endpoints.
         */
        public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

        /**
         * MCP client confirmation identifier accepted at the public gateway endpoint.
         */
        public static final String MCP_CONFIRM_ID = "Mcp-Confirm-Id";

        /**
         * MCP client idempotency key accepted at the public gateway endpoint.
         */
        public static final String MCP_IDEMPOTENCY_KEY = "Mcp-Idempotency-Key";

        /**
         * MCP client name header used for audit attribution.
         */
        public static final String MCP_CLIENT_NAME = "Mcp-Client-Name";

        /**
         * MCP client version header used for audit attribution.
         */
        public static final String MCP_CLIENT_VERSION = "Mcp-Client-Version";

        /**
         * Internal confirmation header forwarded from gateway to backend services.
         */
        public static final String X_MCP_CONFIRM_ID = "X-Mcp-Confirm-Id";

        private Header() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Custom request message related constants.
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2016.10.1
     */
    public static class Message {

        /**
         * Invalid authorization request header message.
         */
        public static final String INVALID_REQUEST = "Invalid request auth header";

        private Message() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Custom request key related constants.
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2016.10.1
     */
    public static class Key {

        /**
         * User request header key used in context.
         */
        public static final String USER_HEADER = "USER_HEADER_KEY";

        private Key() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

}
