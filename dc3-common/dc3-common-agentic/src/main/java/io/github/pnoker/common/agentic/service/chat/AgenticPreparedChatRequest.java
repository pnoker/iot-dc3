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
package io.github.pnoker.common.agentic.service.chat;

import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Immutable request state shared by the chat orchestration pipeline.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
public record AgenticPreparedChatRequest(String userMessage, String scopedConversationId,
                                         String requestSystemContext, String model,
                                         Map<String, Object> toolContext, Double temperature,
                                         Integer maxTokens,
                                         Queue<AgenticRunEvent> runEvents,
                                         boolean toolCallingEnabled, boolean reasoning,
                                         List<Long> attachments, List<AgenticMessageContent.Context> contexts,
                                         AgenticMessageContent.Tokens inputTokens,
                                         List<AgenticRunEvent> runTraceEvents) {
}
