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

import static io.github.pnoker.common.utils.LogSanitizer.sanitize;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
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
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default agentic chat orchestration service.
 *
 * <p>Each chat entry passes a per-tenant rate gate
 * ({@code dc3.agentic.transport-per-tenant-qps}, zero disables it) and the
 * whole lifecycle is instrumented: one request counter and one duration timer
 * tagged with tenant, provider, model and outcome, plus tenant/user-scoped
 * lifecycle logs with elapsed time.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticChatServiceImpl implements AgenticChatService {

    private static final int QPS_BUCKET_CACHE_MAXIMUM_SIZE = 8192;
    private static final Duration QPS_BUCKET_IDLE = Duration.ofMinutes(10);

    private final AgenticChatRequestPreparer requestPreparer;

    private final AgenticChatResponseCodec responseCodec;

    private final AgenticMessageRecorder messageRecorder;

    private final AgenticRuntime agenticRuntime;

    private final AgenticProperties properties;

    private final ChatClientFactory chatClientFactory;

    private final MeterRegistry meterRegistry;

    private final Cache<Long, TenantTokenBucket> tenantQpsBuckets = Caffeine.newBuilder()
            .maximumSize(QPS_BUCKET_CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(QPS_BUCKET_IDLE)
            .build();

    @Override
    public Flux<ServerSentEvent<String>> streamChatCompletion(
            ChatCompletionRequestVO request, RequestHeader.PrincipalHeader userHeader) {
        return Flux.defer(() -> {
            long startNanos = System.nanoTime();
            return acquireTenantQuota(userHeader, request.getModel())
                    .thenMany(requestPreparer
                            .prepareReactive(request, userHeader, "stream")
                            .doOnError(error -> recordLifecycle(
                                    userHeader,
                                    request.getModel(),
                                    providerTag(request.getModel(), userHeader),
                                    "failed",
                                    startNanos))
                            .flatMapMany(prepared -> persistUserMessage(prepared, userHeader)
                                    .thenMany(streamPrepared(prepared, userHeader, startNanos))));
        });
    }

    private Flux<ServerSentEvent<String>> streamPrepared(
            AgenticPreparedChatBO prepared, RequestHeader.PrincipalHeader userHeader, long startNanos) {
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
                .concatMap(frame ->
                        Flux.fromIterable(responseCodec.streamEvents(prepared, chatId, created, frame.delta())));

        Flux<ServerSentEvent<String>> responseEvents = runtimeEvents.onErrorResume(error -> {
            log.warn(
                    "Agentic stream chat failed, tenantId={}, userId={}, conversationId={}, model={}",
                    userHeader.getTenantId(),
                    userHeader.getUserId(),
                    sanitize(prepared.conversationId()),
                    sanitize(prepared.model()),
                    error);
            lastFinishReason.set(AgenticConstant.Chat.FINISH_REASON_ERROR);
            prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
            return Flux.fromIterable(responseCodec.streamEvents(prepared, chatId, created, AgenticStreamDelta.empty()));
        });

        Flux<ServerSentEvent<String>> response =
                Flux.fromIterable(responseCodec.initialEvents(prepared)).concatWith(responseEvents);
        Flux<ServerSentEvent<String>> persistedResponse = Flux.usingWhen(
                Mono.just(prepared),
                ignored -> response,
                ignored -> persistStreamTermination(
                        prepared,
                        assistantContent,
                        assistantReasoningContent,
                        streamStatus(lastFinishReason.get()),
                        lastFinishReason.get(),
                        userHeader,
                        startNanos),
                (ignored, error) -> {
                    prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
                    return persistStreamTermination(
                            prepared,
                            assistantContent,
                            assistantReasoningContent,
                            AgenticMessageStatusEnum.FAILED,
                            AgenticConstant.Chat.FINISH_REASON_ERROR,
                            userHeader,
                            startNanos);
                },
                ignored -> {
                    prepared.runTrace().recordPendingEvent(AgenticRunEvent.requestCancelled());
                    return persistStreamTermination(
                            prepared,
                            assistantContent,
                            assistantReasoningContent,
                            AgenticMessageStatusEnum.CANCELLED,
                            AgenticConstant.Chat.FINISH_REASON_CANCELLED,
                            userHeader,
                            startNanos);
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
    public Mono<ChatCompletionResponseVO> chatCompletion(
            ChatCompletionRequestVO request, RequestHeader.PrincipalHeader userHeader) {
        return Mono.defer(() -> {
            long startNanos = System.nanoTime();
            return acquireTenantQuota(userHeader, request.getModel())
                    .then(requestPreparer
                            .prepareReactive(request, userHeader, "blocking")
                            .doOnError(error -> recordLifecycle(
                                    userHeader,
                                    request.getModel(),
                                    providerTag(request.getModel(), userHeader),
                                    "failed",
                                    startNanos)))
                    .flatMap(prepared -> persistUserMessage(prepared, userHeader)
                            .then(agenticRuntime.call(prepared))
                            .flatMap(result ->
                                    persistAssistantAndBuildResponse(prepared, result, userHeader, startNanos))
                            .onErrorResume(error -> {
                                prepared.runTrace()
                                        .recordPendingEvent(AgenticRunEvent.requestFailed(error.getMessage()));
                                recordLifecycle(
                                        userHeader,
                                        prepared.model(),
                                        providerTag(prepared.model(), userHeader),
                                        "failed",
                                        startNanos);
                                return persistAssistantMessage(
                                                prepared, "", null, AgenticMessageStatusEnum.FAILED, userHeader)
                                        .then(Mono.error(error));
                            }));
        });
    }

    /**
     * Per-tenant rate gate: a token bucket refilled at the configured QPS,
     * with a one-second burst capacity. Zero disables the gate entirely.
     */
    private Mono<Void> acquireTenantQuota(RequestHeader.PrincipalHeader userHeader, String requestedModel) {
        int qps = properties.getTransportPerTenantQps();
        if (qps <= 0) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
            TenantTokenBucket bucket =
                    tenantQpsBuckets.get(userHeader.getTenantId(), key -> new TenantTokenBucket(qps));
            if (!bucket.tryAcquire()) {
                recordLifecycle(userHeader, requestedModel, "none", "rejected", System.nanoTime());
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "Per-tenant chat request rate exceeded; retry shortly");
            }
        });
    }

    private Mono<ChatCompletionResponseVO> persistAssistantAndBuildResponse(
            AgenticPreparedChatBO prepared,
            AgenticRuntimeResult result,
            RequestHeader.PrincipalHeader userHeader,
            long startNanos) {
        String assistantText = StringUtils.defaultString(result.content());
        return persistAssistantMessage(prepared, assistantText, userHeader).then(Mono.fromSupplier(() -> {
            recordLifecycle(
                    userHeader, prepared.model(), providerTag(prepared.model(), userHeader), "completed", startNanos);
            log.info(
                    "Agentic chat complete, tenantId={}, userId={}, conversationId={}, model={}, contentLen={}, finishReason={}, durationMs={}",
                    userHeader.getTenantId(),
                    userHeader.getUserId(),
                    sanitize(prepared.conversationId()),
                    sanitize(prepared.model()),
                    assistantText.length(),
                    result.finishReason(),
                    Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            return responseCodec.blockingResponse(prepared, assistantText, result.finishReason());
        }));
    }

    private Mono<Void> persistUserMessage(AgenticPreparedChatBO prepared, RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence = messageRecorder.persistUserMessage(prepared, userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistAssistantMessage(
            AgenticPreparedChatBO prepared, String content, RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence = messageRecorder.persistAssistantMessage(prepared, content, userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistAssistantMessage(
            AgenticPreparedChatBO prepared,
            String content,
            String reasoningContent,
            RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence =
                messageRecorder.persistAssistantMessage(prepared, content, reasoningContent, userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistAssistantMessage(
            AgenticPreparedChatBO prepared,
            String content,
            String reasoningContent,
            AgenticMessageStatusEnum status,
            RequestHeader.PrincipalHeader userHeader) {
        Mono<Void> persistence =
                messageRecorder.persistAssistantMessage(prepared, content, reasoningContent, status, userHeader);
        return persistence == null ? Mono.empty() : persistence;
    }

    private Mono<Void> persistStreamTermination(
            AgenticPreparedChatBO prepared,
            StringBuilder content,
            StringBuilder reasoningContent,
            AgenticMessageStatusEnum status,
            String finishReason,
            RequestHeader.PrincipalHeader userHeader,
            long startNanos) {
        recordLifecycle(
                userHeader, prepared.model(), providerTag(prepared.model(), userHeader), statusTag(status), startNanos);
        return persistAssistantMessage(prepared, content.toString(), reasoningContent.toString(), status, userHeader)
                .then(Mono.fromRunnable(() -> log.info(
                        "Agentic stream terminated, tenantId={}, userId={}, conversationId={}, model={}, contentLen={}, status={}, finishReason={}, durationMs={}",
                        userHeader.getTenantId(),
                        userHeader.getUserId(),
                        sanitize(prepared.conversationId()),
                        sanitize(prepared.model()),
                        content.length(),
                        status,
                        finishReason,
                        Duration.ofNanos(System.nanoTime() - startNanos).toMillis())));
    }

    private AgenticMessageStatusEnum streamStatus(String finishReason) {
        return AgenticConstant.Chat.FINISH_REASON_ERROR.equals(finishReason)
                ? AgenticMessageStatusEnum.FAILED
                : AgenticMessageStatusEnum.COMPLETED;
    }

    /** Best-effort provider tag for metrics; the provider cache may have expired for long streams. */
    private String providerTag(String model, RequestHeader.PrincipalHeader userHeader) {
        ModelProviderBO provider = chatClientFactory.resolveProviderForModel(model, userHeader.getTenantId());
        return provider == null || provider.getId() == null ? "none" : String.valueOf(provider.getId());
    }

    private void recordLifecycle(
            RequestHeader.PrincipalHeader userHeader, String model, String provider, String result, long startNanos) {
        String tenant = String.valueOf(userHeader.getTenantId());
        String modelTag = StringUtils.defaultIfBlank(model, "unspecified");
        meterRegistry
                .counter(
                        "agentic.chat.requests",
                        "tenant",
                        tenant,
                        "provider",
                        provider,
                        "model",
                        modelTag,
                        "result",
                        result)
                .increment();
        meterRegistry
                .timer(
                        "agentic.chat.duration",
                        "tenant",
                        tenant,
                        "provider",
                        provider,
                        "model",
                        modelTag,
                        "result",
                        result)
                .record(Duration.ofNanos(System.nanoTime() - startNanos));
    }

    private static String statusTag(AgenticMessageStatusEnum status) {
        return status == AgenticMessageStatusEnum.COMPLETED
                ? "completed"
                : status == AgenticMessageStatusEnum.CANCELLED ? "cancelled" : "failed";
    }

    /** Minimal synchronized token bucket: capacity of one burst second, continuous lazy refill. */
    private static final class TenantTokenBucket {

        private final double capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillNanos = System.nanoTime();

        private TenantTokenBucket(int permitsPerSecond) {
            this.capacity = Math.max(1, permitsPerSecond);
            this.refillPerSecond = permitsPerSecond;
            this.tokens = capacity;
        }

        private synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
