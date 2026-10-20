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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.vo.ChatCompletionRequestVO;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * OpenAI-compatible chat completion endpoint.
 * <p>
 * Exposes {@code POST /v1/chat/completions} with both streaming (SSE) and non-streaming
 * (JSON) response modes, following the OpenAI API format.
 * <p>
 * Streaming wire policy applied here: every JSON data frame is tagged with an SSE event
 * name derived from its {@code object} discriminator ({@code chat.completion.chunk},
 * {@code agentic.event}, {@code agentic.visualization}); the {@code [DONE]} sentinel and
 * comment frames stay on the default event. Spec clients dispatch on the event names,
 * while data-line parsers (OpenAI SDKs, the platform frontend) are unaffected. A
 * keep-alive comment heartbeat (see {@code streamHeartbeatSeconds}) is merged into the
 * stream and terminates with it so long tool rounds do not trip idle-connection cutoffs.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Tag(
        name = "chat",
        description =
                "AI chat operations: handle real-time chat interactions including message streaming, tool calling, and conversation context management with language models")
@RestController
@RequestMapping(AgenticConstant.CHAT_URL_PREFIX)
@RequiredArgsConstructor
public class ChatController implements BaseController {

    /** SSE object discriminators that carry a stable event name. */
    private static final Set<String> NAMED_EVENT_OBJECTS = Set.of(
            AgenticConstant.Chat.COMPLETION_CHUNK_OBJECT,
            AgenticConstant.RunEvent.OBJECT,
            AgenticConstant.Visualization.OBJECT);

    private final AgenticChatService agenticChatService;

    private final AgenticProperties properties;

    private final ObjectMapper objectMapper;

    /**
     * Submit a chat prompt and receive the assistant reply in OpenAI-compatible format.
     * <p>
     * Returns SSE when {@code request.stream == true}; otherwise returns the
     * OpenAI-compatible JSON response.
     *
     * @param request chat completion payload carrying the prompt, conversation context, model selection and stream flag
     * @return a streaming SSE entity when stream is true, otherwise the completion resource as JSON
     */
    @PreAuthorize("@perm.can('chat', 'add')")
    @Operation(
            summary = "Create Chat Completion",
            description =
                    "Submit a chat prompt with conversation context and receive the assistant reply using the OpenAI-compatible completion format. "
                            + "When the request sets stream=true the reply is streamed token by token over SSE; otherwise it returns the full reply as a single JSON response.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "true")
                            }))
    @PostMapping("/completions")
    public Mono<ResponseEntity<?>> chatCompletion(@RequestBody ChatCompletionRequestVO request) {
        return getPrincipalHeader().flatMap(header -> {
            if (Objects.nonNull(request) && request.isStream()) {
                return Mono.just(ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(applyStreamWirePolicy(agenticChatService.streamChatCompletion(request, header))));
            }
            return agenticChatService
                    .chatCompletion(request, header)
                    .map(response -> ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response));
        });
    }

    /**
     * Apply the streaming wire policy: name every JSON data frame after its object
     * discriminator, then merge the keep-alive heartbeat. The heartbeat stops when the
     * main stream terminates (complete, error or cancel) because both branches of the
     * merge are cancelled together.
     */
    private Flux<ServerSentEvent<String>> applyStreamWirePolicy(Flux<ServerSentEvent<String>> stream) {
        Flux<ServerSentEvent<String>> named = stream.map(this::withEventName);
        int heartbeatSeconds = properties.getStreamHeartbeatSeconds();
        if (heartbeatSeconds <= 0) {
            return named;
        }
        Sinks.Empty<Void> mainTerminated = Sinks.empty();
        Flux<ServerSentEvent<String>> guarded = named.doFinally(signal -> mainTerminated.tryEmitEmpty());
        Flux<ServerSentEvent<String>> heartbeats = Flux.interval(Duration.ofSeconds(heartbeatSeconds))
                .map(tick ->
                        ServerSentEvent.<String>builder().comment("keep-alive").build())
                .takeUntilOther(mainTerminated.asMono());
        return guarded.mergeWith(heartbeats);
    }

    /**
     * Tag one frame with its event name when it carries a known object discriminator.
     * Frames that already carry an event name, non-JSON payloads (the {@code [DONE]}
     * sentinel) and unparseable data pass through unchanged on the default event.
     */
    private ServerSentEvent<String> withEventName(ServerSentEvent<String> frame) {
        if (Objects.nonNull(frame.event()) || Objects.isNull(frame.data())) {
            return frame;
        }
        String eventName = resolveEventName(frame.data());
        if (Objects.isNull(eventName)) {
            return frame;
        }
        return ServerSentEvent.<String>builder()
                .data(frame.data())
                .event(eventName)
                .build();
    }

    /**
     * Map a JSON frame payload to its SSE event name via the {@code object} field; the
     * event name equals the object discriminator.
     */
    private String resolveEventName(String data) {
        if (AgenticConstant.Chat.STREAM_DONE.equals(data)) {
            return null;
        }
        try {
            String objectName = objectMapper.readTree(data).path("object").asString();
            return NAMED_EVENT_OBJECTS.contains(objectName) ? objectName : null;
        } catch (JacksonException e) {
            return null;
        }
    }
}
