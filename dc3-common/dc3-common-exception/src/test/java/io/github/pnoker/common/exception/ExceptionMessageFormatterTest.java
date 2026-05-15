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

package io.github.pnoker.common.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionMessageFormatterTest {

    private String invokeFormat(String template, Object... params) throws Exception {
        Method method = ExceptionMessageFormatter.class.getDeclaredMethod(
                "format", String.class, Object[].class);
        method.setAccessible(true);
        return (String) method.invoke(null, template, params);
    }

    private Throwable invokeCause(Object... params) throws Exception {
        Method method = ExceptionMessageFormatter.class.getDeclaredMethod("cause", Object[].class);
        method.setAccessible(true);
        return (Throwable) method.invoke(null, new Object[]{params});
    }

    @Test
    void returnsTemplateWhenNoPlaceholders() throws Exception {
        assertThat(invokeFormat("static text", "ignored")).isEqualTo("static text");
    }

    @Test
    void substitutesPlaceholdersInOrder() throws Exception {
        assertThat(invokeFormat("user {} action {}", "alice", "login"))
                .isEqualTo("user alice action login");
    }

    @Test
    void leavesExtraPlaceholdersWhenParamsRunOut() throws Exception {
        assertThat(invokeFormat("a={} b={} c={}", "1", "2"))
                .isEqualTo("a=1 b=2 c={}");
    }

    @Test
    void ignoresExtraParamsBeyondPlaceholders() throws Exception {
        assertThat(invokeFormat("only one {}", "first", "second", "third"))
                .isEqualTo("only one first");
    }

    @Test
    void returnsTemplateWhenParamsArrayIsNullOrEmpty() throws Exception {
        assertThat(invokeFormat("template", (Object[]) null)).isEqualTo("template");
        assertThat(invokeFormat("template")).isEqualTo("template");
    }

    @Test
    void returnsNullWhenTemplateIsNull() throws Exception {
        assertThat(invokeFormat(null, "ignored")).isNull();
    }

    @Test
    void formatsNullParamAsLiteralNull() throws Exception {
        assertThat(invokeFormat("value={}", new Object[]{null})).isEqualTo("value=null");
    }

    @Test
    void preservesSurroundingText() throws Exception {
        assertThat(invokeFormat("[before:{}:after]", "x"))
                .isEqualTo("[before:x:after]");
    }

    @Test
    void trailingThrowableIsCauseNotFormatParam() throws Exception {
        IllegalStateException cause = new IllegalStateException("root");
        assertThat(invokeFormat("operation {} failed", "sync", cause))
                .isEqualTo("operation sync failed");
        assertThat(invokeCause("sync", cause)).isSameAs(cause);
    }
}
