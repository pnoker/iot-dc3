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

import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.service.chat.AgenticPreparedChatBO;
import io.github.pnoker.common.agentic.service.chat.AgenticPromptBuilder;
import io.github.pnoker.common.constant.service.AgenticConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default agentic runtime: routes to the explicit OpenAI-compatible agent loop when
 * tool calling is enabled, otherwise drives the Spring AI ChatClient directly. Both
 * paths run under a per-provider in-flight limit acquired from the client factory —
 * throttling at the source beats retrying into the provider's rate limit.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
@RequiredArgsConstructor
public class SpringAiAgenticRuntime implements AgenticRuntime {

    private final AgenticPromptBuilder promptBuilder;

    private final SpringAiChatResponseMapper responseMapper;

    private final OpenAiCompatibleAgenticRuntime openAiCompatibleAgenticRuntime;

    private final ChatClientFactory chatClientFactory;

    @Override
    public Flux<AgenticRuntimeStreamFrame> stream(AgenticPreparedChatBO prepared) {
        boolean openAiCompatible = openAiCompatibleAgenticRuntime.supports(prepared);
        return Flux.usingWhen(
                chatClientFactory.acquireInFlightSlot(prepared.model(), tenantId(prepared)),
                lease -> openAiCompatible ? openAiCompatibleAgenticRuntime.stream(prepared) : springAiStream(prepared),
                lease -> Mono.fromRunnable(() -> chatClientFactory.releaseInFlightSlot(lease)));
    }

    @Override
    public Mono<AgenticRuntimeResult> call(AgenticPreparedChatBO prepared) {
        boolean openAiCompatible = openAiCompatibleAgenticRuntime.supports(prepared);
        return Mono.usingWhen(
                chatClientFactory.acquireInFlightSlot(prepared.model(), tenantId(prepared)),
                lease -> openAiCompatible ? openAiCompatibleAgenticRuntime.call(prepared) : springAiCall(prepared),
                lease -> Mono.fromRunnable(() -> chatClientFactory.releaseInFlightSlot(lease)));
    }

    private Flux<AgenticRuntimeStreamFrame> springAiStream(AgenticPreparedChatBO prepared) {
        return Flux.defer(() -> {
            ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);
            return promptSpec.stream()
                    .chatResponse()
                    .map(response -> new AgenticRuntimeStreamFrame(
                            responseMapper.streamDelta(response), responseMapper.finishReasonOrNull(response)));
        });
    }

    private Mono<AgenticRuntimeResult> springAiCall(AgenticPreparedChatBO prepared) {
        return Mono.defer(() -> {
            ChatClient.ChatClientRequestSpec promptSpec = promptBuilder.build(prepared);
            return promptSpec.stream().chatResponse().collectList().map(responses -> {
                ChatResponse chatResponse = responses.isEmpty() ? null : responses.get(responses.size() - 1);
                if (chatResponse == null) {
                    return new AgenticRuntimeResult("", null);
                }
                return new AgenticRuntimeResult(
                        responseMapper.assistantContent(chatResponse), responseMapper.finishReasonOrNull(chatResponse));
            });
        });
    }

    private Long tenantId(AgenticPreparedChatBO prepared) {
        Object value = prepared.toolContext().get(AgenticConstant.ToolContextKey.TENANT_ID);
        return value instanceof Number number ? number.longValue() : null;
    }
}
