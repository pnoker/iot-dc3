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
package io.github.pnoker.common.agentic.util;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * Utility methods for tenant/user scoped agentic conversation IDs.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class AgenticConversationIds {

    private static final String SEPARATOR = ":";

    private AgenticConversationIds() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Scope a client conversation ID by tenant and user before it is used for memory or
     * persistence.
     *
     * @param tenantId       tenant ID
     * @param userId         user ID
     * @param conversationId client conversation ID
     * @return tenant/user scoped conversation ID
     */
    public static String scope(Long tenantId, Long userId, String conversationId) {
        return tenantId + SEPARATOR + userId + SEPARATOR + conversationId;
    }

    /**
     * Strip the tenant/user scope before returning a conversation ID to API clients.
     *
     * @param tenantId       tenant ID
     * @param userId         user ID
     * @param conversationId stored conversation ID
     * @return original client conversation ID when the expected scope is present
     */
    public static String stripScope(Long tenantId, Long userId, String conversationId) {
        String prefix = tenantId + SEPARATOR + userId + SEPARATOR;
        return conversationId != null && conversationId.startsWith(prefix) ? conversationId.substring(prefix.length())
                : conversationId;
    }

}
