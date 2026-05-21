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
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.utils.AgenticTokenEstimatorUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists user and assistant messages for the agentic chat pipeline.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class AgenticMessageRecorder {

    private final MessageService messageService;

    public void persistUserMessage(AgenticPreparedChatRequest prepared, RequestHeader.UserHeader userHeader) {
        messageService.save(prepared.scopedConversationId(), "user", buildUserContent(prepared), prepared.model(),
                userHeader);
    }

    public void persistAssistantMessage(AgenticPreparedChatRequest prepared, String content,
                                        RequestHeader.UserHeader userHeader) {
        persistAssistantMessage(prepared, content, null, userHeader);
    }

    public void persistAssistantMessage(AgenticPreparedChatRequest prepared, String content, String reasoningContent,
                                        RequestHeader.UserHeader userHeader) {
        AgenticMessageContent messageContent = buildAssistantContent(prepared, StringUtils.defaultString(content),
                StringUtils.trimToNull(reasoningContent));
        if (!hasPersistableAssistantContent(messageContent)) {
            return;
        }
        messageService.save(prepared.scopedConversationId(), AgenticConstant.Chat.ROLE_ASSISTANT, messageContent,
                prepared.model(), userHeader);
    }

    private AgenticMessageContent buildUserContent(AgenticPreparedChatRequest prepared) {
        AgenticMessageContent content = AgenticMessageContent.ofText(prepared.userMessage());
        if (!prepared.attachments().isEmpty()) {
            content.setAttachments(prepared.attachments());
        }
        return content;
    }

    private AgenticMessageContent buildAssistantContent(AgenticPreparedChatRequest prepared, String text,
                                                        String reasoningContent) {
        List<AgenticRunEvent> runEvents = drainRunEvents(prepared);
        List<String> tools = runEvents.stream()
                .filter(event -> AgenticConstant.RunEvent.TYPE_TOOL.equals(event.type()))
                .map(AgenticRunEvent::name)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        AgenticMessageContent content = AgenticMessageContent.ofText(text);
        content.setFormat("markdown");
        content.setTools(tools);
        content.setTraces(buildTraceEvents(prepared, runEvents));
        content.setCharts(prepared.runTrace().drainAndRecordedVisualizations());
        content.setReasoning(prepared.reasoning());
        content.setReasoningContent(reasoningContent);
        content.setContexts(prepared.contexts());
        content.setTokens(outputTokens(prepared.inputTokens(), text));
        return content;
    }

    private boolean hasPersistableAssistantContent(AgenticMessageContent content) {
        return StringUtils.isNotBlank(content.getText())
                || Boolean.TRUE.equals(content.getReasoning())
                || StringUtils.isNotBlank(content.getReasoningContent())
                || hasItems(content.getTools())
                || hasItems(content.getTraces())
                || hasItems(content.getCharts())
                || hasItems(content.getContexts());
    }

    private boolean hasItems(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private List<AgenticMessageContent.Trace> buildTraceEvents(AgenticPreparedChatRequest prepared,
                                                               List<AgenticRunEvent> runEvents) {
        List<AgenticMessageContent.Trace> traces = new ArrayList<>();
        if (prepared.reasoning()) {
            traces.add(traceOf(AgenticRunEvent.reasoningRequested()));
        }
        for (AgenticRunEvent event : runEvents) {
            traces.add(traceOf(event));
        }
        return traces;
    }

    private AgenticMessageContent.Trace traceOf(AgenticRunEvent event) {
        return AgenticMessageContent.Trace.of(event.type(), event.title(), event.detail(), event.name(),
                event.timestamp() / 1000, event.phase(), event.status(), event.code());
    }

    private List<AgenticRunEvent> drainRunEvents(AgenticPreparedChatRequest prepared) {
        return prepared.runTrace().drainAndRecordedEvents();
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
