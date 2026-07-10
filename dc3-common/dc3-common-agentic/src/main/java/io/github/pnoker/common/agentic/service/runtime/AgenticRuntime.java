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

import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatBO;
import reactor.core.publisher.Flux;

/**
 * Executes the model/tool runtime for one prepared agentic chat turn.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
public interface AgenticRuntime {

    /**
     * Run the model/tool loop as a stream, emitting one frame per delta until the model
     * stops calling tools.
     *
     * @param prepared the prepared chat request
     * @return a flux of stream frames
     */
    Flux<AgenticRuntimeStreamFrame> stream(AgenticPreparedChatBO prepared);

    /**
     * Run the model/tool loop to completion, returning the final assistant answer.
     *
     * @param prepared the prepared chat request
     * @return the final result and finish reason
     */
    AgenticRuntimeResult call(AgenticPreparedChatBO prepared);

}
