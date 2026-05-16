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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.utils.AgenticTokenEstimatorUtil;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persists user and assistant messages for the agentic chat pipeline.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Component
public class AgenticMessageRecorder {

    private final MessageService messageService;

    public AgenticMessageRecorder(MessageService messageService) {
        this.messageService = messageService;
    }

    public void persistUserMessage(AgenticPreparedChatRequest prepared, RequestHeader.UserHeader userHeader) {
        messageService.save(prepared.scopedConversationId(), "user", buildUserContent(prepared), prepared.model(),
                userHeader);
    }

    public void persistAssistantMessage(AgenticPreparedChatRequest prepared, String content,
                                        RequestHeader.UserHeader userHeader) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        messageService.save(prepared.scopedConversationId(), "assistant", buildAssistantContent(prepared, content),
                prepared.model(), userHeader);
    }

    private AgenticMessageContent buildUserContent(AgenticPreparedChatRequest prepared) {
        AgenticMessageContent content = AgenticMessageContent.ofText(prepared.userMessage());
        if (!prepared.attachments().isEmpty()) {
            content.setAttachments(prepared.attachments());
        }
        return content;
    }

    private AgenticMessageContent buildAssistantContent(AgenticPreparedChatRequest prepared, String text) {
        List<AgenticRequestContext.ToolEvent> toolEvents = drainToolEvents(prepared);
        List<String> tools = toolEvents.stream()
                .filter(event -> !"agentic".equals(event.domain()))
                .map(AgenticRequestContext.ToolEvent::toolName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        AgenticMessageContent content = AgenticMessageContent.ofText(text);
        content.setFormat("markdown");
        content.setTools(tools);
        content.setTraces(buildTraceEvents(prepared, toolEvents));
        content.setReasoning(prepared.reasoning());
        content.setContexts(prepared.contexts());
        content.setTokens(outputTokens(prepared.inputTokens(), text));
        return content;
    }

    private List<AgenticMessageContent.Trace> buildTraceEvents(AgenticPreparedChatRequest prepared,
                                                               List<AgenticRequestContext.ToolEvent> toolEvents) {
        List<AgenticMessageContent.Trace> traces = new ArrayList<>();
        long created = Instant.now().getEpochSecond();
        if (prepared.reasoning()) {
            traces.add(AgenticMessageContent.Trace.of("reasoning", "Thinking",
                    "Reasoning mode requested for this model.", "agentic", created));
        }
        for (AgenticRequestContext.ToolEvent event : toolEvents) {
            traces.add(AgenticMessageContent.Trace.of("tool", event.description(), event.detail(), event.toolName(),
                    event.timestamp() / 1000, event.phase(), event.status(), event.code()));
        }
        return traces;
    }

    private List<AgenticRequestContext.ToolEvent> drainToolEvents(AgenticPreparedChatRequest prepared) {
        AgenticRequestContext.ToolEvent event = prepared.toolEvents().poll();
        while (Objects.nonNull(event)) {
            prepared.toolTraceEvents().add(event);
            event = prepared.toolEvents().poll();
        }
        return prepared.toolTraceEvents();
    }

    private AgenticMessageContent.Tokens outputTokens(AgenticMessageContent.Tokens inputTokens, String assistantText) {
        int outputTokens = AgenticTokenEstimatorUtil.estimate(assistantText);
        AgenticMessageContent.Tokens tokens = new AgenticMessageContent.Tokens();
        tokens.setInput(inputTokens.getInput());
        tokens.setOutput(outputTokens);
        tokens.setText(inputTokens.getText());
        tokens.setContext(inputTokens.getContext());
        tokens.setSystem(inputTokens.getSystem());
        tokens.setMemory(inputTokens.getMemory());
        return tokens;
    }

}
