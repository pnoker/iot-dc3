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
package io.github.pnoker.common.agentic.service;

import io.github.pnoker.common.agentic.entity.request.ChatCompletionRequest;
import io.github.pnoker.common.agentic.entity.response.ChatCompletionResponse;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agentic chat orchestration service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface AgenticChatService {

    /**
     * Execute a streaming chat completion request.
     *
     * @param request    OpenAI-compatible request body
     * @param userHeader authenticated user header
     * @return OpenAI-compatible SSE stream
     */
    Flux<ServerSentEvent<String>> streamChatCompletion(ChatCompletionRequest request,
                                                       RequestHeader.UserHeader userHeader);

    /**
     * Execute a non-streaming chat completion request.
     *
     * @param request    OpenAI-compatible request body
     * @param userHeader authenticated user header
     * @return OpenAI-compatible JSON response
     */
    Mono<ChatCompletionResponse> chatCompletion(ChatCompletionRequest request,
                                                RequestHeader.UserHeader userHeader);

}
