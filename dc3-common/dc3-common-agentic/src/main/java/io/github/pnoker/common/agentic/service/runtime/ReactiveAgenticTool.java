/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.runtime;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Mono;

/**
 * Native asynchronous agent tool contract.
 *
 * <p>Spring AI's reflective method-tool contract is synchronous. Tools that touch
 * platform services therefore implement this port and are executed directly by the
 * explicit provider loop instead of being wrapped in a blocking callback.</p>
 */
public interface ReactiveAgenticTool {

    ToolDefinition definition();

    Mono<?> call(String arguments, ToolContext context);
}
