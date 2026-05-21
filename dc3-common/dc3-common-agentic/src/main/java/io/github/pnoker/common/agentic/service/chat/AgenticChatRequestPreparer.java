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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.config.ChatClientConfig;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.request.ChatMessageDTO;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.utils.AgenticConversationIdUtil;
import io.github.pnoker.common.agentic.utils.AgenticTokenEstimatorUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts an API chat request into validated, tenant-scoped orchestration state.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgenticChatRequestPreparer {

    private final ChatClientFactory chatClientFactory;

    private final SessionService sessionService;

    private final MessageService messageService;

    private final AttachmentService attachmentService;

    private final AgenticProperties properties;

    public AgenticPreparedChatRequest prepare(ChatCompletionRequest request, RequestHeader.UserHeader userHeader,
                                              String mode) {
        validateRequest(request);

        String rawUserMessage = extractLastUserMessage(request);
        List<Long> attachments = normalizeAttachments(request);
        String attachmentContext = attachmentService.summarize(attachments, userHeader);
        String conversationId = resolveConversationId(request);
        String scopedConversationId = AgenticConversationIdUtil.scope(userHeader.getTenantId(), userHeader.getUserId(),
                conversationId);
        String model = chatClientFactory.resolveModel(request.getModel());
        boolean modelSupportsToolCall = chatClientFactory.supportsToolCall(model);
        boolean toolCallingEnabled = properties.isToolCallingEnabled() && modelSupportsToolCall;
        if (!toolCallingEnabled) {
            log.warn(
                    "Agentic tool calling disabled for request, requestedModel={}, resolvedModel={}, globalEnabled={}, modelSupportsToolCall={}",
                    request.getModel(), model, properties.isToolCallingEnabled(), modelSupportsToolCall);
        }
        AgenticRunTrace runTrace = new AgenticRunTrace();
        Map<String, Object> toolContext = buildToolContext(userHeader, scopedConversationId, runTrace);

        List<AgenticMessageContent.Context> contexts = buildContexts(attachmentContext);
        String requestSystemContext = buildRequestSystemContext(contexts);
        List<MessageBO> memoryHistory = loadMemoryHistory(scopedConversationId);
        log.debug("Agentic memory loaded, scopedConversationId={}, memoryEnabled={}, count={}",
                scopedConversationId, properties.isMemoryEnabled(), memoryHistory.size());
        AgenticMessageContent.Tokens inputTokens = buildInputTokens(rawUserMessage, contexts, memoryHistory,
                toolCallingEnabled);

        log.debug(
                "Agentic chat request received, mode={}, requestedModel={}, resolvedModel={}, toolCallingEnabled={}, messageCount={}, conversationIdPresent={}, tenantId={}, userId={}",
                mode, request.getModel(), model, toolCallingEnabled, request.getMessages().size(),
                StringUtils.isNotBlank(request.getConversationId()), userHeader.getTenantId(), userHeader.getUserId());

        touchSession(scopedConversationId, conversationId, userHeader, buildSessionExt(request, model));

        return new AgenticPreparedChatRequest(rawUserMessage, scopedConversationId, requestSystemContext, model,
                toolContext, request.getTemperature(), request.getMaxTokens(), runTrace,
                toolCallingEnabled, Boolean.TRUE.equals(request.getReasoning()), attachments, contexts,
                inputTokens, memoryHistory);
    }

    private Map<String, Object> buildToolContext(RequestHeader.UserHeader userHeader, String scopedConversationId,
                                                 AgenticRunTrace runTrace) {
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put(AgenticConstant.ToolContextKey.TENANT_ID, userHeader.getTenantId());
        toolContext.put(AgenticConstant.ToolContextKey.USER_ID, userHeader.getUserId());
        toolContext.put(AgenticConstant.ToolContextKey.USER_HEADER, userHeader);
        toolContext.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, scopedConversationId);
        toolContext.put(AgenticConstant.ToolContextKey.RUN_EVENTS, runTrace.pendingEvents());
        toolContext.put(AgenticConstant.ToolContextKey.VISUALIZATIONS, runTrace.pendingVisualizations());
        return toolContext;
    }

    private void validateRequest(ChatCompletionRequest request) {
        if (Objects.isNull(request)) {
            throw new RequestException("Chat completion request is required");
        }
        if (Objects.isNull(request.getMessages()) || request.getMessages().isEmpty()) {
            throw new RequestException("Chat messages are required");
        }
        if (Objects.nonNull(request.getTemperature())
                && (request.getTemperature() < 0.0 || request.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (Objects.nonNull(request.getMaxTokens()) && request.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    private String extractLastUserMessage(ChatCompletionRequest request) {
        return request.getMessages()
                .stream()
                .filter(message -> Objects.nonNull(message) && "user".equals(message.getRole()))
                .map(ChatMessageDTO::getContent)
                .filter(StringUtils::isNotBlank)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new RequestException("A non-empty user message is required"));
    }

    private String resolveConversationId(ChatCompletionRequest request) {
        String conversationId = StringUtils.trimToNull(request.getConversationId());
        if (Objects.isNull(conversationId)) {
            throw new RequestException("conversationId is required - clients must generate and reuse "
                    + "a stable conversationId so chat memory can be replayed across turns.");
        }
        return conversationId;
    }

    private List<AgenticMessageContent.Context> buildContexts(String attachmentContext) {
        List<AgenticMessageContent.Context> contexts = new ArrayList<>();
        if (StringUtils.isNotBlank(attachmentContext)) {
            contexts.add(AgenticMessageContent.Context.of("attachment", attachmentContext.trim()));
        }
        return contexts;
    }

    private String buildRequestSystemContext(List<AgenticMessageContent.Context> contexts) {
        List<String> sections = new ArrayList<>();
        for (AgenticMessageContent.Context context : contexts) {
            if (Objects.isNull(context) || StringUtils.isBlank(context.getContent())) {
                continue;
            }
            if ("attachment".equals(context.getType())) {
                sections.add("Attachment context:\n" + context.getContent().trim());
            } else {
                sections.add(context.getContent().trim());
            }
        }
        return sections.isEmpty() ? null : String.join("\n\n", sections);
    }

    private AgenticMessageContent.Tokens buildInputTokens(String userMessage, List<AgenticMessageContent.Context> contexts,
                                                          List<MessageBO> memoryHistory,
                                                          boolean toolCallingEnabled) {
        int textTokens = AgenticTokenEstimatorUtil.estimate(userMessage);
        int contextTokens = contexts.stream()
                .map(AgenticMessageContent.Context::getContent)
                .mapToInt(AgenticTokenEstimatorUtil::estimate)
                .sum();
        int systemTokens = AgenticTokenEstimatorUtil.estimate(ChatClientConfig.BASE_SYSTEM_PROMPT)
                + (toolCallingEnabled ? AgenticTokenEstimatorUtil.estimate(ChatClientConfig.TOOL_SYSTEM_PROMPT) : 0);
        int memoryTokens = estimateMemoryTokens(memoryHistory);
        return AgenticMessageContent.Tokens.of(textTokens + contextTokens + systemTokens + memoryTokens, 0,
                textTokens, contextTokens, systemTokens, memoryTokens);
    }

    private List<MessageBO> loadMemoryHistory(String scopedConversationId) {
        if (!properties.isMemoryEnabled()) {
            return List.of();
        }
        try {
            return messageService.loadHistory(scopedConversationId, properties.getHistoryWindowSize());
        } catch (Exception e) {
            log.debug("Agentic memory history load failed, conversationId={}", scopedConversationId, e);
            return List.of();
        }
    }

    private int estimateMemoryTokens(List<MessageBO> history) {
        if (!properties.isMemoryEnabled() || Objects.isNull(history) || history.isEmpty()) {
            return 0;
        }
        return history.stream()
                .map(message -> Objects.nonNull(message.getContent()) ? message.getContent().getText() : null)
                .map(StringUtils::defaultString)
                .filter(StringUtils::isNotBlank)
                .mapToInt(AgenticTokenEstimatorUtil::estimate)
                .sum();
    }

    private SessionExt buildSessionExt(ChatCompletionRequest request, String model) {
        if (Objects.isNull(request.getReasoning()) && Objects.isNull(request.getTemperature())
                && Objects.isNull(request.getMaxTokens()) && StringUtils.isBlank(model)) {
            return null;
        }
        SessionExt sessionExt = new SessionExt();
        sessionExt.setModel(model);
        sessionExt.setReasoningEnabled(request.getReasoning());
        sessionExt.setTemperature(request.getTemperature());
        sessionExt.setMaxTokens(request.getMaxTokens());
        return sessionExt;
    }

    private void touchSession(String scopedConversationId, String conversationId, RequestHeader.UserHeader userHeader,
                              SessionExt sessionExt) {
        try {
            sessionService.touch(scopedConversationId, userHeader.getTenantId(), userHeader.getUserId(), sessionExt);
        } catch (Exception e) {
            log.warn(
                    "Agentic session touch failed, tenantId={}, userId={}, conversationId={}",
                    userHeader.getTenantId(), userHeader.getUserId(), conversationId, e);
        }
    }

    private List<Long> normalizeAttachments(ChatCompletionRequest request) {
        if (Objects.isNull(request.getAttachments()) || request.getAttachments().isEmpty()) {
            return List.of();
        }
        return request.getAttachments().stream().filter(Objects::nonNull).distinct().toList();
    }

}
