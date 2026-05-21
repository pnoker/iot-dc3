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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.agentic.service.chat.AgenticChatRequestPreparer;
import io.github.pnoker.common.agentic.service.chat.AgenticChatResponseCodec;
import io.github.pnoker.common.agentic.service.chat.AgenticMessageRecorder;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatRequest;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntime;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntimeResult;
import io.github.pnoker.common.agentic.service.runtime.AgenticStreamDelta;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default agentic chat orchestration service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticChatServiceImpl implements AgenticChatService {

    private final AgenticChatRequestPreparer requestPreparer;

    private final AgenticChatResponseCodec responseCodec;

    private final AgenticMessageRecorder messageRecorder;

    private final AgenticRuntime agenticRuntime;

    @Override
    public Flux<ServerSentEvent<String>> streamChatCompletion(ChatCompletionRequest request,
                                                              RequestHeader.UserHeader userHeader) {
        return Flux.defer(() -> {
            AgenticPreparedChatRequest prepared = requestPreparer.prepare(request, userHeader, "stream");
            messageRecorder.persistUserMessage(prepared, userHeader);

            String chatId = responseCodec.newChatId();
            long created = Instant.now().getEpochSecond();
            StringBuilder assistantContent = new StringBuilder();
            StringBuilder assistantReasoningContent = new StringBuilder();
            AtomicReference<String> lastFinishReason = new AtomicReference<>();

            Flux<ServerSentEvent<String>> runtimeEvents = agenticRuntime.stream(prepared)
                    .doOnNext(frame -> {
                        if (frame.hasFinishReason()) {
                            lastFinishReason.set(frame.finishReason());
                        }
                        if (frame.delta().content() != null) {
                            assistantContent.append(frame.delta().content());
                        }
                        if (frame.delta().reasoningContent() != null) {
                            assistantReasoningContent.append(frame.delta().reasoningContent());
                        }
                    })
                    .concatMap(frame -> Flux.fromIterable(responseCodec.streamEvents(prepared, chatId, created,
                            frame.delta())))
                    .doOnComplete(() -> {
                        messageRecorder.persistAssistantMessage(prepared, assistantContent.toString(),
                                assistantReasoningContent.toString(), userHeader);
                        log.info(
                                "Agentic stream complete, conversationId={}, model={}, contentLen={}, finishReason={}",
                                prepared.scopedConversationId(), prepared.model(), assistantContent.length(),
                                lastFinishReason.get());
                    });

            Flux<ServerSentEvent<String>> initialEvents = Flux.fromIterable(responseCodec.initialEvents(prepared));
            Flux<ServerSentEvent<String>> responseEvents = runtimeEvents.onErrorResume(error -> {
                log.warn("Agentic stream chat failed, conversationId={}, model={}",
                        prepared.scopedConversationId(), prepared.model(), error);
                lastFinishReason.set(AgenticConstant.Chat.FINISH_REASON_ERROR);
                prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
                return Flux.fromIterable(responseCodec.streamEvents(prepared, chatId, created, AgenticStreamDelta.empty()))
                        .doOnComplete(() -> messageRecorder.persistAssistantMessage(prepared,
                                assistantContent.toString(), assistantReasoningContent.toString(), userHeader));
            });

            return initialEvents
                    .concatWith(responseEvents)
                    .concatWith(Mono.defer(() -> Mono.just(ServerSentEvent.<String>builder()
                            .data(responseCodec.formatFinalChunk(chatId, created, prepared.model(),
                                    lastFinishReason.get()))
                            .build())))
                    .concatWith(Mono.just(ServerSentEvent.<String>builder()
                            .data(AgenticConstant.Chat.STREAM_DONE)
                            .build()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request, RequestHeader.UserHeader userHeader) {
        return Mono.fromCallable(() -> {
            AgenticPreparedChatRequest prepared = requestPreparer.prepare(request, userHeader, "blocking");
            messageRecorder.persistUserMessage(prepared, userHeader);

            AgenticRuntimeResult result;
            try {
                result = agenticRuntime.call(prepared);
            } catch (RuntimeException e) {
                prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(e.getMessage()));
                messageRecorder.persistAssistantMessage(prepared, "", userHeader);
                throw e;
            }
            String assistantText = StringUtils.defaultString(result.content());
            messageRecorder.persistAssistantMessage(prepared, assistantText, userHeader);
            log.info("Agentic blocking complete, conversationId={}, model={}, contentLen={}, finishReason={}",
                    prepared.scopedConversationId(), prepared.model(), assistantText.length(), result.finishReason());

            return responseCodec.blockingResponse(prepared, assistantText, result.finishReason());
        }).subscribeOn(Schedulers.boundedElastic());
    }

}
