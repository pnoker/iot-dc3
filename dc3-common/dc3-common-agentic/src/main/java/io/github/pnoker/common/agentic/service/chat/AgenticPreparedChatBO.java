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

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import java.util.List;
import java.util.Map;

/**
 * Immutable request state shared by the chat orchestration pipeline.
 *
 * <p>{@code memoryHistory} is the <b>replay transcript</b> for the turn: either the
 * client-provided prior turns (when the client manages its own context) or the
 * platform-persisted history loaded per the memory window. Runtimes replay it ahead of
 * {@code userMessage}; trailing user turns adjacent to the prompt are suppressed by the
 * runtime to avoid double-prompting.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public record AgenticPreparedChatBO(
        String userMessage,
        String conversationId,
        String requestSystemContext,
        String model,
        Map<String, Object> toolContext,
        Double temperature,
        Integer maxTokens,
        AgenticRunTrace runTrace,
        boolean toolCallingEnabled,
        boolean reasoning,
        List<Long> attachments,
        List<AgenticMessageContent.Context> contexts,
        AgenticMessageContent.Tokens inputTokens,
        List<MessageBO> memoryHistory) {}
