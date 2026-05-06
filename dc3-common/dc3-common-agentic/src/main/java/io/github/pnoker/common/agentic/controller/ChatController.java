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
package io.github.pnoker.common.agentic.controller;

import io.github.pnoker.common.agentic.constant.AgenticConstant;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionChunkResponse;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.agentic.skill.SkillDefinition;
import io.github.pnoker.common.agentic.skill.SkillRegistry;
import io.github.pnoker.common.base.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * OpenAI-compatible chat completion endpoint.
 * <p>
 * Exposes {@code POST /v1/chat/completions} with both streaming (SSE) and
 * non-streaming (JSON) response modes, following the OpenAI API format.
 */
@Slf4j
@RestController
@RequestMapping(AgenticConstant.CHAT_URL_PREFIX)
public class ChatController implements BaseController {

    private final ChatClient chatClient;
    private final SkillRegistry skillRegistry;
    private final SessionService sessionService;

    public ChatController(ChatClient chatClient, SkillRegistry skillRegistry, SessionService sessionService) {
        this.chatClient = chatClient;
        this.skillRegistry = skillRegistry;
        this.sessionService = sessionService;
    }

    /**
     * Streaming chat completion — returns SSE events in OpenAI chunk format.
     * Activated when {@code request.stream == true}.
     */
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChatCompletion(@RequestBody ChatCompletionRequest request) {
        log.debug("Stream chat request: model={}, messages={}, conversationId={}, skill={}",
                request.getModel(), request.getMessages().size(), request.getConversationId(), request.getSkill());

        String userMessage = extractLastUserMessage(request);
        String conversationId = resolveConversationId(request);
        String systemAddition = resolveSkillSystemPrompt(request);

        touchSession(conversationId, request);

        var promptSpec = chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId));
        if (systemAddition != null) {
            promptSpec = promptSpec.system(systemAddition);
        }

        Flux<String> contentFlux = promptSpec.stream().content();

        String chatId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        long created = Instant.now().getEpochSecond();
        String model = request.getModel() != null ? request.getModel() : "dc3-agentic";

        return contentFlux
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(formatChunk(chatId, created, model, chunk))
                        .build())
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .data(formatFinalChunk(chatId, created, model))
                                .build()))
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .data("[DONE]")
                                .build()));
    }

    /**
     * Non-streaming chat completion — returns a single JSON response.
     * Activated when {@code request.stream == false} or omitted.
     */
    @PostMapping(value = "/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatCompletionResponse> chatCompletion(@RequestBody ChatCompletionRequest request) {
        log.debug("Chat request: model={}, messages={}, conversationId={}, skill={}",
                request.getModel(), request.getMessages().size(), request.getConversationId(), request.getSkill());

        String userMessage = extractLastUserMessage(request);
        String conversationId = resolveConversationId(request);
        String systemAddition = resolveSkillSystemPrompt(request);

        touchSession(conversationId, request);

        return Mono.fromCallable(() -> {
            var promptSpec = chatClient.prompt()
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId));
            if (systemAddition != null) {
                promptSpec = promptSpec.system(systemAddition);
            }

            String content = promptSpec.call().content();

            String chatId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            return ChatCompletionResponse.builder()
                    .id(chatId)
                    .object("chat.completion")
                    .created(Instant.now().getEpochSecond())
                    .model(request.getModel() != null ? request.getModel() : "dc3-agentic")
                    .choices(List.of(
                            ChatCompletionResponse.Choice.builder()
                                    .index(0)
                                    .message(new ChatCompletionResponse.Message("assistant", content))
                                    .finishReason("stop")
                                    .build()
                    ))
                    .usage(new ChatCompletionResponse.Usage(0, 0, 0))
                    .build();
        });
    }

    private String extractLastUserMessage(ChatCompletionRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "";
        }
        return request.getMessages().stream()
                .filter(m -> "user".equals(m.getRole()))
                .reduce((first, second) -> second)
                .map(m -> m.getContent())
                .orElse("");
    }

    private String resolveConversationId(ChatCompletionRequest request) {
        return request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();
    }

    /**
     * Resolve the skill-specific system prompt addition for the given request.
     * Returns {@code null} when no skill is specified or the skill has no addition.
     */
    private String resolveSkillSystemPrompt(ChatCompletionRequest request) {
        String skillName = request.getSkill();
        if (skillName == null) {
            return null;
        }
        SkillDefinition skill = skillRegistry.get(skillName);
        if (skill == null) {
            log.warn("Requested skill '{}' not found in registry", skillName);
            return null;
        }
        log.info("Activating skill: {} — {}", skill.getName(), skill.getDescription());
        return skill.getSystemPromptAddition();
    }

    private String formatChunk(String id, long created, String model, String content) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(
                        ChatCompletionChunkResponse.ChunkChoice.builder()
                                .index(0)
                                .delta(new ChatCompletionChunkResponse.Delta(null, content))
                                .finishReason(null)
                                .build()
                ))
                .build();
        return toJson(chunk);
    }

    private String formatFinalChunk(String id, long created, String model) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(
                        ChatCompletionChunkResponse.ChunkChoice.builder()
                                .index(0)
                                .delta(new ChatCompletionChunkResponse.Delta(null, null))
                                .finishReason("stop")
                                .build()
                ))
                .build();
        return toJson(chunk);
    }

    private String toJson(Object obj) {
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
            return "{}";
        }
    }

    private void touchSession(String conversationId, ChatCompletionRequest request) {
        try {
            sessionService.touch(conversationId, request.getSkill());
        } catch (Exception e) {
            log.warn("Failed to touch session for {}: {}", conversationId, e.getMessage());
        }
    }
}
