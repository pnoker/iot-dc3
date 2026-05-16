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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.agentic.service.chat.AgenticChatRequestPreparer;
import io.github.pnoker.common.agentic.service.chat.AgenticChatResponseCodec;
import io.github.pnoker.common.agentic.service.chat.AgenticMessageRecorder;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatRequest;
import io.github.pnoker.common.agentic.service.chat.AgenticPromptBuilder;
import io.github.pnoker.common.agentic.service.chat.AgenticStreamDelta;
import io.github.pnoker.common.entity.common.RequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    private final AgenticChatRequestPreparer requestPreparer;

    private final AgenticPromptBuilder promptBuilder;

    private final AgenticChatResponseCodec responseCodec;

    private final AgenticMessageRecorder messageRecorder;

    public AgenticChatServiceImpl(AgenticChatRequestPreparer requestPreparer, AgenticPromptBuilder promptBuilder,
                                  AgenticChatResponseCodec responseCodec, AgenticMessageRecorder messageRecorder) {
        this.requestPreparer = requestPreparer;
        this.promptBuilder = promptBuilder;
        this.responseCodec = responseCodec;
        this.messageRecorder = messageRecorder;
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChatCompletion(ChatCompletionRequest request,
                                                              RequestHeader.UserHeader userHeader) {
        return Flux.defer(() -> {
            AgenticPreparedChatRequest prepared = requestPreparer.prepare(request, userHeader, "stream");
            messageRecorder.persistUserMessage(prepared, userHeader);
            if (StringUtils.isNotBlank(prepared.directAnswer())) {
                return streamDirectAnswer(prepared, userHeader);
            }

            ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);

            String chatId = responseCodec.newChatId();
            long created = Instant.now().getEpochSecond();
            StringBuilder assistantContent = new StringBuilder();
            AtomicReference<String> lastFinishReason = new AtomicReference<>();

            Flux<AgenticStreamDelta> contentFlux = promptSpec.stream().chatResponse()
                    .doOnSubscribe(subscription -> AgenticRequestContext.set(userHeader))
                    .doOnNext(response -> responseCodec.rememberFinishReason(response, lastFinishReason))
                    .map(responseCodec::extractStreamDelta)
                    .filter(AgenticStreamDelta::hasContent)
                    .doOnNext(delta -> assistantContent.append(delta.content()))
                    .doOnComplete(() -> {
                        messageRecorder.persistAssistantMessage(prepared, assistantContent.toString(), userHeader);
                        log.info(
                                "Agentic stream complete, conversationId={}, model={}, contentLen={}, finishReason={}",
                                prepared.scopedConversationId(), prepared.model(), assistantContent.length(),
                                lastFinishReason.get());
                    })
                    .doFinally(signalType -> AgenticRequestContext.clear());

            Flux<ServerSentEvent<String>> initialEvents = Flux.fromIterable(responseCodec.initialEvents(prepared));
            Flux<ServerSentEvent<String>> responseEvents = contentFlux
                    .flatMap(chunk -> Flux.fromIterable(responseCodec.chunkEvents(prepared, chatId, created, chunk)))
                    .onErrorResume(error -> {
                        log.warn("Agentic stream chat failed, conversationId={}, model={}",
                                prepared.scopedConversationId(), prepared.model(), error);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data(responseCodec.formatEvent("error", "Request failed", error.getMessage(),
                                        "agentic"))
                                .build());
                    });

            return initialEvents
                    .concatWith(responseEvents)
                    .concatWith(Mono.defer(() -> Mono.just(ServerSentEvent.<String>builder()
                            .data(responseCodec.formatFinalChunk(chatId, created, prepared.model(),
                                    responseCodec.normalizeFinishReason(lastFinishReason.get())))
                            .build())))
                    .concatWith(Mono.just(ServerSentEvent.<String>builder().data("[DONE]").build()));
        }).doFinally(signalType -> AgenticRequestContext.clear()).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<ServerSentEvent<String>> streamDirectAnswer(AgenticPreparedChatRequest prepared,
                                                             RequestHeader.UserHeader userHeader) {
        String chatId = responseCodec.newChatId();
        long created = Instant.now().getEpochSecond();
        String content = prepared.directAnswer();
        List<ServerSentEvent<String>> events = new ArrayList<>();
        events.addAll(responseCodec.initialEvents(prepared));
        events.addAll(responseCodec.chunkEvents(prepared, chatId, created, new AgenticStreamDelta(content, null)));
        events.add(ServerSentEvent.<String>builder()
                .data(responseCodec.formatFinalChunk(chatId, created, prepared.model(), "stop"))
                .build());
        events.add(ServerSentEvent.<String>builder().data("[DONE]").build());
        messageRecorder.persistAssistantMessage(prepared, content, userHeader);
        log.info("Agentic direct answer complete, conversationId={}, model={}, contentLen={}",
                prepared.scopedConversationId(), prepared.model(), content.length());
        return Flux.fromIterable(events);
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request, RequestHeader.UserHeader userHeader) {
        return Mono.fromCallable(() -> {
            try {
                AgenticPreparedChatRequest prepared = requestPreparer.prepare(request, userHeader, "blocking");
                messageRecorder.persistUserMessage(prepared, userHeader);
                if (StringUtils.isNotBlank(prepared.directAnswer())) {
                    String content = prepared.directAnswer();
                    messageRecorder.persistAssistantMessage(prepared, content, userHeader);
                    return responseCodec.blockingResponse(prepared, content, "stop");
                }

                ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);

                AgenticRequestContext.set(userHeader);
                ChatResponse chatResponse = promptSpec.call().chatResponse();
                String content = responseCodec.assistantContent(chatResponse);
                String finishReason = responseCodec.finishReason(chatResponse);
                messageRecorder.persistAssistantMessage(prepared, content, userHeader);
                log.info("Agentic blocking complete, conversationId={}, model={}, contentLen={}, finishReason={}",
                        prepared.scopedConversationId(), prepared.model(), content.length(), finishReason);

                return responseCodec.blockingResponse(prepared, content, finishReason);
            } finally {
                AgenticRequestContext.clear();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

}
