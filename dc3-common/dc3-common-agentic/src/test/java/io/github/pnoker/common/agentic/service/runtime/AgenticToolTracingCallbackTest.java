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

import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.constant.service.AgenticConstant;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticToolTracingCallbackTest {

    @Test
    void recordsStructuredToolResultFromAgenticEnvelope() {
        Queue<AgenticRunEvent> events = new ConcurrentLinkedQueue<>();
        ToolContext context = new ToolContext(Map.of(AgenticConstant.ToolContextKey.RUN_EVENTS, events));
        ToolCallback callback = new AgenticToolTracingCallback(new StubToolCallback(
                "{\"success\":false,\"code\":\"INVALID_ARGUMENT\",\"message\":\"Device ID is required\"}"),
                new ObjectMapper());

        String result = callback.call("{}", context);

        assertThat(result).contains("INVALID_ARGUMENT");
        assertThat(events).hasSize(1);
        AgenticRunEvent event = events.poll();
        assertThat(event.name()).isEqualTo("lookupDeviceById");
        assertThat(event.phase()).isEqualTo("result");
        assertThat(event.status()).isEqualTo("failed");
        assertThat(event.code()).isEqualTo("INVALID_ARGUMENT");
        assertThat(event.title()).isEqualTo("Device ID is required");
    }

    @Test
    void recordsToolErrorWhenDelegateThrows() {
        Queue<AgenticRunEvent> events = new ConcurrentLinkedQueue<>();
        ToolContext context = new ToolContext(Map.of(AgenticConstant.ToolContextKey.RUN_EVENTS, events));
        ToolCallback callback = new AgenticToolTracingCallback(new ThrowingToolCallback(), new ObjectMapper());

        try {
            callback.call("{}", context);
        } catch (IllegalStateException ignored) {
            // Expected test path.
        }

        assertThat(events).hasSize(1);
        AgenticRunEvent event = events.poll();
        assertThat(event.phase()).isEqualTo("error");
        assertThat(event.status()).isEqualTo("failed");
        assertThat(event.code()).isEqualTo("ERROR");
        assertThat(event.title()).isEqualTo("backend unavailable");
    }

    private static class StubToolCallback implements ToolCallback {

        private final String result;

        private StubToolCallback(String result) {
            this.result = result;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return DefaultToolDefinition.builder()
                    .name("lookupDeviceById")
                    .description("Query device by ID")
                    .inputSchema("{}")
                    .build();
        }

        @Override
        public String call(String toolInput) {
            return result;
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return result;
        }

    }

    private static class ThrowingToolCallback extends StubToolCallback {

        private ThrowingToolCallback() {
            super("");
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            throw new IllegalStateException("backend unavailable");
        }

    }

}
