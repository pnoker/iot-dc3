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

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

/**
 * Wraps Spring AI tool callbacks with structured tracing.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public class AgenticToolTracingCallbackProvider implements ToolCallbackProvider {

    private final ToolCallback[] callbacks;

    public AgenticToolTracingCallbackProvider(ToolCallbackProvider delegate, ObjectMapper objectMapper) {
        this.callbacks = Arrays.stream(delegate.getToolCallbacks())
                .map(callback -> new AgenticToolTracingCallback(callback, objectMapper))
                .toArray(ToolCallback[]::new);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.copyOf(callbacks, callbacks.length);
    }

}
