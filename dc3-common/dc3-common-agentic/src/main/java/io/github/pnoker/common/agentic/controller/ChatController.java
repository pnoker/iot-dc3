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

import io.github.pnoker.common.agentic.entity.vo.ChatCompletionVO;
import io.github.pnoker.common.agentic.service.AgenticChatService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * OpenAI-compatible chat completion endpoint.
 * <p>
 * Exposes {@code POST /v1/chat/completions} with both streaming (SSE) and non-streaming
 * (JSON) response modes, following the OpenAI API format.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "chat", description = "AI chat operations: handle real-time chat interactions including message streaming, tool calling, and conversation context management with language models")
@RestController
@RequestMapping(AgenticConstant.CHAT_URL_PREFIX)
@RequiredArgsConstructor
public class ChatController implements BaseController {

    private final AgenticChatService agenticChatService;

    /**
     * Chat completion endpoint. Returns SSE when {@code request.stream == true};
     * otherwise returns the OpenAI-compatible JSON response.
     */
    @PreAuthorize("@perm.can('chat', 'list')")
    @Operation(summary = "Create Chat Completion", description = "Submit a chat prompt with conversation context and receive the assistant reply using the OpenAI-compatible completion format. "
            + "When the request sets stream=true the reply is streamed token by token over SSE; otherwise it returns the full reply as a single JSON response.")
    @PostMapping("/completions")
    public Mono<ResponseEntity<?>> chatCompletion(@RequestBody ChatCompletionVO request) {
        return getPrincipalHeader().flatMap(header -> {
            if (Objects.nonNull(request) && request.isStream()) {
                return Mono.just(ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(agenticChatService.streamChatCompletion(request, header)));
            }
            return agenticChatService.chatCompletion(request, header)
                    .map(response -> ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(R.ok(response)));
        });
    }

}
