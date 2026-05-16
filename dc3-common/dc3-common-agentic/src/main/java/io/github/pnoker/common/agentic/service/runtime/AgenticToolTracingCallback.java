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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Adds structured runtime tracing around Spring AI tool callbacks.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@SuppressWarnings("deprecation")
public class AgenticToolTracingCallback implements ToolCallback {

    private final ToolCallback delegate;

    private final ObjectMapper objectMapper;

    public AgenticToolTracingCallback(ToolCallback delegate, ObjectMapper objectMapper) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String toolName = toolName();
        try {
            String result = delegate.call(toolInput, toolContext);
            recordResult(toolContext, toolName, result);
            return result;
        } catch (RuntimeException e) {
            AgenticRequestContext.recordToolError(toolContext, toolName, StringUtils.defaultIfBlank(e.getMessage(),
                    "Tool execution failed"));
            throw e;
        }
    }

    private void recordResult(ToolContext toolContext, String toolName, String result) {
        ToolResultSummary summary = parseResult(result);
        AgenticRequestContext.recordToolResult(toolContext, toolName, summary.success(), summary.code(),
                summary.message());
    }

    private ToolResultSummary parseResult(String result) {
        if (StringUtils.isBlank(result)) {
            return new ToolResultSummary(true, "OK", "Tool completed");
        }
        try {
            JsonNode root = objectMapper.readTree(result);
            if (Objects.isNull(root) || !root.isObject() || !root.has("success")) {
                return new ToolResultSummary(true, "OK", "Tool completed");
            }
            boolean success = root.path("success").asBoolean(false);
            String code = StringUtils.defaultIfBlank(root.path("code").asString(), success ? "OK" : "ERROR");
            String message = StringUtils.defaultIfBlank(root.path("message").asString(), "Tool completed");
            return new ToolResultSummary(success, code, message);
        } catch (JacksonException e) {
            log.debug("Agentic tool result trace parse skipped, tool={}, resultLen={}", toolName(),
                    result.length(), e);
            return new ToolResultSummary(true, "OK", "Tool completed");
        }
    }

    private String toolName() {
        ToolDefinition definition = getToolDefinition();
        return Objects.nonNull(definition) ? StringUtils.defaultString(definition.name(), "tool") : "tool";
    }

    private record ToolResultSummary(boolean success, String code, String message) {
    }

}
