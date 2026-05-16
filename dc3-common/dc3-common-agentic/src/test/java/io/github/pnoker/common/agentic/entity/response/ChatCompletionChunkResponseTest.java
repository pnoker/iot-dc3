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

package io.github.pnoker.common.agentic.entity.response;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatCompletionChunkResponseTest {

    @Test
    void serializesOpenAiCompatibleDeltaFields() throws Exception {
        ChatCompletionChunkResponse response = ChatCompletionChunkResponse.builder()
                .id("chatcmpl-test")
                .object("chat.completion.chunk")
                .created(1L)
                .model("deepseek-v4-pro")
                .choices(List.of(ChatCompletionChunkResponse.ChunkChoice.builder()
                        .index(0)
                        .delta(new ChatCompletionChunkResponse.Delta(null, "answer"))
                        .finishReason("stop")
                        .build()))
                .build();

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"finish_reason\":\"stop\"");
        assertThat(json).contains("\"content\":\"answer\"");
        assertThat(json).doesNotContain("reasoning_content");
    }

}
