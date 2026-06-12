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

import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.service.chat.AgenticChatRequestPreparer;
import io.github.pnoker.common.agentic.service.chat.AgenticChatResponseCodec;
import io.github.pnoker.common.agentic.service.chat.AgenticMessageRecorder;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatRequest;
import io.github.pnoker.common.agentic.service.chat.AgenticRunTrace;
import io.github.pnoker.common.agentic.service.runtime.AgenticRuntime;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenticChatServiceImplTest {

    @Mock
    private AgenticChatRequestPreparer requestPreparer;

    @Mock
    private AgenticMessageRecorder messageRecorder;

    @Mock
    private AgenticRuntime agenticRuntime;

    private AgenticChatServiceImpl service;

    private RequestHeader.PrincipalHeader userHeader;

    @BeforeEach
    void setUp() {
        service = new AgenticChatServiceImpl(requestPreparer, new AgenticChatResponseCodec(new ObjectMapper()),
                messageRecorder, agenticRuntime);
        userHeader = new RequestHeader.PrincipalHeader();
        userHeader.setTenantId(1L);
        userHeader.setPrincipalId(2L);
        userHeader.setPrincipalName("admin");
    }

    @Test
    void streamChatCompletionPersistsStructuredFailureTrace() {
        ChatCompletionRequest request = new ChatCompletionRequest();
        AgenticPreparedChatRequest prepared = prepared();
        when(requestPreparer.prepare(request, userHeader, "stream")).thenReturn(prepared);
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
        verify(messageRecorder).persistAssistantMessage(prepared, "", "", userHeader);
        assertThat(prepared.runTrace().recordedEvents()).hasSize(1);
        assertThat(prepared.runTrace().recordedEvents().get(0).status()).isEqualTo("failed");
    }

    private AgenticPreparedChatRequest prepared() {
        return new AgenticPreparedChatRequest("hello", "tenant:user:conversation", null, "dc3-test-model",
                Map.of(), null, null, new AgenticRunTrace(), true, false, List.of(), List.of(),
                AgenticMessageContent.Tokens.of(1, 0, 1, 0, 0, 0), List.of());
    }

}
