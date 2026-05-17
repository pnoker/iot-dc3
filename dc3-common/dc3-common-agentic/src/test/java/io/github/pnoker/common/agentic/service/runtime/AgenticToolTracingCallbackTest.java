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

import io.github.pnoker.common.agentic.annotation.AgenticToolMetadata;
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.constant.service.AgenticConstant;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
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
        assertThat(events).hasSize(2);
        AgenticRunEvent startEvent = events.poll();
        assertThat(startEvent.name()).isEqualTo("lookupDeviceById");
        assertThat(startEvent.phase()).isEqualTo("start");
        assertThat(startEvent.status()).isEqualTo("running");
        AgenticRunEvent resultEvent = events.poll();
        assertThat(resultEvent.name()).isEqualTo("lookupDeviceById");
        assertThat(resultEvent.phase()).isEqualTo("result");
        assertThat(resultEvent.status()).isEqualTo("failed");
        assertThat(resultEvent.code()).isEqualTo("INVALID_ARGUMENT");
        assertThat(resultEvent.title()).isEqualTo("Device ID is required");
    }

    @Test
    void recordsVisualizationsFromStructuredToolResult() {
        Queue<AgenticRunEvent> events = new ConcurrentLinkedQueue<>();
        Queue<AgenticVisualizationSpec> visualizations = new ConcurrentLinkedQueue<>();
        ToolContext context = new ToolContext(Map.of(
                AgenticConstant.ToolContextKey.RUN_EVENTS, events,
                AgenticConstant.ToolContextKey.VISUALIZATIONS, visualizations));
        ToolCallback callback = new AgenticToolTracingCallback(new StubToolCallback("""
                {"success":true,"code":"OK","message":"History loaded","visualizations":[{"id":"chart-1","type":"line","title":"Trend","dataset":[{"index":0,"value":23.5}],"encode":{"x":"index","y":"value"}}]}
                """), new ObjectMapper());

        callback.call("{}", context);

        assertThat(visualizations).hasSize(1);
        AgenticVisualizationSpec visualization = visualizations.poll();
        assertThat(visualization.getId()).isEqualTo("chart-1");
        assertThat(visualization.getType()).isEqualTo("line");
        assertThat(visualization.getEncode().getX()).isEqualTo("index");
        assertThat(visualization.getDataset()).hasSize(1);
    }

    @Test
    void providerUsesAgenticMetadataWhenRecordingToolStart() {
        Queue<AgenticRunEvent> events = new ConcurrentLinkedQueue<>();
        ToolContext context = new ToolContext(Map.of(AgenticConstant.ToolContextKey.RUN_EVENTS, events));
        ToolCallbackProvider provider = new AgenticToolTracingCallbackProvider(
                ToolCallbackProvider.from(new StubToolCallback(
                        "{\"success\":true,\"code\":\"OK\",\"message\":\"Device loaded\"}")),
                new ObjectMapper(), new MetadataFixtureTool());

        String result = provider.getToolCallbacks()[0].call("{}", context);

        assertThat(result).contains("Device loaded");
        assertThat(events).hasSize(2);
        AgenticRunEvent startEvent = events.poll();
        assertThat(startEvent.name()).isEqualTo("lookupDeviceById");
        assertThat(startEvent.title()).isEqualTo("Fixture lookup");
        assertThat(startEvent.detail()).isEqualTo("fixture");
        assertThat(startEvent.phase()).isEqualTo("start");
        assertThat(startEvent.status()).isEqualTo("running");
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

        assertThat(events).hasSize(2);
        AgenticRunEvent startEvent = events.poll();
        assertThat(startEvent.phase()).isEqualTo("start");
        assertThat(startEvent.status()).isEqualTo("running");
        AgenticRunEvent errorEvent = events.poll();
        assertThat(errorEvent.phase()).isEqualTo("error");
        assertThat(errorEvent.status()).isEqualTo("failed");
        assertThat(errorEvent.code()).isEqualTo("ERROR");
        assertThat(errorEvent.title()).isEqualTo("backend unavailable");
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

    private static class MetadataFixtureTool {

        @AgenticToolMetadata(domain = "fixture", title = "Fixture lookup")
        void lookupDeviceById() {
            // Metadata-only fixture.
        }

    }

}
