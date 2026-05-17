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

import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adds structured runtime tracing around Spring AI tool callbacks.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
public class AgenticToolTracingCallback implements ToolCallback {

    private final ToolCallback delegate;

    private final ObjectMapper objectMapper;

    private final AgenticToolTraceMetadata traceMetadata;

    public AgenticToolTracingCallback(ToolCallback delegate, ObjectMapper objectMapper) {
        this(delegate, objectMapper, null);
    }

    public AgenticToolTracingCallback(ToolCallback delegate, ObjectMapper objectMapper,
                                      AgenticToolTraceMetadata traceMetadata) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.traceMetadata = traceMetadata;
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
        recordStart(toolContext, toolName);
        try {
            String result = delegate.call(toolInput, toolContext);
            recordResult(toolContext, toolName, result);
            return result;
        } catch (RuntimeException e) {
            AgenticToolContextUtil.recordToolError(toolContext, toolName, StringUtils.defaultIfBlank(e.getMessage(),
                    AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED));
            throw e;
        }
    }

    private void recordStart(ToolContext toolContext, String toolName) {
        AgenticToolTraceMetadata metadata = Objects.nonNull(traceMetadata)
                ? traceMetadata
                : new AgenticToolTraceMetadata(AgenticConstant.RunEvent.TYPE_TOOL, toolName);
        AgenticToolContextUtil.recordToolInvocation(toolContext, toolName, metadata.domain(),
                StringUtils.defaultIfBlank(metadata.title(), toolName));
    }

    private void recordResult(ToolContext toolContext, String toolName, String result) {
        ToolResultSummary summary = parseResult(result);
        AgenticToolContextUtil.recordToolResult(toolContext, toolName, summary.success(), summary.code(),
                summary.message());
        AgenticToolContextUtil.recordVisualizations(toolContext, parseVisualizations(result));
    }

    private ToolResultSummary parseResult(String result) {
        if (StringUtils.isBlank(result)) {
            return new ToolResultSummary(true, AgenticConstant.ToolResult.CODE_OK,
                    AgenticConstant.ToolResult.MESSAGE_COMPLETED);
        }
        try {
            JsonNode root = objectMapper.readTree(result);
            if (Objects.isNull(root) || !root.isObject() || !root.has("success")) {
                return new ToolResultSummary(true, AgenticConstant.ToolResult.CODE_OK,
                        AgenticConstant.ToolResult.MESSAGE_COMPLETED);
            }
            boolean success = root.path("success").asBoolean(false);
            String code = StringUtils.defaultIfBlank(root.path("code").asString(),
                    success ? AgenticConstant.ToolResult.CODE_OK : AgenticConstant.ToolResult.CODE_ERROR);
            String message = StringUtils.defaultIfBlank(root.path("message").asString(),
                    AgenticConstant.ToolResult.MESSAGE_COMPLETED);
            return new ToolResultSummary(success, code, message);
        } catch (JacksonException e) {
            log.debug("Agentic tool result trace parse skipped, tool={}, resultLen={}", toolName(),
                    result.length(), e);
            return new ToolResultSummary(true, AgenticConstant.ToolResult.CODE_OK,
                    AgenticConstant.ToolResult.MESSAGE_COMPLETED);
        }
    }

    private List<AgenticVisualizationSpec> parseVisualizations(String result) {
        if (StringUtils.isBlank(result)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(result);
            JsonNode visualizations = root.path(AgenticConstant.ToolResult.FIELD_VISUALIZATIONS);
            if (!visualizations.isArray() || visualizations.isEmpty()) {
                return List.of();
            }
            List<AgenticVisualizationSpec> specs = new ArrayList<>();
            for (JsonNode node : visualizations) {
                AgenticVisualizationSpec spec = objectMapper.treeToValue(node, AgenticVisualizationSpec.class);
                if (Objects.nonNull(spec)) {
                    specs.add(spec);
                }
            }
            return List.copyOf(specs);
        } catch (JacksonException e) {
            log.debug("Agentic tool visualization parse skipped, tool={}, resultLen={}", toolName(),
                    result.length(), e);
            return List.of();
        }
    }

    private String toolName() {
        ToolDefinition definition = getToolDefinition();
        return Objects.nonNull(definition)
                ? Objects.toString(definition.name(), AgenticConstant.RunEvent.TYPE_TOOL)
                : AgenticConstant.RunEvent.TYPE_TOOL;
    }

    private record ToolResultSummary(boolean success, String code, String message) {
    }

}
