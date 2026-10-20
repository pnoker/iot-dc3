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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Guards the request-side OpenAI wire acceptance: max tokens binds under every accepted
 * JSON field name, and a request without conversationId deserializes cleanly (the
 * server generates a fresh id — see the preparer).
 */
class ChatCompletionRequestVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bindsMaxTokensUnderAllAcceptedWireNames() {
        assertThat(objectMapper
                        .readValue("{\"messages\":[],\"max_tokens\":2048}", ChatCompletionRequestVO.class)
                        .getMaxTokens())
                .isEqualTo(2048);
        assertThat(objectMapper
                        .readValue("{\"messages\":[],\"max_completion_tokens\":1024}", ChatCompletionRequestVO.class)
                        .getMaxTokens())
                .isEqualTo(1024);
        assertThat(objectMapper
                        .readValue("{\"messages\":[],\"maxTokens\":512}", ChatCompletionRequestVO.class)
                        .getMaxTokens())
                .isEqualTo(512);
    }

    @Test
    void acceptsRequestWithoutConversationId() {
        ChatCompletionRequestVO request = objectMapper.readValue(
                "{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}", ChatCompletionRequestVO.class);

        assertThat(request.getConversationId()).isNull();
        assertThat(request.getMessages()).singleElement().satisfies(message -> {
            assertThat(message.getRole()).isEqualTo("user");
            assertThat(message.getContent()).isEqualTo("hi");
        });
    }
}
