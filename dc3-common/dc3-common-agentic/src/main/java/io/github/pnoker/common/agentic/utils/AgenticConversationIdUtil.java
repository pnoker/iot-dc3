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
package io.github.pnoker.common.agentic.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;

import java.util.Objects;

/**
 * Utility methods for tenant/user scoped agentic conversation IDs.
 *
 * <p>The scope is encoded as {@code tenantId<US>userId<US>conversationId} where
 * {@code <US>} is the ASCII Unit Separator (0x1F). Using a non-printable separator
 * prevents collisions with client-supplied conversation IDs that may contain
 * common punctuation such as colons or slashes.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class AgenticConversationIdUtil {

    /**
     * ASCII Unit Separator (0x1F). Not allowed in printable client conversation IDs.
     */
    private static final String SEPARATOR = "";

    /**
     * Legacy separator that may appear in already-persisted scoped IDs from older
     * deployments. Strip support is preserved for backward compatibility, but new
     * scopes are always written with {@link #SEPARATOR}.
     */
    private static final String LEGACY_SEPARATOR = ":";

    private AgenticConversationIdUtil() {
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
     * Recognises both the current {@link #SEPARATOR} and the legacy colon separator.
     *
     * @param tenantId       tenant ID
     * @param userId         user ID
     * @param conversationId stored conversation ID
     * @return original client conversation ID when the expected scope is present
     */
    public static String stripScope(Long tenantId, Long userId, String conversationId) {
        if (Objects.isNull(conversationId)) {
            return null;
        }
        String currentPrefix = tenantId + SEPARATOR + userId + SEPARATOR;
        if (conversationId.startsWith(currentPrefix)) {
            return conversationId.substring(currentPrefix.length());
        }
        String legacyPrefix = tenantId + LEGACY_SEPARATOR + userId + LEGACY_SEPARATOR;
        if (conversationId.startsWith(legacyPrefix)) {
            return conversationId.substring(legacyPrefix.length());
        }
        return conversationId;
    }

}
