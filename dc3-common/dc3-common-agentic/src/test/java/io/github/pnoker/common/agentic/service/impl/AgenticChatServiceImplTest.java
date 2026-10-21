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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionRequestVO;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionResponseVO;
import io.github.pnoker.common.agentic.service.chat.AgenticChatRequestPreparer;
import io.github.pnoker.common.agentic.service.chat.AgenticChatResponseCodec;
import io.github.pnoker.common.agentic.service.chat.AgenticMessageRecorder;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatBO;
import io.github.pnoker.common.agentic.service.chat.AgenticRunTrace;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntime;
import io.github.pnoker.common.agentic.service.runtime.AgenticTokenUsage;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntimeResult;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AgenticChatServiceImplTest {

    @Mock
    private AgenticChatRequestPreparer requestPreparer;

    @Mock
    private AgenticMessageRecorder messageRecorder;

    @Mock
    private AgenticRuntime agenticRuntime;

    @Mock
    private ChatClientFactory chatClientFactory;

    private AgenticProperties properties;

    private SimpleMeterRegistry meterRegistry;

    private AgenticChatServiceImpl service;

    private RequestHeader.PrincipalHeader userHeader;

    @BeforeEach
    void setUp() {
        properties = new AgenticProperties();
        meterRegistry = new SimpleMeterRegistry();
        service = new AgenticChatServiceImpl(
                requestPreparer,
                new AgenticChatResponseCodec(new ObjectMapper()),
                messageRecorder,
                agenticRuntime,
                properties,
                chatClientFactory,
                meterRegistry);
        userHeader = new RequestHeader.PrincipalHeader();
        userHeader.setTenantId(1L);
        userHeader.setPrincipalId(2L);
        userHeader.setPrincipalName("admin");
    }

    @Test
    void streamChatCompletionPersistsStructuredFailureTrace() {
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        AgenticPreparedChatBO prepared = prepared();
        when(requestPreparer.prepareReactive(request, userHeader, "stream")).thenReturn(Mono.just(prepared));
        when(agenticRuntime.stream(prepared)).thenReturn(Flux.error(new RuntimeException("transport closed")));

        StepVerifier.create(service.streamChatCompletion(request, userHeader).map(ServerSentEvent::data))
                .assertNext(data -> {
                    assertThat(data).contains("\"object\":\"agentic.event\"");
                    assertThat(data).contains("\"type\":\"error\"");
                    assertThat(data).contains("transport closed");
                })
                .assertNext(data -> {
                    assertThat(data).contains("\"object\":\"chat.completion.chunk\"");
                    assertThat(data).contains("\"finish_reason\":\"error\"");
                })
                .expectNext("[DONE]")
                .verifyComplete();

        verify(messageRecorder).persistUserMessage(prepared, userHeader);
        verify(messageRecorder)
                .persistAssistantMessage(prepared, "", "", AgenticMessageStatusEnum.FAILED, AgenticTokenUsage.empty(), userHeader);
        assertThat(prepared.runTrace().recordedEvents()).hasSize(1);
        assertThat(prepared.runTrace().recordedEvents().get(0).status()).isEqualTo("failed");
    }

    @Test
    void streamChatCompletionPersistsCancellationWithoutCompletionFrames() {
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        AgenticPreparedChatBO prepared = prepared();
        when(requestPreparer.prepareReactive(request, userHeader, "stream")).thenReturn(Mono.just(prepared));
        when(agenticRuntime.stream(prepared)).thenReturn(Flux.never());

        StepVerifier.create(service.streamChatCompletion(request, userHeader))
                .thenAwait(Duration.ofMillis(10))
                .thenCancel()
                .verify();

        verify(messageRecorder).persistUserMessage(prepared, userHeader);
        verify(messageRecorder)
                .persistAssistantMessage(
                        prepared, "", "", AgenticMessageStatusEnum.CANCELLED, AgenticTokenUsage.empty(), userHeader);
        verify(messageRecorder, never())
                .persistAssistantMessage(
                        prepared, "", "", AgenticMessageStatusEnum.COMPLETED, AgenticTokenUsage.empty(), userHeader);
        assertThat(prepared.runTrace().pendingEvents())
                .singleElement()
                .satisfies(event -> assertThat(event.status()).isEqualTo("cancelled"));
    }

    @Test
    void perTenantQpsGateRejectsBurstsBeyondTheConfiguredRate() {
        properties.setTransportPerTenantQps(1);
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        AgenticPreparedChatBO prepared = prepared();
        when(requestPreparer.prepareReactive(request, userHeader, "blocking")).thenReturn(Mono.just(prepared));
        when(agenticRuntime.call(prepared)).thenReturn(Mono.fromSupplier(() -> new AgenticRuntimeResult("ok", "stop")));
        // First request passes the gate and proceeds; the immediate second is rejected.
        service.chatCompletion(request, userHeader).block(Duration.ofSeconds(5));

        StepVerifier.create(service.chatCompletion(request, userHeader))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode())
                            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                })
                .verify(Duration.ofSeconds(5));

        assertThat(meterRegistry
                        .get("agentic.chat.requests")
                        .tag("tenant", "1")
                        .tag("result", "rejected")
                        .counter()
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void qpsGateIsDisabledAtZero() {
        properties.setTransportPerTenantQps(0);
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        AgenticPreparedChatBO prepared = prepared();
        when(requestPreparer.prepareReactive(request, userHeader, "blocking")).thenReturn(Mono.just(prepared));
        when(agenticRuntime.call(prepared)).thenReturn(Mono.fromSupplier(() -> new AgenticRuntimeResult("ok", "stop")));

        service.chatCompletion(request, userHeader).block(Duration.ofSeconds(5));
        service.chatCompletion(request, userHeader).block(Duration.ofSeconds(5));
        assertThat(meterRegistry
                        .get("agentic.chat.requests")
                        .tag("tenant", "1")
                        .tag("result", "completed")
                        .counter()
                        .count())
                .isEqualTo(2.0);
    }

    @Test
    void blockingChatCompletionPropagatesProviderUsageToWireAndPersistence() {
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        AgenticPreparedChatBO prepared = prepared();
        AgenticTokenUsage usage = new AgenticTokenUsage(11L, 22L, 33L);
        when(requestPreparer.prepareReactive(request, userHeader, "blocking")).thenReturn(Mono.just(prepared));
        when(agenticRuntime.call(prepared))
                .thenReturn(Mono.fromSupplier(() -> new AgenticRuntimeResult("done", "stop", usage)));
        when(messageRecorder.persistAssistantMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());

        ChatCompletionResponseVO response = service.chatCompletion(request, userHeader).block(Duration.ofSeconds(5));

        assertThat(response).isNotNull();
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(11);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(22);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(33);
        assertThat(response.getUsage().getEstimated()).isFalse();
        verify(messageRecorder).persistAssistantMessage(
                eq(prepared), eq("done"), isNull(), eq(AgenticMessageStatusEnum.COMPLETED), eq(usage), eq(userHeader));
        assertThat(meterRegistry
                        .get("agentic.chat.tokens")
                        .tag("tenant", "1")
                        .tag("model", "dc3-test-model")
                        .counter()
                        .count())
                .isEqualTo(33.0);
    }

    private AgenticPreparedChatBO prepared() {
        return new AgenticPreparedChatBO(
                "hello",
                "conversation",
                null,
                "dc3-test-model",
                Map.of(),
                null,
                null,
                new AgenticRunTrace(),
                true,
                false,
                List.of(),
                List.of(),
                AgenticMessageContent.Tokens.of(1, 0, 1, 0, 0, 0),
                List.of());
    }
}
