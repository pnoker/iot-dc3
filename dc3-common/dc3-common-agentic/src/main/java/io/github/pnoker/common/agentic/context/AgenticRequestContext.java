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
package io.github.pnoker.common.agentic.context;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.springframework.ai.chat.model.ToolContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

/**
 * Request-scoped auth context used by Spring AI tools.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.9
 */
public class AgenticRequestContext {

    private static final ThreadLocal<RequestHeader.UserHeader> USER_HEADER = new InheritableThreadLocal<>();
    private static final ThreadLocal<Map<String, List<MessageBO>>> MEMORY_HISTORY = new InheritableThreadLocal<>();

    private AgenticRequestContext() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    public static void set(RequestHeader.UserHeader userHeader) {
        USER_HEADER.set(userHeader);
    }

    public static void clear() {
        USER_HEADER.remove();
        MEMORY_HISTORY.remove();
    }

    public static void setMemoryHistory(String conversationId, List<MessageBO> history) {
        if (Objects.isNull(conversationId) || conversationId.isBlank()) {
            return;
        }
        Map<String, List<MessageBO>> histories = MEMORY_HISTORY.get();
        if (Objects.isNull(histories)) {
            histories = new HashMap<>();
            MEMORY_HISTORY.set(histories);
        }
        histories.put(conversationId, Objects.isNull(history) ? List.of() : List.copyOf(history));
    }

    public static Optional<List<MessageBO>> getMemoryHistory(String conversationId) {
        if (Objects.isNull(conversationId) || conversationId.isBlank()) {
            return Optional.empty();
        }
        Map<String, List<MessageBO>> histories = MEMORY_HISTORY.get();
        if (Objects.isNull(histories) || !histories.containsKey(conversationId)) {
            return Optional.empty();
        }
        return Optional.of(histories.get(conversationId));
    }

    public static RequestHeader.UserHeader requireUserHeader() {
        RequestHeader.UserHeader userHeader = USER_HEADER.get();
        if (Objects.isNull(userHeader) || Objects.isNull(userHeader.getTenantId())
                || Objects.isNull(userHeader.getUserId())) {
            throw new UnAuthorizedException("Unable to get agentic user header");
        }
        return userHeader;
    }

    public static Long requireTenantId(ToolContext toolContext) {
        Long tenantId = getLongContextValue(toolContext, AgenticConstant.ToolContextKey.TENANT_ID);
        return Objects.nonNull(tenantId) ? tenantId : requireTenantId();
    }

    public static Long requireTenantId() {
        return requireUserHeader().getTenantId();
    }

    public static Long requireUserId(ToolContext toolContext) {
        Long userId = getLongContextValue(toolContext, AgenticConstant.ToolContextKey.USER_ID);
        return Objects.nonNull(userId) ? userId : requireUserId();
    }

    public static Long requireUserId() {
        return requireUserHeader().getUserId();
    }

    public static String requireConversationId(ToolContext toolContext) {
        Object value = getContextValue(toolContext, AgenticConstant.ToolContextKey.CONVERSATION_ID);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new UnAuthorizedException("Unable to get agentic conversation ID");
    }

    @SuppressWarnings("unchecked")
    public static void recordToolInvocation(ToolContext toolContext, String toolName, String domain,
                                            String description) {
        Object value = getContextValue(toolContext, AgenticConstant.ToolContextKey.TOOL_EVENTS);
        if (value instanceof Queue<?>) {
            ((Queue<ToolEvent>) value).offer(new ToolEvent(toolName, domain, description, Instant.now().toEpochMilli()));
        }
    }

    private static Long getLongContextValue(ToolContext toolContext, String key) {
        Object value = getContextValue(toolContext, key);
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return null;
    }

    private static Object getContextValue(ToolContext toolContext, String key) {
        if (Objects.isNull(toolContext) || Objects.isNull(toolContext.getContext())) {
            return null;
        }
        return toolContext.getContext().get(key);
    }

    public record ToolEvent(String toolName, String domain, String description, long timestamp) {
    }

}
