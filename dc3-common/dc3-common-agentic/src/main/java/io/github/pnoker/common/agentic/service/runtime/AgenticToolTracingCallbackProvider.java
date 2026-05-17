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
import io.github.pnoker.common.constant.service.AgenticConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps Spring AI tool callbacks with structured tracing.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
public class AgenticToolTracingCallbackProvider implements ToolCallbackProvider {

    private final ToolCallback[] callbacks;

    public AgenticToolTracingCallbackProvider(ToolCallbackProvider delegate, ObjectMapper objectMapper) {
        this(delegate, objectMapper, new Object[0]);
    }

    public AgenticToolTracingCallbackProvider(ToolCallbackProvider delegate, ObjectMapper objectMapper,
                                              Object... toolObjects) {
        Map<String, AgenticToolTraceMetadata> metadataByName = resolveMetadata(toolObjects);
        this.callbacks = Arrays.stream(delegate.getToolCallbacks())
                .map(callback -> new AgenticToolTracingCallback(callback, objectMapper,
                        metadataByName.get(toolName(callback))))
                .toArray(ToolCallback[]::new);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.copyOf(callbacks, callbacks.length);
    }

    private Map<String, AgenticToolTraceMetadata> resolveMetadata(Object... toolObjects) {
        Map<String, AgenticToolTraceMetadata> metadataByName = new HashMap<>();
        if (Objects.isNull(toolObjects)) {
            return metadataByName;
        }
        for (Object toolObject : toolObjects) {
            if (Objects.isNull(toolObject)) {
                continue;
            }
            for (Method method : toolObject.getClass().getDeclaredMethods()) {
                AgenticToolMetadata metadata = method.getAnnotation(AgenticToolMetadata.class);
                if (Objects.isNull(metadata)) {
                    continue;
                }
                AgenticToolTraceMetadata traceMetadata = new AgenticToolTraceMetadata(metadata.domain(),
                        metadata.title());
                metadataByName.put(method.getName(), traceMetadata);
                Tool tool = method.getAnnotation(Tool.class);
                if (Objects.nonNull(tool) && StringUtils.isNotBlank(tool.name())) {
                    metadataByName.put(tool.name(), traceMetadata);
                }
            }
        }
        return metadataByName;
    }

    private String toolName(ToolCallback callback) {
        ToolDefinition definition = callback.getToolDefinition();
        if (Objects.isNull(definition) || StringUtils.isBlank(definition.name())) {
            return AgenticConstant.RunEvent.TYPE_TOOL;
        }
        return definition.name();
    }

}
