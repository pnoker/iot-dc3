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

class AgenticToolContextUtilTest {

    private static RequestHeader.PrincipalHeader userHeader(Long tenantId, Long userId) {
        RequestHeader.PrincipalHeader h = new RequestHeader.PrincipalHeader();
        h.setTenantId(tenantId);
        h.setPrincipalId(userId);
        return h;
    }

    private static ToolContext toolContext(Map<String, Object> values) {
        return new ToolContext(new HashMap<>(values));
    }

    @Test
    void requirePrincipalHeaderThrowsWhenAbsent() {
        assertThatThrownBy(() -> AgenticToolContextUtil.requirePrincipalHeader(toolContext(Map.of())))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requirePrincipalHeaderThrowsWhenTenantIdMissing() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, userHeader(null, 5L)));
        assertThatThrownBy(() -> AgenticToolContextUtil.requirePrincipalHeader(ctx))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requirePrincipalHeaderThrowsWhenUserIdMissing() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, null)));
        assertThatThrownBy(() -> AgenticToolContextUtil.requirePrincipalHeader(ctx))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requirePrincipalHeaderReturnsHeaderFromToolContextWhenComplete() {
        RequestHeader.PrincipalHeader header = userHeader(1L, 5L);
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, header));
        assertThat(AgenticToolContextUtil.requirePrincipalHeader(ctx)).isSameAs(header);
        assertThat(AgenticToolContextUtil.requireTenantId(ctx)).isEqualTo(1L);
        assertThat(AgenticToolContextUtil.requireUserId(ctx)).isEqualTo(5L);
    }

    @Test
    void requirePrincipalHeaderCanBeRebuiltFromScopedIds() {
        ToolContext ctx = toolContext(Map.of(
                AgenticConstant.ToolContextKey.TENANT_ID, 1L,
                AgenticConstant.ToolContextKey.USER_ID, 5L));
        RequestHeader.PrincipalHeader header = AgenticToolContextUtil.requirePrincipalHeader(ctx);
        assertThat(header.getTenantId()).isEqualTo(1L);
        assertThat(header.getUserId()).isEqualTo(5L);
    }

    @Test
    void requireTenantIdFromToolContextPrefersToolContextValue() {
        ToolContext ctx = toolContext(Map.of(
                AgenticConstant.ToolContextKey.TENANT_ID, 99L,
                AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, 5L)));
        assertThat(AgenticToolContextUtil.requireTenantId(ctx)).isEqualTo(99L);
    }

    @Test
    void requireTenantIdFallsBackToHeaderWhenContextValueMissing() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, 5L)));
        assertThat(AgenticToolContextUtil.requireTenantId(ctx)).isEqualTo(1L);
        assertThatThrownBy(() -> AgenticToolContextUtil.requireTenantId((ToolContext) null))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void requireTenantIdAcceptsNumberSubtypesInToolContext() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.TENANT_ID, Integer.valueOf(7)));
        assertThat(AgenticToolContextUtil.requireTenantId(ctx)).isEqualTo(7L);
    }

    @Test
    void requireUserIdFromToolContextPrefersToolContextValue() {
        ToolContext ctx = toolContext(Map.of(
                AgenticConstant.ToolContextKey.USER_ID, 88L,
                AgenticConstant.ToolContextKey.USER_HEADER, userHeader(1L, 5L)));
        assertThat(AgenticToolContextUtil.requireUserId(ctx)).isEqualTo(88L);
    }

    @Test
    void requireConversationIdAcceptsNonBlankString() {
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.CONVERSATION_ID, "abc"));
        assertThat(AgenticToolContextUtil.requireConversationId(ctx)).isEqualTo("abc");
    }

    @Test
    void requireConversationIdRejectsBlankOrMissing() {
        assertThatThrownBy(() -> AgenticToolContextUtil.requireConversationId(toolContext(Map.of())))
                .isInstanceOf(UnAuthorizedException.class);
        assertThatThrownBy(() -> AgenticToolContextUtil.requireConversationId(
                toolContext(Map.of(AgenticConstant.ToolContextKey.CONVERSATION_ID, "   "))))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void recordToolInvocationAppendsToQueueWhenPresent() {
        Queue<AgenticRunEvent> events = new ConcurrentLinkedQueue<>();
        ToolContext ctx = toolContext(Map.of(AgenticConstant.ToolContextKey.RUN_EVENTS, events));
        AgenticToolContextUtil.recordToolInvocation(ctx, "readPoint", "data", "Read point value");
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
        AgenticToolContextUtil.recordToolInvocation(ctx, "tool", "domain", "desc");
        AgenticToolContextUtil.recordToolInvocation(null, "tool", "domain", "desc");
    }
}
