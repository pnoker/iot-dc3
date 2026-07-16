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
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.agentic.entity.vo.AgenticRunEventVO;
import io.github.pnoker.common.agentic.entity.vo.AgenticVisualizationVO;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionChunkVO;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionResponseVO;
import io.github.pnoker.common.agentic.service.runtime.AgenticStreamDelta;
import io.github.pnoker.common.agentic.utils.AgenticTokenEstimatorUtil;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Encodes agentic chat responses and server-sent events.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgenticChatResponseCodec {

    private final ObjectMapper objectMapper;

    /**
     * Assemble a non-streaming chat completion response, estimating token usage from the
     * prepared input tokens and the assistant content.
     *
     * @param prepared     the prepared chat
     * @param content      the assistant content
     * @param finishReason the finish reason
     * @return the chat completion response
     */
    public ChatCompletionResponseVO blockingResponse(AgenticPreparedChatBO prepared, String content,
                                                     String finishReason) {
        int completionTokens = AgenticTokenEstimatorUtil.estimate(content);
        int promptTokens = Objects.nonNull(prepared.inputTokens()) ? prepared.inputTokens().getInput() : 0;
        return ChatCompletionResponseVO.builder()
                .id(newChatId())
                .object(AgenticConstant.Chat.COMPLETION_OBJECT)
                .created(Instant.now().getEpochSecond())
                .model(prepared.model())
                .choices(List.of(ChatCompletionResponseVO.Choice.builder()
                        .index(0)
                        .message(new ChatCompletionResponseVO.Message(AgenticConstant.Chat.ROLE_ASSISTANT, content,
                                buildResponseContentExt(prepared)))
                        .finishReason(normalizeFinishReason(finishReason))
                        .build()))
                .usage(new ChatCompletionResponseVO.Usage(promptTokens, completionTokens,
                        promptTokens + completionTokens))
                .build();
    }

    public String newChatId() {
        return AgenticConstant.Chat.ID_PREFIX
                + UUID.randomUUID().toString().replace(SymbolConstant.HYPHEN, StringUtils.EMPTY).substring(0, 24);
    }

    public String formatFinalChunk(String id, long created, String model, String finishReason) {
        ChatCompletionChunkVO chunk = ChatCompletionChunkVO.builder()
                .id(id)
                .object(AgenticConstant.Chat.COMPLETION_CHUNK_OBJECT)
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkVO.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkVO.Delta(null, null, null))
                        .finishReason(normalizeFinishReason(finishReason))
                        .build()))
                .build();
        return toJson(chunk);
    }

    public List<ServerSentEvent<String>> initialEvents(AgenticPreparedChatBO prepared) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        if (prepared.reasoning()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent(AgenticRunEvent.reasoningRequested()))
                    .build());
        }
        return events;
    }

    /**
     * Build the SSE events for one streaming step: drain pending run-trace events and
     * visualizations, then append a content chunk when the delta carries one.
     *
     * @param prepared    the prepared chat
     * @param chatId      the stream chat id
     * @param created     the stream creation epoch second
     * @param streamDelta the delta for this step, may be null or content-less
     * @return the SSE events to emit for this step
     */
    public List<ServerSentEvent<String>> streamEvents(AgenticPreparedChatBO prepared, String chatId, long created,
                                                      AgenticStreamDelta streamDelta) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        for (AgenticRunEvent event : prepared.runTrace().drainPendingEvents()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatEvent(event))
                    .build());
        }
        for (AgenticVisualizationSpec visualization : prepared.runTrace().drainPendingVisualizations()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatVisualization(visualization))
                    .build());
        }
        if (Objects.nonNull(streamDelta) && streamDelta.hasContent()) {
            events.add(ServerSentEvent.<String>builder()
                    .data(formatChunk(chatId, created, prepared.model(), streamDelta))
                    .build());
        }
        return events;
    }

    public String formatEvent(AgenticRunEvent runEvent) {
        return toJson(AgenticRunEventVO.of(runEvent));
    }

    public String formatVisualization(AgenticVisualizationSpec visualization) {
        return toJson(AgenticVisualizationVO.of(visualization, Instant.now().getEpochSecond()));
    }

    private AgenticMessageContent buildResponseContentExt(AgenticPreparedChatBO prepared) {
        List<AgenticVisualizationSpec> visualizations = prepared.runTrace().drainAndRecordedVisualizations();
        if (visualizations.isEmpty()) {
            return null;
        }
        AgenticMessageContent content = new AgenticMessageContent();
        content.setCharts(visualizations);
        return content;
    }

    private String formatChunk(String id, long created, String model, AgenticStreamDelta streamDelta) {
        ChatCompletionChunkVO chunk = ChatCompletionChunkVO.builder()
                .id(id)
                .object(AgenticConstant.Chat.COMPLETION_CHUNK_OBJECT)
                .created(created)
                .model(model)
                .choices(List.of(ChatCompletionChunkVO.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkVO.Delta(null, StringUtils.trimToNull(streamDelta.content()),
                                streamDelta.reasoningContent()))
                        .finishReason(null)
                        .build()))
                .build();
        return toJson(chunk);
    }

    private String normalizeFinishReason(String reason) {
        if (StringUtils.isBlank(reason)) {
            return AgenticConstant.Chat.FINISH_REASON_STOP;
        }
        return reason.toLowerCase(Locale.ROOT);
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
