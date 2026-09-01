package io.github.pnoker.common.agentic.service.runtime;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.CancellationException;

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
            return delegate.call(arguments, context).cast(Object.class)
                    .doOnNext(result -> recordResult(context, result))
                    .doOnError(error -> AgenticToolContextUtil.recordToolError(context, name,
                            error.getMessage() == null ? AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED : error.getMessage()))
                    .onErrorResume(error -> error instanceof CancellationException
                            ? Mono.error(error)
                            : Mono.just((Object) AgenticToolResult.error(error.getMessage() == null
                            ? AgenticConstant.ToolResult.MESSAGE_EXECUTION_FAILED : error.getMessage())));
        });
    }

    private String domain() {
        String value = name;
        for (String prefix : new String[]{"lookup", "search", "list", "get", "read", "write"}) {
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
            AgenticToolContextUtil.recordToolResult(context, name, toolResult.success(), toolResult.code(), toolResult.message());
            AgenticToolContextUtil.recordVisualizations(context, toolResult.visualizations());
            return;
        }
        AgenticToolContextUtil.recordToolResult(context, name, true, AgenticConstant.ToolResult.CODE_OK,
                AgenticConstant.ToolResult.MESSAGE_COMPLETED);
    }
}
