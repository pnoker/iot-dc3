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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single SSE chunk in the OpenAI streaming chat completion response.
 *
 * @author pnoker
 * @version 2025.9.0
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/streaming">OpenAI
 * Streaming</a>
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Chat Completion Chunk response body")
public class ChatCompletionChunkResponse {

    @Schema(description = "Primary key")

    private String id;

    @Schema(description = "object")

    private String object;

    @Schema(description = "created")

    private long created;

    @Schema(description = "model")

    private String model;

    @Schema(description = "choices")

    private List<ChunkChoice> choices;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChunkChoice {

        @Schema(description = "index")

        private int index;

        @Schema(description = "delta")

        private Delta delta;

        @Schema(description = "finish reason")

        @JsonProperty("finish_reason")
        private String finishReason;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Delta {

        @Schema(description = "role")

        private String role;

        @Schema(description = "content")

        private String content;

        @Schema(description = "reasoning content")

        @JsonProperty("reasoning_content")
        private String reasoningContent;

    }

}
