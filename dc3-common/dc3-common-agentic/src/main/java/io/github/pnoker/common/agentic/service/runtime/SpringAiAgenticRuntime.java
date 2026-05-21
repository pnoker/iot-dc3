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
package io.github.pnoker.common.agentic.service.runtime;

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatRequest;
import io.github.pnoker.common.agentic.service.chat.AgenticPromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Spring AI backed implementation of the agentic runtime.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class SpringAiAgenticRuntime implements AgenticRuntime {

    private final AgenticPromptBuilder promptBuilder;

    private final SpringAiChatResponseMapper responseMapper;

    private final OpenAiCompatibleAgenticRuntime openAiCompatibleAgenticRuntime;

    @Override
    public Flux<AgenticRuntimeStreamFrame> stream(AgenticPreparedChatRequest prepared) {
        if (openAiCompatibleAgenticRuntime.supports(prepared)) {
            return openAiCompatibleAgenticRuntime.stream(prepared);
        }
        return Flux.defer(() -> {
            ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);
            return promptSpec.stream()
                    .chatResponse()
                    .map(response -> new AgenticRuntimeStreamFrame(responseMapper.streamDelta(response),
                            responseMapper.finishReasonOrNull(response)));
        });
    }

    @Override
    public AgenticRuntimeResult call(AgenticPreparedChatRequest prepared) {
        if (openAiCompatibleAgenticRuntime.supports(prepared)) {
            return openAiCompatibleAgenticRuntime.call(prepared);
        }
        ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);
        ChatResponse chatResponse = promptSpec.call().chatResponse();
        return new AgenticRuntimeResult(responseMapper.assistantContent(chatResponse),
                responseMapper.finishReasonOrNull(chatResponse));
    }

}
