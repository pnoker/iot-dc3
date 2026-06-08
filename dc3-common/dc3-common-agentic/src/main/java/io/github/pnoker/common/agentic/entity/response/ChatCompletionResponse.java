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
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAI-compatible non-streaming chat completion response.
 *
 * @author pnoker
 * @version 2025.9.0
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/object">OpenAI Chat
 * Object</a>
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Chat Completion response body")
public class ChatCompletionResponse {

    @Schema(description = "Primary key")

    private String id;

    @Schema(description = "object")

    private String object;

    @Schema(description = "created")

    private long created;

    @Schema(description = "model")

    private String model;

    @Schema(description = "choices")

    private List<Choice> choices;

    @Schema(description = "usage")

    private Usage usage;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Choice {

        @Schema(description = "index")

        private int index;

        @Schema(description = "message")

        private Message message;

        @Schema(description = "finish reason")

        private String finishReason;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        @Schema(description = "role")

        private String role;

        @Schema(description = "content")

        private String content;

        @Schema(description = "content extension information (JSON)")

        @JsonProperty("content_ext")
        private AgenticMessageContent contentExt;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {

        @Schema(description = "prompt tokens")

        private int promptTokens;

        @Schema(description = "completion tokens")

        private int completionTokens;

        @Schema(description = "total tokens")

        private int totalTokens;

    }

}
