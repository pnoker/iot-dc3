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

import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Accessors for the explicit Spring AI tool context passed into platform tools.
 *
 * @author pnoker
 * @since 2026.5.9
 */
public class AgenticToolContextUtil {

    private AgenticToolContextUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Require principal header.
     *
     * @param toolContext tool context
     * @return require principal header result
     */
    public static RequestHeader.PrincipalHeader requirePrincipalHeader(ToolContext toolContext) {
        Object value = getContextValue(toolContext, AgenticConstant.ToolContextKey.USER_HEADER);
        if (value instanceof RequestHeader.PrincipalHeader userHeader) {
            validatePrincipalHeader(userHeader);
            return userHeader;
        }
        RequestHeader.PrincipalHeader userHeader = new RequestHeader.PrincipalHeader();
        userHeader.setTenantId(getLongContextValue(toolContext, AgenticConstant.ToolContextKey.TENANT_ID));
        userHeader.setPrincipalId(getLongContextValue(toolContext, AgenticConstant.ToolContextKey.USER_ID));
        validatePrincipalHeader(userHeader);
        return userHeader;
    }

    private static void validatePrincipalHeader(RequestHeader.PrincipalHeader userHeader) {
        if (Objects.isNull(userHeader) || Objects.isNull(userHeader.getTenantId())
                || Objects.isNull(userHeader.getPrincipalId())) {
            throw new UnAuthorizedException("Unable to get agentic principal header");
        }
    }

    /**
     * Require tenant identifier.
     *
     * @param toolContext tool context
     * @return require tenant identifier result
     */
    public static Long requireTenantId(ToolContext toolContext) {
        Long tenantId = getLongContextValue(toolContext, AgenticConstant.ToolContextKey.TENANT_ID);
        if (Objects.nonNull(tenantId)) {
            return tenantId;
        }
        return requirePrincipalHeader(toolContext).getTenantId();
    }

    /**
     * Require user identifier.
     *
     * @param toolContext tool context
     * @return require user identifier result
     */
    public static Long requireUserId(ToolContext toolContext) {
        Long userId = getLongContextValue(toolContext, AgenticConstant.ToolContextKey.USER_ID);
        if (Objects.nonNull(userId)) {
            return userId;
        }
        return requirePrincipalHeader(toolContext).getUserId();
    }

    /**
     * Require conversation identifier.
     *
     * @param toolContext tool context
     * @return require conversation identifier result
     */
    public static String requireConversationId(ToolContext toolContext) {
        Object value = getContextValue(toolContext, AgenticConstant.ToolContextKey.CONVERSATION_ID);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new UnAuthorizedException("Unable to get agentic conversation ID");
    }

    /**
     * Record tool invocation.
     *
     * @param toolContext tool context
     * @param toolName    tool name
     * @param domain      domain
     * @param description description
     */
    public static void recordToolInvocation(ToolContext toolContext, String toolName, String domain,
                                            String description) {
        recordRunEvent(toolContext, AgenticRunEvent.toolStart(toolName, domain, description));
    }

    /**
     * Record tool result.
     *
     * @param toolContext tool context
     * @param toolName    tool name
     * @param success     success
     * @param code        code
     * @param message     message
     */
    public static void recordToolResult(ToolContext toolContext, String toolName, boolean success, String code,
                                        String message) {
        recordRunEvent(toolContext, AgenticRunEvent.toolResult(toolName, success, code, message));
    }

    /**
     * Record tool error.
     *
     * @param toolContext tool context
     * @param toolName    tool name
     * @param message     message
     */
    public static void recordToolError(ToolContext toolContext, String toolName, String message) {
        recordRunEvent(toolContext, AgenticRunEvent.toolError(toolName, message));
    }

    /**
     * Record visualizations.
     *
     * @param toolContext    tool context
     * @param visualizations visualizations
     */
    @SuppressWarnings("unchecked")
    public static void recordVisualizations(ToolContext toolContext, List<AgenticVisualizationSpec> visualizations) {
        if (Objects.isNull(visualizations) || visualizations.isEmpty()) {
            return;
        }
        Object value = getContextValue(toolContext, AgenticConstant.ToolContextKey.VISUALIZATIONS);
        if (value instanceof Queue<?>) {
            Queue<AgenticVisualizationSpec> queue = (Queue<AgenticVisualizationSpec>) value;
            visualizations.stream().filter(Objects::nonNull).forEach(queue::add);
        }
    }

    /**
     * Record run event.
     *
     * @param toolContext tool context
     * @param event       event
     */
    @SuppressWarnings("unchecked")
    public static void recordRunEvent(ToolContext toolContext, AgenticRunEvent event) {
        if (Objects.isNull(event)) {
            return;
        }
        Object value = getContextValue(toolContext, AgenticConstant.ToolContextKey.RUN_EVENTS);
        if (value instanceof Queue<?>) {
            ((Queue<AgenticRunEvent>) value).add(event);
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

}
