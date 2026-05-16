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

import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletionChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiChatResponseMapperTest {

    private final SpringAiChatResponseMapper mapper = new SpringAiChatResponseMapper();

    @Test
    void streamDeltaExtractsOpenAiCompatibleReasoningContentFromChunkChoice() {
        ChatCompletionChunk.Choice chunkChoice = ChatCompletionChunk.Choice.builder()
                .index(0)
                .finishReason(Optional.empty())
                .delta(ChatCompletionChunk.Choice.Delta.builder()
                        .putAdditionalProperty("reasoning_content", JsonValue.from("Checking real platform data."))
                        .build())
                .build();
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .properties(Map.of("chunkChoice", chunkChoice))
                .build();

        AgenticStreamDelta delta = mapper.streamDelta(new ChatResponse(List.of(new Generation(assistantMessage))));

        assertThat(delta.content()).isEmpty();
        assertThat(delta.reasoningContent()).isEqualTo("Checking real platform data.");
        assertThat(delta.hasContent()).isTrue();
    }

}
