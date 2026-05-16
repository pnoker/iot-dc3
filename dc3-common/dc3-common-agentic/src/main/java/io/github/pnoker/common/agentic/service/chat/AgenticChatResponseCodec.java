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

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionChunk;
import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionChunkResponse;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.agentic.skill.SkillDefinition;
import io.github.pnoker.common.agentic.util.AgenticTokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Encodes agentic chat responses and server-sent events.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class AgenticChatResponseCodec {

    private final ObjectMapper objectMapper;

    public AgenticChatResponseCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChatCompletionResponse blockingResponse(AgenticPreparedChatRequest prepared, String content,
                                                   String finishReason) {
        int completionTokens = AgenticTokenEstimator.estimate(content);
        int promptTokens = Objects.nonNull(prepared.inputTokens()) ? prepared.inputTokens().getInput() : 0;
        return ChatCompletionResponse.builder()
                .id(newChatId())
                .object("chat.completion")
                .created(Instant.now().getEpochSecond())
                .model(prepared.model())
                .choices(List.of(ChatCompletionResponse.Choice.builder()
                        .index(0)
                        .message(new ChatCompletionResponse.Message("assistant", content))
                        .finishReason(finishReason)
                        .build()))
                .usage(new ChatCompletionResponse.Usage(promptTokens, completionTokens,
                        promptTokens + completionTokens))
                .build();
    }

    public String assistantContent(ChatResponse chatResponse) {
        return Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())
                && Objects.nonNull(chatResponse.getResult().getOutput())
                        ? StringUtils.defaultString(chatResponse.getResult().getOutput().getText())
                        : "";
    }

    public String finishReason(ChatResponse chatResponse) {
        String finishReason = Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())
                && Objects.nonNull(chatResponse.getResult().getMetadata())
                        ? chatResponse.getResult().getMetadata().getFinishReason()
                        : null;
        return normalizeFinishReason(finishReason);
    }

    public String newChatId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    public String formatFinalChunk(String id, long created, String model, String finishReason) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, null, null))
                        .finishReason(finishReason)
                        .build()))
                .build();
        return toJson(chunk);
    }

    public void rememberFinishReason(ChatResponse response, AtomicReference<String> sink) {
        if (Objects.isNull(response) || Objects.isNull(response.getResult())
                || Objects.isNull(response.getResult().getMetadata())) {
            return;
        }
        String reason = response.getResult().getMetadata().getFinishReason();
        if (StringUtils.isNotBlank(reason)) {
            sink.set(reason);
        }
    }

    public String normalizeFinishReason(String reason) {
        if (StringUtils.isBlank(reason)) {
            return "stop";
        }
        return reason.toLowerCase(Locale.ROOT);
    }

    public List<ServerSentEvent<String>> initialEvents(AgenticPreparedChatRequest prepared) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        SkillDefinition skillDefinition = prepared.skillDefinition();
        String skillName = Objects.nonNull(skillDefinition) ? skillDefinition.getName() : "general";
        String skillDescription = Objects.nonNull(skillDefinition) ? skillDefinition.getDescription()
                : "General assistant mode";
        events.add(ServerSentEvent.<String>builder()
                .data(formatEvent("skill", "Auto skill", skillDescription, skillName))
                .build());
        if (StringUtils.isBlank(prepared.directAnswer()) && prepared.toolCallingEnabled()
                && !prepared.toolNames().isEmpty()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("tools", "Available tools", String.join(", ", prepared.toolNames()), skillName))
                    .build());
        }
        if (prepared.directContextProvided()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("tool", "Backend context loaded", "Queried DC3 backend before response",
                            skillName))
                    .build());
        }
        if (prepared.reasoning()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("reasoning", "Thinking", "Reasoning mode requested for this model.", skillName))
                    .build());
        }
        return events;
    }

    public List<ServerSentEvent<String>> chunkEvents(AgenticPreparedChatRequest prepared, String chatId, long created,
                                                     AgenticStreamDelta streamDelta) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        AgenticRequestContext.ToolEvent event = prepared.toolEvents().poll();
        while (Objects.nonNull(event)) {
            prepared.toolTraceEvents().add(event);
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent("tool", event.description(), event.domain(), event.toolName()))
                    .build());
            event = prepared.toolEvents().poll();
        }
        events.add(ServerSentEvent.<String>builder()
                .data(formatChunk(chatId, created, prepared.model(), streamDelta))
                .build());
        return events;
    }

    public AgenticStreamDelta extractStreamDelta(ChatResponse response) {
        if (Objects.isNull(response) || Objects.isNull(response.getResult())) {
            return AgenticStreamDelta.empty();
        }
        Generation generation = response.getResult();
        String content = Objects.nonNull(generation.getOutput()) ? generation.getOutput().getText() : null;
        if (log.isDebugEnabled()) {
            log.debug("Agentic stream chunk, contentLen={}, hasReasoning={}",
                    Objects.isNull(content) ? 0 : content.length(),
                    Objects.nonNull(extractReasoningContent(generation)));
        }
        return new AgenticStreamDelta(StringUtils.defaultString(content), extractReasoningContent(generation));
    }

    public String formatEvent(String type, String title, String detail, String name) {
        Map<String, Object> event = new HashMap<>();
        event.put("object", "agentic.event");
        event.put("type", type);
        event.put("title", StringUtils.defaultString(title));
        event.put("detail", StringUtils.defaultString(detail));
        event.put("name", StringUtils.defaultString(name));
        event.put("created", Instant.now().getEpochSecond());
        return toJson(event);
    }

    private String formatChunk(String id, long created, String model, AgenticStreamDelta streamDelta) {
        ChatCompletionChunkResponse chunk = ChatCompletionChunkResponse.builder()
                .id(id)
                .object("chat.completion.chunk")
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, streamDelta.content(),
                                streamDelta.reasoningContent()))
                        .finishReason(null)
                        .build()))
                .build();
        return toJson(chunk);
    }

    private String extractReasoningContent(Generation generation) {
        if (Objects.isNull(generation) || Objects.isNull(generation.getOutput())) {
            return null;
        }
        Object chunkChoice = generation.getOutput().getMetadata().get("chunkChoice");
        if (Objects.isNull(chunkChoice)) {
            return null;
        }

        if (chunkChoice instanceof ChatCompletionChunk.Choice openAiChunkChoice) {
            Object rawValue = openAiChunkChoice.delta()._additionalProperties().get("reasoning_content");
            if (!(rawValue instanceof JsonValue value)) {
                return null;
            }
            Optional<String> reasoningContent = value.asString();
            return reasoningContent.orElse(null);
        }
        return null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (DatabindException e) {
            log.error("Agentic response serialization failed, responseType={}", obj.getClass().getSimpleName(), e);
            return "{}";
        }
    }

}
