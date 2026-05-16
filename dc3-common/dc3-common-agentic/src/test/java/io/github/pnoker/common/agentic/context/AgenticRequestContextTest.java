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
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.List;
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

    @AfterEach
    void cleanThreadLocal() {
        AgenticRequestContext.clear();
    }

    @Test
    void requireUserHeaderThrowsWhenAbsent() {
        assertThatThrownBy(AgenticRequestContext::requireUserHeader).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireUserHeaderThrowsWhenTenantIdMissing() {
        AgenticRequestContext.set(userHeader(null, 5L));
        assertThatThrownBy(AgenticRequestContext::requireUserHeader).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireUserHeaderThrowsWhenUserIdMissing() {
        AgenticRequestContext.set(userHeader(1L, null));
        assertThatThrownBy(AgenticRequestContext::requireUserHeader).isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireUserHeaderReturnsHeaderWhenComplete() {
        RequestHeader.UserHeader header = userHeader(1L, 5L);
        AgenticRequestContext.set(header);
        assertThat(AgenticRequestContext.requireUserHeader()).isSameAs(header);
        assertThat(AgenticRequestContext.requireTenantId()).isEqualTo(1L);
        assertThat(AgenticRequestContext.requireUserId()).isEqualTo(5L);
    }

    @Test
    void clearRemovesThreadLocalHeader() {
        AgenticRequestContext.set(userHeader(1L, 5L));
        AgenticRequestContext.setMemoryHistory("conv", List.of(new MessageBO()));
        AgenticRequestContext.clear();
        assertThatThrownBy(AgenticRequestContext::requireUserHeader).isInstanceOf(UnAuthorizedException.class);
        assertThat(AgenticRequestContext.getMemoryHistory("conv")).isEmpty();
    }

    @Test
    void memoryHistoryCanCacheEmptyResultForConversation() {
        AgenticRequestContext.setMemoryHistory("conv", List.of());
        assertThat(AgenticRequestContext.getMemoryHistory("conv")).hasValue(List.of());
        assertThat(AgenticRequestContext.getMemoryHistory("other")).isEmpty();
    }

    @Test
    void requireTenantIdFromToolContextPrefersToolContextValue() {
        AgenticRequestContext.set(userHeader(1L, 5L));
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.TENANT_ID, 99L));
        assertThat(AgenticRequestContext.requireTenantId(ctx)).isEqualTo(99L);
    }

    @Test
    void requireTenantIdFallsBackToHeaderWhenContextValueMissing() {
        AgenticRequestContext.set(userHeader(1L, 5L));
        assertThat(AgenticRequestContext.requireTenantId(toolContext(Map.of()))).isEqualTo(1L);
        assertThat(AgenticRequestContext.requireTenantId((ToolContext) null)).isEqualTo(1L);
    }

    @Test
    void requireTenantIdAcceptsNumberSubtypesInToolContext() {
        AgenticRequestContext.set(userHeader(1L, 5L));
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.TENANT_ID, Integer.valueOf(7)));
        assertThat(AgenticRequestContext.requireTenantId(ctx)).isEqualTo(7L);
    }

    @Test
    void requireUserIdFromToolContextPrefersToolContextValue() {
        AgenticRequestContext.set(userHeader(1L, 5L));
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_ID, 88L));
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
