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

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Mono;

/** Adds non-blocking invocation tracing to a reactive agentic tool. */
final class ReactiveAgenticToolTracing implements ReactiveAgenticTool {
    private final ReactiveAgenticTool delegate;
    private final String name;

    ReactiveAgenticToolTracing(String name, ReactiveAgenticTool delegate) {
        this.name = Objects.requireNonNull(name, "name");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public ToolDefinition definition() {
        return delegate.definition();
    }

    @Override
    public Mono<?> call(String arguments, ToolContext context) {
        return Mono.defer(() -> {
            AgenticToolContextUtil.recordToolInvocation(context, name, domain(), name);
            return delegate.call(arguments, context)
                    .cast(Object.class)
                    .doOnNext(result -> recordResult(context, result))
                    .doOnError(error -> AgenticToolContextUtil.recordToolError(
                            context,
                            name,
                            error.getMessage() == null
                                    ? AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED
                                    : error.getMessage()))
                    .onErrorResume(error -> error instanceof CancellationException
                            ? Mono.error(error)
                            : Mono.just((Object) AgenticToolResult.error(
                                    error.getMessage() == null
                                            ? AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED
                                            : error.getMessage())));
        });
    }

    private String domain() {
        String value = name;
        for (String prefix : new String[] {"lookup", "search", "list", "get", "read", "write"}) {
            if (value.startsWith(prefix) && value.length() > prefix.length()) {
                value = value.substring(prefix.length());
                break;
            }
        }
        int separator = value.indexOf("By");
        return (separator > 0 ? value.substring(0, separator) : value).toLowerCase();
    }

    private void recordResult(ToolContext context, Object result) {
        if (result instanceof AgenticToolResult<?> toolResult) {
            AgenticToolContextUtil.recordToolResult(
                    context, name, toolResult.success(), toolResult.code(), toolResult.message());
            AgenticToolContextUtil.recordVisualizations(context, toolResult.visualizations());
            return;
        }
        AgenticToolContextUtil.recordToolResult(
                context, name, true, AgenticConstant.ToolResult.CODE_OK, AgenticConstant.ToolResult.MESSAGE_COMPLETED);
    }
}
