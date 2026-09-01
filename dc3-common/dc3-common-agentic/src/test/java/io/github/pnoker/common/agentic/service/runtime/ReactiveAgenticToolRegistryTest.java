package io.github.pnoker.common.agentic.service.runtime;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.tools.CommandTool;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import io.github.pnoker.common.agentic.tools.EventTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReactiveAgenticToolRegistryTest {
    @Test
    void allRegisteredToolsAreUniqueAndUseOffsetPagination() {
        var registry = new ReactiveAgenticToolRegistry(mock(PointValueTool.class), mock(PointTool.class),
                mock(ProfileTool.class), mock(CommandTool.class), mock(EventTool.class), mock(DeviceTool.class),
                mock(DriverTool.class), new ObjectMapper());
        var tools = registry.tools();
        assertThat(tools.keySet()).doesNotHaveDuplicates();
        tools.values().stream().filter(tool -> tool.definition().name().startsWith("search")
                        || tool.definition().name().startsWith("list"))
                .forEach(tool -> {
                    assertThat(tool.definition().inputSchema()).contains("offset", "limit");
                    assertThat(tool.definition().inputSchema()).doesNotContain("current", "size", "page");
                });
    }

    @Test
    void malformedArgumentsBecomeStructuredErrors() {
        var registry = new ReactiveAgenticToolRegistry(mock(PointValueTool.class), mock(PointTool.class),
                mock(ProfileTool.class), mock(CommandTool.class), mock(EventTool.class), mock(DeviceTool.class),
                mock(DriverTool.class), new ObjectMapper());
        StepVerifier.create(registry.tools().get("searchDevices").call("{", new ToolContext(Map.of())))
                .assertNext(result -> {
                    assertThat(result).isInstanceOf(AgenticToolResult.class);
                    assertThat(((AgenticToolResult<?>) result).success()).isFalse();
                }).verifyComplete();
    }
}
