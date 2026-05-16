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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.service.chat.AgenticChatResponseCodec;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatRequest;
import io.github.pnoker.common.agentic.service.chat.AgenticPromptBuilder;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Spring AI backed implementation of the agentic runtime.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Component
public class SpringAiAgenticRuntime implements AgenticRuntime {

    private final AgenticPromptBuilder promptBuilder;

    private final AgenticChatResponseCodec responseCodec;

    public SpringAiAgenticRuntime(AgenticPromptBuilder promptBuilder, AgenticChatResponseCodec responseCodec) {
        this.promptBuilder = promptBuilder;
        this.responseCodec = responseCodec;
    }

    @Override
    public Flux<AgenticRuntimeStreamFrame> stream(AgenticPreparedChatRequest prepared,
                                                  RequestHeader.UserHeader userHeader) {
        return Flux.defer(() -> {
            ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);
            return promptSpec.stream()
                    .chatResponse()
                    .doOnSubscribe(subscription -> AgenticRequestContext.set(userHeader))
                    .map(response -> new AgenticRuntimeStreamFrame(responseCodec.extractStreamDelta(response),
                            responseCodec.finishReasonOrNull(response)))
                    .doFinally(signalType -> AgenticRequestContext.clear());
        });
    }

    @Override
    public AgenticRuntimeResult call(AgenticPreparedChatRequest prepared, RequestHeader.UserHeader userHeader) {
        ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);
        try {
            AgenticRequestContext.set(userHeader);
            ChatResponse chatResponse = promptSpec.call().chatResponse();
            return new AgenticRuntimeResult(responseCodec.assistantContent(chatResponse),
                    responseCodec.finishReason(chatResponse));
        } finally {
            AgenticRequestContext.clear();
        }
    }

}
