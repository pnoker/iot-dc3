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

import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticRequestContextTest {

    private static RequestHeader.UserHeader userHeader(Long tenantId, Long userId) {
        RequestHeader.UserHeader h = new RequestHeader.UserHeader();
        h.setTenantId(tenantId);
        h.setUserId(userId);
        return h;
    }

    private static ToolContext toolContext(Map<String, Object> values) {
        return new ToolContext(new HashMap<>(values));
    }

    @Test
    void requireUserHeaderThrowsWhenAbsent() {
        assertThatThrownBy(() -> AgenticRequestContext.requireUserHeader(toolContext(Map.of())))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireUserHeaderThrowsWhenTenantIdMissing() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, userHeader(null, 5L)));
        assertThatThrownBy(() -> AgenticRequestContext.requireUserHeader(ctx))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireUserHeaderThrowsWhenUserIdMissing() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, null)));
        assertThatThrownBy(() -> AgenticRequestContext.requireUserHeader(ctx))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireUserHeaderReturnsHeaderFromToolContextWhenComplete() {
        RequestHeader.UserHeader header = userHeader(1L, 5L);
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, header));
        assertThat(AgenticRequestContext.requireUserHeader(ctx)).isSameAs(header);
        assertThat(AgenticRequestContext.requireTenantId(ctx)).isEqualTo(1L);
        assertThat(AgenticRequestContext.requireUserId(ctx)).isEqualTo(5L);
    }

    @Test
    void requireUserHeaderCanBeRebuiltFromScopedIds() {
        ToolContext ctx = toolContext(Map.of(
                AgenticConstant.ToolContextKey.TENANT_ID, 1L,
                AgenticConstant.ToolContextKey.USER_ID, 5L));
        RequestHeader.UserHeader header = AgenticRequestContext.requireUserHeader(ctx);
        assertThat(header.getTenantId()).isEqualTo(1L);
        assertThat(header.getUserId()).isEqualTo(5L);
    }

    @Test
    void requireTenantIdFromToolContextPrefersToolContextValue() {
        ToolContext ctx = toolContext(Map.of(
                AgenticConstant.ToolContextKey.TENANT_ID, 99L,
                AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, 5L)));
        assertThat(AgenticRequestContext.requireTenantId(ctx)).isEqualTo(99L);
    }

    @Test
    void requireTenantIdFallsBackToHeaderWhenContextValueMissing() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, 5L)));
        assertThat(AgenticRequestContext.requireTenantId(ctx)).isEqualTo(1L);
        assertThatThrownBy(() -> AgenticRequestContext.requireTenantId((ToolContext) null))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireTenantIdAcceptsNumberSubtypesInToolContext() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.TENANT_ID, Integer.valueOf(7)));
        assertThat(AgenticRequestContext.requireTenantId(ctx)).isEqualTo(7L);
    }

    @Test
    void requireUserIdFromToolContextPrefersToolContextValue() {
        ToolContext ctx = toolContext(Map.of(
                AgenticConstant.ToolContextKey.USER_ID, 88L,
                AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, 5L)));
        assertThat(AgenticRequestContext.requireUserId(ctx)).isEqualTo(88L);
    }

    @Test
    void requireConversationIdAcceptsNonBlankString() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.CONVERSATION_ID, "abc"));
        assertThat(AgenticRequestContext.requireConversationId(ctx)).isEqualTo("abc");
    }

    @Test
    void requireConversationIdRejectsBlankOrMissing() {
        assertThatThrownBy(() -> AgenticRequestContext.requireConversationId(toolContext(Map.of())))
                .isInstanceOf(UnAuthorizedException.class);
        assertThatThrownBy(() -> AgenticRequestContext.requireConversationId(
                toolContext(Map.of(AgenticConstant.ToolContextKey.CONVERSATION_ID, "   "))))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void recordToolInvocationAppendsToQueueWhenPresent() {
        Queue<AgenticRunEvent> events = new ConcurrentLinkedQueue<>();
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.RUN_EVENTS, events));
        AgenticRequestContext.recordToolInvocation(ctx, "readPoint", "data", "Read point value");
        assertThat(events).hasSize(1);
        assertThat(events.peek().type()).isEqualTo("tool");
        assertThat(events.peek().name()).isEqualTo("readPoint");
        assertThat(events.peek().detail()).isEqualTo("data");
        assertThat(events.peek().title()).isEqualTo("Read point value");
        assertThat(events.peek().timestamp()).isPositive();
    }

    @Test
    void recordToolInvocationIsNoOpWhenContextHasNoQueue() {
        ToolContext ctx = toolContext(Map.of());
        // Just assert it doesn't throw.
        AgenticRequestContext.recordToolInvocation(ctx, "tool", "domain", "desc");
        AgenticRequestContext.recordToolInvocation(null, "tool", "domain", "desc");
    }
}
