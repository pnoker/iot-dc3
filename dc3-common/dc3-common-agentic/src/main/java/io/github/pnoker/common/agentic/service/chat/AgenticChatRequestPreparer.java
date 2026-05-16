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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.config.ChatClientConfig;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Converts an API chat request into validated, tenant-scoped orchestration state.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AgenticChatRequestPreparer {

    private final ChatClientFactory chatClientFactory;

    private final SessionService sessionService;

    private final MessageService messageService;

    private final AttachmentService attachmentService;

    private final AgenticProperties properties;

    public AgenticChatRequestPreparer(ChatClientFactory chatClientFactory, SessionService sessionService,
                                      MessageService messageService,
                                      AttachmentService attachmentService,
                                      AgenticProperties properties) {
        this.chatClientFactory = chatClientFactory;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.attachmentService = attachmentService;
        this.properties = properties;
    }

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
        boolean toolCallingEnabled = properties.isToolCallingEnabled() && chatClientFactory.supportsToolCall(model);
        Queue<AgenticRunEvent> runEvents = new ConcurrentLinkedQueue<>();
        Map<String, Object> toolContext = buildToolContext(request, userHeader, scopedConversationId, runEvents);

        List<AgenticMessageContent.Context> contexts = buildContexts(attachmentContext);
        String requestSystemContext = buildRequestSystemContext(contexts);
        List<MessageBO> memoryHistory = loadMemoryHistory(scopedConversationId);
        AgenticRequestContext.setMemoryHistory(scopedConversationId, memoryHistory);
        log.debug("Agentic memory loaded, scopedConversationId={}, memoryEnabled={}, count={}",
                scopedConversationId, properties.isMemoryEnabled(), memoryHistory.size());
        AgenticMessageContent.Tokens inputTokens = buildInputTokens(rawUserMessage, requestSystemContext, contexts,
                memoryHistory);

        log.debug(
                "Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, tenantId={}, userId={}",
                mode, model, request.getMessages().size(), StringUtils.isNotBlank(request.getConversationId()),
                userHeader.getTenantId(), userHeader.getUserId());

        touchSession(scopedConversationId, conversationId, userHeader, buildSessionExt(request, model));

        return new AgenticPreparedChatRequest(rawUserMessage, scopedConversationId, requestSystemContext, model,
                toolContext, request.getTemperature(), request.getMaxTokens(), runEvents,
                toolCallingEnabled, Boolean.TRUE.equals(request.getReasoning()), attachments, contexts, inputTokens,
                new ArrayList<>());
    }

    private Map<String, Object> buildToolContext(ChatCompletionRequest request, RequestHeader.UserHeader userHeader,
                                                 String scopedConversationId,
                                                 Queue<AgenticRunEvent> runEvents) {
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put(AgenticConstant.ToolContextKey.TENANT_ID, userHeader.getTenantId());
        toolContext.put(AgenticConstant.ToolContextKey.USER_ID, userHeader.getUserId());
        toolContext.put(AgenticConstant.ToolContextKey.CONVERSATION_ID, scopedConversationId);
        toolContext.put(AgenticConstant.ToolContextKey.RUN_EVENTS, runEvents);
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
                sections.add("Attachment context:\n" + context.getContent().trim()
                        + "\n\nUse only the metadata above unless a future multimodal model endpoint provides file contents.");
            } else {
                sections.add(context.getContent().trim());
            }
        }
        return sections.isEmpty() ? null : String.join("\n\n", sections);
    }

    private AgenticMessageContent.Tokens buildInputTokens(String userMessage, String requestSystemContext,
                                                          List<AgenticMessageContent.Context> contexts,
                                                          List<MessageBO> memoryHistory) {
        int textTokens = AgenticTokenEstimatorUtil.estimate(userMessage);
        int contextTokens = contexts.stream()
                .map(AgenticMessageContent.Context::getContent)
                .mapToInt(AgenticTokenEstimatorUtil::estimate)
                .sum();
        int systemTokens = AgenticTokenEstimatorUtil.estimate(ChatClientConfig.SYSTEM_PROMPT)
                + AgenticTokenEstimatorUtil.estimate(systemInstructions(requestSystemContext, contexts));
        int memoryTokens = estimateMemoryTokens(memoryHistory);
        return AgenticMessageContent.Tokens.of(textTokens + contextTokens + systemTokens + memoryTokens, 0,
                textTokens, contextTokens, systemTokens, memoryTokens);
    }

    private String systemInstructions(String requestSystemContext, List<AgenticMessageContent.Context> contexts) {
        if (StringUtils.isBlank(requestSystemContext)) {
            return "";
        }
        List<String> instructions = new ArrayList<>();
        if (contexts.stream().anyMatch(context -> "attachment".equals(context.getType()))) {
            instructions.add("Use attachment metadata only unless a future multimodal model endpoint provides file contents.");
        }
        return String.join("\n", instructions);
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
