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
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    ReactiveAgenticToolTracing(String name, ReactiveAgenticTool delegate) {
        this(name, delegate, null);
    }

    ReactiveAgenticToolTracing(String name, ReactiveAgenticTool delegate,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.name = Objects.requireNonNull(name, "name");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ToolDefinition definition() {
        return delegate.definition();
    }

    @Override
    public String domain() {
        return delegate.domain();
    }

    /** Upper bound for an error message replayed to the model; internals stay in server logs. */
    private static final int ERROR_MESSAGE_MAX_CHARS = 300;

    @Override
    public Mono<?> call(String arguments, ToolContext context) {
        return Mono.defer(() -> {
            AgenticToolContextUtil.recordToolInvocation(context, name, delegate.domain(), name);
            long startNanos = System.nanoTime();
            return delegate.call(arguments, context)
                    .cast(Object.class)
                    .doOnNext(result -> {
                        recordResult(context, result);
                        recordMetrics(startNanos, outcomeOf(result));
                    })
                    .doOnError(error -> {
                        AgenticToolContextUtil.recordToolError(context, name, sanitizeErrorMessage(error));
                        recordMetrics(startNanos, "error");
                    })
                    .onErrorResume(error -> error instanceof CancellationException
                            ? Mono.error(error)
                            : Mono.just((Object) AgenticToolResult.error(sanitizeErrorMessage(error))));
        });
    }

    /** Map a tool envelope to a compact metric outcome tag. */
    private static String outcomeOf(Object result) {
        if (result instanceof AgenticToolResult<?> toolResult) {
            return toolResult.success() ? "ok" : toolResult.code().toLowerCase();
        }
        return "ok";
    }

    /** Record call counter and duration per tool/domain; absent registry is a no-op. */
    private void recordMetrics(long startNanos, String outcome) {
        if (meterRegistry == null) {
            return;
        }
        String domain = delegate.domain();
        meterRegistry
                .counter("agentic.tool.calls", "tool", name, "domain", domain, "outcome", outcome)
                .increment();
        meterRegistry
                .timer("agentic.tool.duration", "tool", name, "domain", domain)
                .record(java.time.Duration.ofNanos(System.nanoTime() - startNanos));
    }

    /**
     * Map an internal failure to a stable, bounded message before it is replayed to the
     * model and persisted: argument errors lose the raw exception text (it can echo
     * internal class and query details), everything else is capped in length. The full
     * stack trace remains in the server-side logs.
     */
    private static String sanitizeErrorMessage(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof NumberFormatException) {
            return "Invalid tool arguments";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED;
        }
        return message.length() <= ERROR_MESSAGE_MAX_CHARS
                ? message
                : message.substring(0, ERROR_MESSAGE_MAX_CHARS) + "...";
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
