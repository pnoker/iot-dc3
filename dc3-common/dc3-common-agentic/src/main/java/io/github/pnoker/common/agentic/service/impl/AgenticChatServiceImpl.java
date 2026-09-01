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

import io.github.pnoker.common.agentic.entity.model.AgenticRunEvent;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionRequestVO;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionResponseVO;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.agentic.service.chat.AgenticChatRequestPreparer;
import io.github.pnoker.common.agentic.service.chat.AgenticChatResponseCodec;
import io.github.pnoker.common.agentic.service.chat.AgenticMessageRecorder;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatBO;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntime;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntimeResult;
import io.github.pnoker.common.agentic.service.runtime.AgenticStreamDelta;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.pnoker.common.utils.LogSanitizer.sanitize;

/**
 * Default agentic chat orchestration service.
 *
 * @author pnoker
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
    public Flux<ServerSentEvent<String>> streamChatCompletion(ChatCompletionRequestVO request,
                                                              RequestHeader.PrincipalHeader userHeader) {
        return Flux.defer(() -> {
            return requestPreparer.prepareReactive(request, userHeader, "stream")
                    .flatMapMany(prepared -> persistUserMessage(prepared, userHeader)
                    .thenMany(streamPrepared(prepared, userHeader)));
        });
    }

    private Flux<ServerSentEvent<String>> streamPrepared(AgenticPreparedChatBO prepared,
                                                          RequestHeader.PrincipalHeader userHeader) {
        String chatId = responseCodec.newChatId();
        long created = Instant.now().getEpochSecond();
        StringBuilder assistantContent = new StringBuilder();
        StringBuilder assistantReasoningContent = new StringBuilder();
        AtomicReference<String> lastFinishReason = new AtomicReference<>(AgenticConstant.Chat.FINISH_REASON_STOP);

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
                        frame.delta())));

        Flux<ServerSentEvent<String>> responseEvents = runtimeEvents
                .onErrorResume(error -> {
                    log.warn("Agentic stream chat failed, conversationId={}, model={}",
                            sanitize(prepared.conversationId()), sanitize(prepared.model()), error);
                    lastFinishReason.set(AgenticConstant.Chat.FINISH_REASON_ERROR);
                    prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
                    return Flux.fromIterable(responseCodec.streamEvents(prepared, chatId, created,
                            AgenticStreamDelta.empty()));
                });

        Flux<ServerSentEvent<String>> response = Flux.fromIterable(responseCodec.initialEvents(prepared))
                .concatWith(responseEvents);
        Flux<ServerSentEvent<String>> persistedResponse = Flux.usingWhen(Mono.just(prepared), ignored -> response,
                ignored -> persistStreamTermination(prepared, assistantContent, assistantReasoningContent,
                        streamStatus(lastFinishReason.get()), lastFinishReason.get(), userHeader),
                (ignored, error) -> {
                    prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
                    return persistStreamTermination(prepared, assistantContent, assistantReasoningContent,
                            AgenticMessageStatusEnum.FAILED, AgenticConstant.Chat.FINISH_REASON_ERROR, userHeader);
                },
                ignored -> {
                    prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestCancelled());
                    return persistStreamTermination(prepared, assistantContent, assistantReasoningContent,
                            AgenticMessageStatusEnum.CANCELLED, AgenticConstant.Chat.FINISH_REASON_CANCELLED,
                            userHeader);
                });
        return persistedResponse
                .concatWith(Mono.fromSupplier(() -> ServerSentEvent.<String>builder()
                        .data(responseCodec.formatFinalChunk(chatId, created, prepared.model(), lastFinishReason.get()))
                        .build()))
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .data(AgenticConstant.Chat.STREAM_DONE)
                        .build()));
    }

    @Override
    public Mono<ChatCompletionResponseVO> chatCompletion(ChatCompletionRequestVO request,
                                                          RequestHeader.PrincipalHeader userHeader) {
        return Mono.defer(() -> {
            return requestPreparer.prepareReactive(request, userHeader, "blocking")
                    .flatMap(prepared -> persistUserMessage(prepared, userHeader)
                    .then(agenticRuntime.call(prepared))
                    .flatMap(result -> persistAssistantAndBuildResponse(prepared, result, userHeader))
                    .onErrorResume(error -> {
                        prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
                        return persistAssistantMessage(prepared, "", null, AgenticMessageStatusEnum.FAILED, userHeader)
                                .then(Mono.error(error));
                    }));
        });
    }

    private Mono<ChatCompletionResponseVO> persistAssistantAndBuildResponse(AgenticPreparedChatBO prepared,
                                                                               AgenticRuntimeResult result,
                                                                               RequestHeader.PrincipalHeader userHeader) {
        String assistantText = StringUtils.defaultString(result.content());
        return persistAssistantMessage(prepared, assistantText, userHeader)
                .then(Mono.fromSupplier(() -> {
                    log.info("Agentic chat complete, conversationId={}, model={}, contentLen={}, finishReason={}",
                            sanitize(prepared.conversationId()), sanitize(prepared.model()), assistantText.length(),
                            result.finishReason());
                    return responseCodec.blockingResponse(prepared, assistantText, result.finishReason());
                }));
    }

    private Mono<Void> persistUserMessage(AgenticPreparedChatBO prepared,
                                           RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence = messageRecorder.persistUserMessage(prepared, userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistAssistantMessage(AgenticPreparedChatBO prepared, String content,
                                                RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence = messageRecorder.persistAssistantMessage(prepared, content, userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistAssistantMessage(AgenticPreparedChatBO prepared, String content,
                                                String reasoningContent,
                                                RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence = messageRecorder.persistAssistantMessage(prepared, content, reasoningContent,
                userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistAssistantMessage(AgenticPreparedChatBO prepared, String content,
                                                String reasoningContent, AgenticMessageStatusEnum status,
                                                RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence = messageRecorder.persistAssistantMessage(prepared, content, reasoningContent, status,
                userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistStreamTermination(AgenticPreparedChatBO prepared, StringBuilder content,
                                                 StringBuilder reasoningContent,
                                                 AgenticMessageStatusEnum status, String finishReason,
                                                 RequestHeader.PrincipalHeader userHeader) {
        return persistAssistantMessage(prepared, content.toString(), reasoningContent.toString(), status, userHeader)
                .then(Mono.fromRunnable(() -> log.info(
                        "Agentic stream terminated, conversationId={}, model={}, contentLen={}, status={}, finishReason={}",
                        sanitize(prepared.conversationId()), sanitize(prepared.model()), content.length(), status,
                        finishReason)));
    }

    private AgenticMessageStatusEnum streamStatus(String finishReason) {
        return AgenticConstant.Chat.FINISH_REASON_ERROR.equals(finishReason)
                ? AgenticMessageStatusEnum.FAILED
                : AgenticMessageStatusEnum.COMPLETED;
    }

}
