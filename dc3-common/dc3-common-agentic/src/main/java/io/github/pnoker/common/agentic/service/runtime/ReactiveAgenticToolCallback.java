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
import io.github.pnoker.common.utils.JsonUtil;
import java.time.Duration;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Bridges a {@link ReactiveAgenticTool} into Spring AI's synchronous {@link ToolCallback}
 * SPI so the Spring AI chat client path sees exactly the same tool surface as the explicit
 * OpenAI-compatible loop — one registry, one set of tool definitions, both runtimes.
 *
 * <p>Spring AI executes tool callbacks on the calling thread. The Spring AI runtime
 * subscribes its whole chain (tool execution included) on the bounded-elastic scheduler,
 * so the blocking wait below never lands on a non-blocking event-loop thread; the guard
 * exists so a mis-wired caller fails loudly instead of dead-locking the loop.</p>
 *
 * @author pnoker
 * @since 2026.10.10
 */
public class ReactiveAgenticToolCallback implements ToolCallback {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);

    private final ReactiveAgenticTool tool;

    private final Scheduler scheduler = Schedulers.boundedElastic();

    public ReactiveAgenticToolCallback(ReactiveAgenticTool tool) {
        this.tool = tool;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return tool.definition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, new ToolContext(java.util.Map.of()));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        if (Schedulers.isInNonBlockingThread()) {
            throw new IllegalStateException(
                    "Agentic tool callback invoked on a non-blocking thread; the Spring AI chain must be "
                            + "subscribed on bounded elastic (see SpringAiAgenticRuntime)");
        }
        try {
            Object result = tool.call(toolInput, toolContext).subscribeOn(scheduler).block(CALL_TIMEOUT);
            return serialize(result);
        } catch (IllegalStateException error) {
            throw error;
        } catch (RuntimeException error) {
            Throwable unwrapped = Exceptions.unwrap(error);
            return serialize(AgenticToolResult.error(
                    unwrapped.getMessage() == null ? "Tool execution failed" : unwrapped.getMessage()));
        }
    }

    private String serialize(Object result) {
        if (result instanceof String text) {
            return text;
        }
        return JsonUtil.getJsonMapper().writeValueAsString(result);
    }
}
