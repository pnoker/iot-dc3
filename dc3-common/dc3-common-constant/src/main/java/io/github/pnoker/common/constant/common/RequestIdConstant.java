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
 * Cross-transport request correlation identifiers.
 *
 * @author pnoker
 * @since 2026.5.22
 */
public final class RequestIdConstant {

    /**
     * SLF4J MDC key used by the shared logging pattern.
     */
    public static final String MDC_KEY = "requestId";

    /**
     * Reactor context key used to propagate the request id across thread switches.
     */
    public static final String REACTOR_CONTEXT_KEY = "dc3.requestId";

    /**
     * HTTP, gRPC and AMQP header carrying the request id.
     */
    public static final String HEADER = "X-Request-Id";

    private RequestIdConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
