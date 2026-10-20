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
package io.github.pnoker.common.agentic.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.service.runtime.ReactiveAgenticToolRegistry;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Locks the tool registration contract: every public tool method on the tool classes
 * must surface in the registry under its own name, so adding a tool method without
 * registering it (or the reverse) fails the build instead of silently shrinking the
 * agent's tool surface.
 */
class AgenticToolContractTest {

    private static Stream<Class<?>> toolTypes() {
        return List.of(
                        TenantTool.class,
                        UserTool.class,
                        SystemTool.class)
                .stream();
    }

    private static ReactiveAgenticToolRegistry registry() {
        return new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                mock(TenantTool.class),
                mock(UserTool.class),
                mock(SystemTool.class),
                new ObjectMapper());
    }

    private static Stream<Method> publicToolMethods() {
        return toolTypes()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()));
    }

    @ParameterizedTest
    @MethodSource("toolTypes")
    void toolClassesUseToolSuffix(Class<?> toolType) {
        assertThat(toolType.getSimpleName()).endsWith("Tool");
    }

    @ParameterizedTest
    @MethodSource("publicToolMethods")
    void everyPublicToolMethodIsRegistered(Method method) {
        assertThat(registry().tools())
                .as("tool method %s.%s must be registered in the registry",
                        method.getDeclaringClass().getSimpleName(), method.getName())
                .containsKey(method.getName());
    }

    @Test
    void platformToolsReturnStructuredResults() {
        publicToolMethods().forEach(method -> assertThat(method.getReturnType())
                .as("Tool method %s must return AgenticToolResult or Mono", method.getName())
                .satisfiesAnyOf(
                        type -> assertThat(type).isEqualTo(AgenticToolResult.class),
                        type -> assertThat(type).isEqualTo(Mono.class)));
    }
}
