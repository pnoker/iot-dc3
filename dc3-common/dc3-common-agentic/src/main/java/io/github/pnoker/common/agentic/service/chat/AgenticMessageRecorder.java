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
import io.github.pnoker.common.agentic.repository.ReactiveMessageStore;
import io.github.pnoker.common.agentic.utils.AgenticTokenEstimatorUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Persists user and assistant messages for the agentic chat pipeline.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class AgenticMessageRecorder {

    private final ReactiveMessageStore messageStore;

    /**
     * Persist user message.
     *
     * @param prepared   prepared
     * @param userHeader user header
     */
    public Mono<Void> persistUserMessage(AgenticPreparedChatBO prepared, RequestHeader.PrincipalHeader userHeader) {
        return messageStore
                .save(
                        prepared.conversationId(),
                        "user",
                        buildUserContent(prepared),
                        prepared.model(),
                        AgenticMessageStatusEnum.COMPLETED,
                        userHeader)
                .then();
    }

    /**
     * Persist assistant message.
     *
     * @param prepared   prepared
     * @param content    content
     * @param userHeader user header
     */
    public Mono<Void> persistAssistantMessage(
            AgenticPreparedChatBO prepared, String content, RequestHeader.PrincipalHeader userHeader) {
        return persistAssistantMessage(prepared, content, null, AgenticMessageStatusEnum.COMPLETED, userHeader);
    }

    /**
     * Persist the assistant message for a turn. Builds the assistant content (text,
     * reasoning, tool trace), and skips persistence entirely when nothing is
     * persistable.
     *
     * @param prepared         the prepared chat
     * @param content          the assistant text content
     * @param reasoningContent the reasoning trace, may be null
     * @param userHeader       authenticated caller principal and tenant
     */
    public Mono<Void> persistAssistantMessage(
            AgenticPreparedChatBO prepared,
            String content,
            String reasoningContent,
            RequestHeader.PrincipalHeader userHeader) {
        return persistAssistantMessage(
                prepared, content, reasoningContent, AgenticMessageStatusEnum.COMPLETED, userHeader);
    }

    /** Persist the assistant reply and run bookkeeping for the exchange. */
    public Mono<Void> persistAssistantMessage(
            AgenticPreparedChatBO prepared,
            String content,
            String reasoningContent,
            AgenticMessageStatusEnum status,
            RequestHeader.PrincipalHeader userHeader) {
        AgenticMessageContent messageContent = buildAssistantContent(
                prepared, StringUtils.defaultString(content), StringUtils.trimToNull(reasoningContent));
        if (status == AgenticMessageStatusEnum.COMPLETED && !hasPersistableAssistantContent(messageContent)) {
            return Mono.empty();
        }
        return messageStore
                .save(
                        prepared.conversationId(),
                        AgenticConstant.Chat.ROLE_ASSISTANT,
                        messageContent,
                        prepared.model(),
                        status,
                        userHeader)
                .then();
    }

    private AgenticMessageContent buildUserContent(AgenticPreparedChatBO prepared) {
        AgenticMessageContent content = AgenticMessageContent.ofText(prepared.userMessage());
        if (!prepared.attachments().isEmpty()) {
            content.setAttachments(prepared.attachments());
        }
        return content;
    }

    private AgenticMessageContent buildAssistantContent(
            AgenticPreparedChatBO prepared, String text, String reasoningContent) {
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

    private List<AgenticMessageContent.Trace> buildTraceEvents(
            AgenticPreparedChatBO prepared, List<AgenticRunEvent> runEvents) {
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
        return AgenticMessageContent.Trace.of(
                event.type(),
                event.title(),
                event.detail(),
                event.name(),
                event.timestamp() / 1000,
                event.phase(),
                event.status(),
                event.code());
    }

    private List<AgenticRunEvent> drainRunEvents(AgenticPreparedChatBO prepared) {
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
