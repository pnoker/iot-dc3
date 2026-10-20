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
package io.github.pnoker.common.agentic.entity.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Guards the OpenAI wire names of the non-streaming completion response: the protocol
 * fields must serialize snake_case and must never leak their camelCase Java names.
 */
class ChatCompletionResponseVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesOpenAiCompatibleChoiceAndUsageFields() {
        ChatCompletionResponseVO response = ChatCompletionResponseVO.builder()
                .id("chatcmpl-test")
                .object("chat.completion")
                .created(1L)
                .model("deepseek-v4-pro")
                .choices(List.of(ChatCompletionResponseVO.Choice.builder()
                        .index(0)
                        .message(new ChatCompletionResponseVO.Message("assistant", "answer", null))
                        .finishReason("stop")
                        .build()))
                .usage(new ChatCompletionResponseVO.Usage(42, 128, 170))
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"finish_reason\":\"stop\"");
        assertThat(json).contains("\"prompt_tokens\":42");
        assertThat(json).contains("\"completion_tokens\":128");
        assertThat(json).contains("\"total_tokens\":170");
        assertThat(json).doesNotContain("\"finishReason\"");
        assertThat(json).doesNotContain("\"promptTokens\"");
        assertThat(json).doesNotContain("\"completionTokens\"");
        assertThat(json).doesNotContain("\"totalTokens\"");
    }

    @Test
    void roundTripsThroughTheSnakeCaseWireNames() {
        String json = "{\"id\":\"chatcmpl-test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"m\","
                + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"hi\"},\"finish_reason\":\"length\"}],"
                + "\"usage\":{\"prompt_tokens\":7,\"completion_tokens\":9,\"total_tokens\":16}}";

        ChatCompletionResponseVO response = objectMapper.readValue(json, ChatCompletionResponseVO.class);

        assertThat(response.getChoices()).singleElement().satisfies(choice -> {
            assertThat(choice.getFinishReason()).isEqualTo("length");
            assertThat(choice.getMessage().getContent()).isEqualTo("hi");
        });
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(7);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(9);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(16);
    }
}
