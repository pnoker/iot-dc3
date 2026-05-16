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

import io.github.pnoker.common.agentic.annotation.AgenticToolMetadata;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticToolContractTest {

    private static Stream<Class<?>> toolTypes() {
        return List.of(
                        TenantTool.class,
                        UserTool.class,
                        DeviceTool.class,
                        DriverTool.class,
                        ProfileTool.class,
                        PointTool.class,
                        PointValueTool.class,
                        SystemTool.class)
                .stream();
    }

    private static Stream<Method> toolMethods() {
        return toolTypes()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(Tool.class));
    }

    @ParameterizedTest
    @MethodSource("toolTypes")
    void toolClassesUseToolSuffix(Class<?> toolType) {
        assertThat(toolType.getSimpleName()).endsWith("Tool");
    }

    @ParameterizedTest
    @MethodSource("toolMethods")
    void platformToolsReturnStructuredResults(Method method) {
        assertThat(method.getReturnType()).isEqualTo(AgenticToolResult.class);
    }

    @ParameterizedTest
    @MethodSource("toolMethods")
    void platformToolsRequireBackendToolContext(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        assertThat(parameterTypes)
                .as("Tool method %s must receive backend ToolContext", method.getName())
                .isNotEmpty();
        assertThat(parameterTypes[parameterTypes.length - 1]).isEqualTo(ToolContext.class);
    }

    @ParameterizedTest
    @MethodSource("toolMethods")
    void platformToolsDeclareTraceMetadata(Method method) {
        assertThat(method.getAnnotation(AgenticToolMetadata.class))
                .as("Tool method %s must expose trace metadata", method.getName())
                .isNotNull();
    }

}
