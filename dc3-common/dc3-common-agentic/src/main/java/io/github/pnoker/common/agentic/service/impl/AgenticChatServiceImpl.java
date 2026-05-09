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
package io.github.pnoker.common.agentic.service.impl;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import io.github.pnoker.common.agentic.config.ChatClientConfig;
import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.request.ChatMessageDTO;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionChunkResponse;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.skill.SkillDefinition;
import io.github.pnoker.common.agentic.skill.SkillRegistry;
import io.github.pnoker.common.agentic.util.AgenticConversationIds;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default agentic chat orchestration service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AgenticChatServiceImpl implements AgenticChatService {

    private static final String DEFAULT_MODEL = "dc3-agentic";

    private final ChatClient chatClient;

    private final SkillRegistry skillRegistry;

    private final SessionService sessionService;

    private final ObjectMapper objectMapper;

    public AgenticChatServiceImpl(ChatClient chatClient, SkillRegistry skillRegistry, SessionService sessionService,
                                  ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.skillRegistry = skillRegistry;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChatCompletion(ChatCompletionRequest request,
                                                              RequestHeader.UserHeader userHeader) {
        return Flux.defer(() -> {
            PreparedChatRequest prepared = prepare(request, userHeader, "stream");
            ChatClient.ChatClientRequestSpec promptSpec = buildPrompt(prepared);

            String chatId = newChatId();
            long created = Instant.now().getEpochSecond();

            Flux<String> contentFlux = promptSpec.stream().content()
                    .doOnSubscribe(subscription -> AgenticRequestContext.set(userHeader))
                    .doFinally(signalType -> AgenticRequestContext.clear());

            return contentFlux
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .data(formatChunk(chatId, created, prepared.model(), chunk))
                            .build())
                    .concatWith(Mono.just(ServerSentEvent.<String>builder()
                            .data(formatFinalChunk(chatId, created, prepared.model()))
                            .build()))
                    .concatWith(Mono.just(ServerSentEvent.<String>builder().data("[DONE]").build()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request, RequestHeader.UserHeader userHeader) {
        return Mono.fromCallable(() -> {
            PreparedChatRequest prepared = prepare(request, userHeader, "blocking");
            ChatClient.ChatClientRequestSpec promptSpec = buildPrompt(prepared);

            String content;
            AgenticRequestContext.set(userHeader);
            try {
                content = promptSpec.call().content();
            } finally {
                AgenticRequestContext.clear();
            }

            return ChatCompletionResponse.builder()
                    .id(newChatId())
                    .object("chat.completion")
                    .created(Instant.now().getEpochSecond())
                    .model(prepared.model())
                    .choices(List.of(ChatCompletionResponse.Choice.builder()
                            .index(0)
                            .message(new ChatCompletionResponse.Message("assistant", content))
                            .finishReason("stop")
                            .build()))
                    .usage(new ChatCompletionResponse.Usage(0, 0, 0))
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PreparedChatRequest prepare(ChatCompletionRequest request, RequestHeader.UserHeader userHeader, String mode) {
        validateRequest(request);

        String userMessage = extractLastUserMessage(request);
        String conversationId = resolveConversationId(request);
        String scopedConversationId = AgenticConversationIds.scope(userHeader.getTenantId(), userHeader.getUserId(),
                conversationId);
        SkillDefinition skill = resolveSkill(request.getSkill());
        List<String> toolNames = skill == null ? List.of() : skillRegistry.getEnabledToolNames(skill.getName());
        String skillSystemPrompt = skill == null ? null : buildSkillSystemPrompt(skill);
        String model = StringUtils.defaultIfBlank(request.getModel(), DEFAULT_MODEL);
        Map<String, Object> toolContext = Map.of(
                AgenticConstant.ToolContextKey.TENANT_ID, userHeader.getTenantId(),
                AgenticConstant.ToolContextKey.USER_ID, userHeader.getUserId());

        log.debug(
                "Agentic chat request received, mode={}, model={}, messageCount={}, conversationIdPresent={}, skill={}, tenantId={}, userId={}",
                mode, model, request.getMessages().size(), StringUtils.isNotBlank(request.getConversationId()),
                skill == null ? null : skill.getName(), userHeader.getTenantId(), userHeader.getUserId());

        touchSession(scopedConversationId, conversationId, request.getSkill(), userHeader);

        return new PreparedChatRequest(userMessage, scopedConversationId, skillSystemPrompt,
                normalizeToolNames(toolNames), model, toolContext, request.getTemperature(), request.getMaxTokens());
    }

    private void validateRequest(ChatCompletionRequest request) {
        if (request == null) {
            throw new RequestException("Chat completion request is required");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new RequestException("Chat messages are required");
        }
        if (request.getTemperature() != null && (request.getTemperature() < 0.0 || request.getTemperature() > 2.0)) {
            throw new RequestException("Temperature must be between 0.0 and 2.0");
        }
        if (request.getMaxTokens() != null && request.getMaxTokens() < 1) {
            throw new RequestException("Max tokens must be greater than 0");
        }
    }

    private String extractLastUserMessage(ChatCompletionRequest request) {
        return request.getMessages()
                .stream()
                .filter(message -> message != null && "user".equals(message.getRole()))
                .map(ChatMessageDTO::getContent)
                .filter(StringUtils::isNotBlank)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new RequestException("A non-empty user message is required"));
    }

    private String resolveConversationId(ChatCompletionRequest request) {
        return StringUtils.defaultIfBlank(request.getConversationId(), UUID.randomUUID().toString());
    }

    private SkillDefinition resolveSkill(String skillName) {
        String normalizedSkillName = StringUtils.trimToNull(skillName);
        if (normalizedSkillName == null) {
            return null;
        }
        SkillDefinition skill = skillRegistry.get(normalizedSkillName);
        if (skill == null) {
            log.warn("Agentic skill not found, skill={}", normalizedSkillName);
            throw new RequestException("Agentic skill does not exist: {}", normalizedSkillName);
        }
        log.debug("Agentic skill activated, skill={}, toolNames={}", skill.getName(), skill.getTools());
        return skill;
    }

    private List<String> normalizeToolNames(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }
        return toolNames.stream().filter(StringUtils::isNotBlank).distinct().toList();
    }

    private String buildSkillSystemPrompt(SkillDefinition skill) {
        List<String> sections = new ArrayList<>();
        if (StringUtils.isNotBlank(skill.getSystemPromptAddition())) {
            sections.add(skill.getSystemPromptAddition().trim());
        }
        if (skill.getExamples() != null && !skill.getExamples().isEmpty()) {
            StringBuilder examples = new StringBuilder("Examples:");
            for (SkillDefinition.SkillExample example : skill.getExamples()) {
                if (example == null || StringUtils.isAnyBlank(example.getUser(), example.getAssistant())) {
                    continue;
                }
                examples.append("\n- User: ").append(example.getUser().trim())
                        .append("\n  Assistant: ").append(example.getAssistant().trim());
            }
            sections.add(examples.toString());
        }
        return sections.isEmpty() ? null : String.join("\n\n", sections);
    }

    private ChatClient.ChatClientRequestSpec buildPrompt(PreparedChatRequest prepared) {
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt()
                .user(prepared.userMessage())
                .toolContext(prepared.toolContext())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prepared.scopedConversationId()));

        if (StringUtils.isNotBlank(prepared.skillSystemPrompt())) {
            promptSpec = promptSpec.system(ChatClientConfig.SYSTEM_PROMPT + "\n\n" + prepared.skillSystemPrompt());
        }
        if (!prepared.toolNames().isEmpty()) {
            promptSpec = promptSpec.toolNames(prepared.toolNames().toArray(String[]::new));
        }
        promptSpec = applyRequestOptions(promptSpec, prepared.temperature(), prepared.maxTokens());
        return promptSpec;
    }

    private ChatClient.ChatClientRequestSpec applyRequestOptions(ChatClient.ChatClientRequestSpec promptSpec,
                                                                 Double temperature, Integer maxTokens) {
        if (temperature == null && maxTokens == null) {
            return promptSpec;
        }
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder();
        if (temperature != null) {
            options.temperature(temperature);
        }
        if (maxTokens != null) {
            options.maxTokens(maxTokens);
        }
        return promptSpec.options(options);
    }

    private String formatChunk(String id, long created, String model, String content) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, content))
                        .finishReason(null)
                        .build()))
                .build();
        return toJson(chunk);
    }

    private String formatFinalChunk(String id, long created, String model) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, null))
                        .finishReason("stop")
                        .build()))
                .build();
        return toJson(chunk);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (DatabindException e) {
            log.error("Agentic response serialization failed, responseType={}", obj.getClass().getSimpleName(), e);
            return "{}";
        }
    }

    private String newChatId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private void touchSession(String scopedConversationId, String conversationId, String skill,
                              RequestHeader.UserHeader userHeader) {
        try {
            sessionService.touch(scopedConversationId, skill, userHeader.getTenantId(), userHeader.getUserId());
        } catch (Exception e) {
            log.warn(
                    "Agentic session touch failed, tenantId={}, userId={}, conversationId={}, skill={}",
                    userHeader.getTenantId(), userHeader.getUserId(), conversationId, skill, e);
        }
    }

    private record PreparedChatRequest(String userMessage, String scopedConversationId,
                                       String skillSystemPrompt, List<String> toolNames, String model,
                                       Map<String, Object> toolContext, Double temperature, Integer maxTokens) {
    }

}
