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
package io.github.pnoker.common.agentic.service.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.tools.CommandTool;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import io.github.pnoker.common.agentic.tools.EventTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

class ReactiveAgenticToolRegistryTest {
    @Test
    void allRegisteredToolsAreUniqueAndUseOffsetPagination() {
        var registry = new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                new ObjectMapper());
        var tools = registry.tools();
        assertThat(tools.keySet()).doesNotHaveDuplicates();
        tools.values().stream()
                .filter(tool -> tool.definition().name().startsWith("search")
                        || tool.definition().name().startsWith("list"))
                .forEach(tool -> {
                    assertThat(tool.definition().inputSchema()).contains("offset", "limit");
                    assertThat(tool.definition().inputSchema()).doesNotContain("current", "size", "page");
                });
    }

    @Test
    void malformedArgumentsBecomeStructuredErrors() {
        var registry = new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                new ObjectMapper());
        StepVerifier.create(registry.tools().get("searchDevices").call("{", new ToolContext(Map.of())))
                .assertNext(result -> {
                    assertThat(result).isInstanceOf(AgenticToolResult.class);
                    assertThat(((AgenticToolResult<?>) result).success()).isFalse();
                })
                .verifyComplete();
    }
}
