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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Objects;

/**
 * Request-scoped auth context used by Spring AI tools.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.9
 */
public class AgenticRequestContext {

    private static final ThreadLocal<RequestHeader.UserHeader> USER_HEADER = new InheritableThreadLocal<>();

    private AgenticRequestContext() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    public static void set(RequestHeader.UserHeader userHeader) {
        USER_HEADER.set(userHeader);
    }

    public static void clear() {
        USER_HEADER.remove();
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

    private static Long getLongContextValue(ToolContext toolContext, String key) {
        if (Objects.isNull(toolContext) || Objects.isNull(toolContext.getContext())) {
            return null;
        }
        Object value = toolContext.getContext().get(key);
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return null;
    }

}
