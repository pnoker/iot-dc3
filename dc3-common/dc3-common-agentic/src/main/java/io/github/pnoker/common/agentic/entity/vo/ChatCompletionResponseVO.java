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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * OpenAI-compatible non-streaming chat completion response.
 *
 * @author pnoker
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
@Schema(description = "OpenAI-compatible non-streaming chat completion response returned by the agentic service.")
public class ChatCompletionResponseVO {

    @Schema(
            description = "Unique identifier of this completion response, prefixed with 'chatcmpl-'.",
            example = "chatcmpl-abc123xyz")
    private String id;

    @Schema(
            description = "Object type discriminator; always 'chat.completion' for non-streaming responses.",
            example = "chat.completion")
    private String object;

    @Schema(
            description = "Unix timestamp (seconds since epoch) when the completion was created.",
            example = "1718700000")
    private long created;

    @Schema(description = "Identifier of the model that generated the response.", example = "gpt-4o")
    private String model;

    @Schema(
            description =
                    "List of generated completion choices; contains one entry per requested choice (n parameter).")
    private List<Choice> choices;

    @Schema(description = "Token usage statistics for the request and response.")
    private Usage usage;

    /**
     * One completion choice: index + assistant message.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Choice {

        @Schema(description = "Zero-based index of this choice within the choices array.", example = "0")
        private int index;

        @Schema(description = "Generated assistant message containing role and content.")
        private Message message;

        @Schema(
                description =
                        "Reason the model stopped generating tokens; 'stop' means natural end, 'length' means token limit reached. Serialized as finish_reason per the OpenAI wire contract.",
                example = "stop")
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Generated assistant message (role + content).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Message {

        @Schema(
                description = "Role of the message author; always 'assistant' for generated responses.",
                example = "assistant")
        private String role;

        @Schema(
                description = "Text content of the assistant's reply; null when contentExt carries structured content.",
                example = "The temperature sensor reads 23.5 °C.")
        private String content;

        @Schema(
                description =
                        "Structured content extension carrying multi-part or tool-call message payloads; present when plain text content is insufficient.")
        private AgenticMessageContent contentExt;
    }

    /**
     * Token usage of the completion (prompt/completion/total). All three fields keep
     * their OpenAI snake_case wire names.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Usage {

        @Schema(
                description = "Number of tokens consumed by the input prompt. Serialized as prompt_tokens.",
                example = "42")
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @Schema(
                description = "Number of tokens generated in the completion response. Serialized as completion_tokens.",
                example = "128")
        @JsonProperty("completion_tokens")
        private int completionTokens;

        @Schema(
                description =
                        "Total tokens used by the request; equals prompt_tokens + completion_tokens. Serialized as total_tokens.",
                example = "170")
        @JsonProperty("total_tokens")
        private int totalTokens;

        @Schema(
                description =
                        "Present and true when the numbers are heuristic estimates instead of provider-reported usage. Absent means provider-reported.",
                example = "true")
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private Boolean estimated;

        /**
         * Three-argument constructor for provider-reported usage (estimated absent).
         *
         * @param promptTokens     prompt tokens
         * @param completionTokens completion tokens
         * @param totalTokens      total tokens
         */
        public Usage(int promptTokens, int completionTokens, int totalTokens) {
            this(promptTokens, completionTokens, totalTokens, null);
        }
    }
}
