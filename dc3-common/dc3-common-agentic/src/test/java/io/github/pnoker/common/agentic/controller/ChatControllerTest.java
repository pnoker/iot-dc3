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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionRequestVO;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionResponseVO;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies the streaming wire policy of the chat endpoint: named SSE events derived
 * from the payload object discriminator, an unnamed [DONE] sentinel, and keep-alive
 * comment heartbeats that tick while the stream is open and stop with it.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private AgenticChatService agenticChatService;

    private AgenticProperties properties;

    private ChatController controller;

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        properties = new AgenticProperties();
        controller = new ChatController(agenticChatService, properties, new ObjectMapper());
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void namesChunkAndEventFramesByTheirObjectDiscriminator() {
        properties.setStreamHeartbeatSeconds(0);
        Flux<ServerSentEvent<String>> stream = Flux.just(
                frame("{\"object\":\"chat.completion.chunk\",\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}"),
                frame("{\"object\":\"agentic.event\",\"type\":\"tool\",\"title\":\"Tool call\"}"),
                frame("{\"object\":\"agentic.visualization\",\"visualization\":{}}"),
                frame("[DONE]"));
        ChatCompletionRequestVO request = streamingRequest();

        when(agenticChatService.streamChatCompletion(request, header)).thenReturn(stream);

        StepVerifier.create(streamBody(request))
                .assertNext(frame -> assertThat(frame.event()).isEqualTo("chat.completion.chunk"))
                .assertNext(frame -> assertThat(frame.event()).isEqualTo("agentic.event"))
                .assertNext(frame -> assertThat(frame.event()).isEqualTo("agentic.visualization"))
                .assertNext(frame -> {
                    assertThat(frame.event()).isNull();
                    assertThat(frame.data()).isEqualTo("[DONE]");
                })
                .verifyComplete();
    }

    @Test
    void mergesKeepAliveCommentsThatTickWhileTheStreamIsOpen() {
        properties.setStreamHeartbeatSeconds(1);
        Flux<ServerSentEvent<String>> stream =
                Flux.just(frame("{\"object\":\"chat.completion.chunk\"}")).concatWith(Flux.never());
        ChatCompletionRequestVO request = streamingRequest();

        when(agenticChatService.streamChatCompletion(request, header)).thenReturn(stream);

        StepVerifier.withVirtualTime(() -> streamBody(request))
                .assertNext(frame -> assertThat(frame.event()).isEqualTo("chat.completion.chunk"))
                .thenAwait(Duration.ofSeconds(2))
                .assertNext(frame -> {
                    assertThat(frame.comment()).isEqualTo("keep-alive");
                    assertThat(frame.data()).isNull();
                })
                .assertNext(frame -> assertThat(frame.comment()).isEqualTo("keep-alive"))
                .thenCancel()
                .verify();
    }

    @Test
    void blockingBranchReturnsJsonResponseUntouched() {
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        request.setStream(false);
        ChatCompletionResponseVO response =
                ChatCompletionResponseVO.builder().id("chatcmpl-1").build();
        when(agenticChatService.chatCompletion(request, header)).thenReturn(Mono.just(response));

        StepVerifier.create(controller
                        .chatCompletion(request)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .assertNext(entity -> {
                    assertThat(entity.getStatusCode().value()).isEqualTo(200);
                    assertThat(entity.getBody()).isSameAs(response);
                })
                .verifyComplete();
    }

    private ChatCompletionRequestVO streamingRequest() {
        ChatCompletionRequestVO request = new ChatCompletionRequestVO();
        request.setStream(true);
        return request;
    }

    @SuppressWarnings("unchecked")
    private Flux<ServerSentEvent<String>> streamBody(ChatCompletionRequestVO request) {
        return controller
                .chatCompletion(request)
                .flatMapMany(entity -> (Flux<ServerSentEvent<String>>) entity.getBody())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication()));
    }

    private ServerSentEvent<String> frame(String data) {
        return ServerSentEvent.<String>builder().data(data).build();
    }

    private GatewayAuthenticationToken authentication() {
        return new GatewayAuthenticationToken(header, List.of());
    }
}
