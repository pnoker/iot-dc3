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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticConversationIdsTest {

    private static final String SEPARATOR = Character.toString(31);

    @Test
    void scopePrefixesTenantAndUserBeforeConversationId() {
        assertThat(AgenticConversationIds.scope(1L, 2L, "abc")).isEqualTo("1" + SEPARATOR + "2" + SEPARATOR + "abc");
    }

    @Test
    void scopeAcceptsBlankConversationId() {
        assertThat(AgenticConversationIds.scope(1L, 2L, "")).isEqualTo("1" + SEPARATOR + "2" + SEPARATOR);
    }

    @Test
    void stripScopeRemovesMatchingPrefix() {
        assertThat(AgenticConversationIds.stripScope(1L, 2L, "1" + SEPARATOR + "2" + SEPARATOR + "abc"))
                .isEqualTo("abc");
    }

    @Test
    void stripScopeRemovesLegacyColonPrefix() {
        assertThat(AgenticConversationIds.stripScope(1L, 2L, "1:2:abc")).isEqualTo("abc");
    }

    @Test
    void stripScopeReturnsOriginalWhenPrefixMismatch() {
        assertThat(AgenticConversationIds.stripScope(1L, 2L, "9:9:abc")).isEqualTo("9:9:abc");
    }

    @Test
    void stripScopeReturnsNullWhenInputIsNull() {
        assertThat(AgenticConversationIds.stripScope(1L, 2L, null)).isNull();
    }

    @Test
    void stripScopeReturnsEmptyWhenInputIsExactlyThePrefix() {
        assertThat(AgenticConversationIds.stripScope(1L, 2L, "1" + SEPARATOR + "2" + SEPARATOR)).isEmpty();
    }

    @Test
    void utilityConstructorMustReject() throws NoSuchMethodException {
        Constructor<AgenticConversationIds> ctor = AgenticConversationIds.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThatThrownBy(ctor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
