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

import com.fasterxml.jackson.annotation.JsonAlias;
import io.github.pnoker.common.agentic.entity.dto.ChatMessageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OpenAI-compatible chat completion request body.
 *
 * @author pnoker
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI API
 * Reference</a>
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description =
                "OpenAI-compatible chat completion request body; all fields except messages are optional overrides of server-side defaults.")
public class ChatCompletionRequestVO {

    /**
     * Model identifier (advisory — the actual model is configured server-side).
     */
    @Schema(
            description =
                    "Advisory model identifier; the actual model used is determined by server-side configuration.",
            example = "gpt-4o")
    private String model;

    /**
     * Conversation messages in chronological order. System messages are merged into the
     * platform system prompt, the last user message drives this turn, and every earlier
     * user/assistant turn is replayed as conversation context.
     */
    @Schema(
            description =
                    "Ordered list of conversation messages (system, user, assistant turns) comprising the prompt context. "
                            + "System messages are merged into the system prompt; the last user message becomes this turn's prompt; "
                            + "earlier user and assistant turns are replayed as conversation context.")
    private List<ChatMessageDTO> messages;

    /**
     * Sampling temperature override (0.0–2.0). Null uses the server default.
     */
    @Schema(
            description =
                    "Sampling temperature in the range [0.0, 2.0]; higher values produce more random output. Null defers to the server default.",
            example = "0.7")
    private Double temperature;

    /**
     * Maximum tokens to generate. Accepts the OpenAI wire names {@code max_tokens} and
     * {@code max_completion_tokens} in addition to the platform camelCase name; null uses
     * the server default.
     */
    @Schema(
            description =
                    "Maximum number of tokens to generate in the completion. Accepts max_tokens, max_completion_tokens, "
                            + "or maxTokens as the JSON field name; the value is applied to the model request. Null defers to the server default.",
            example = "2048")
    @JsonAlias({"max_tokens", "max_completion_tokens"})
    private Integer maxTokens;

    /**
     * Whether to stream the response as SSE events.
     */
    @Schema(
            description =
                    "When true, the response is streamed as Server-Sent Events (SSE); when false or omitted, a single JSON response is returned.",
            example = "false")
    private Boolean stream;

    /**
     * Conversation ID for chat memory correlation. Optional: when omitted the server
     * generates a fresh UUIDv7 conversation id, matching the OpenAI stateless request
     * style — each such request starts a new conversation. Pass a stable id to keep
     * server-side memory replay across turns.
     */
    @Schema(
            description =
                    "Opaque conversation identifier used to correlate multi-turn memory. Optional: when omitted the server "
                            + "generates a fresh UUIDv7 id and each request starts a new conversation (OpenAI stateless style); "
                            + "pass a stable id to replay server-side memory across turns.",
            example = "conv-7f3a1b2c")
    private String conversationId;

    /**
     * Uploaded attachment database IDs associated with this request.
     */
    @Schema(
            description =
                    "List of attachment record IDs (database primary keys) previously uploaded and associated with this completion request.")
    private List<Long> attachments;

    /**
     * Whether the client asked for model reasoning. Current OpenAI-compatible text
     * endpoint treats this as advisory metadata.
     */
    @Schema(
            description =
                    "When true, requests that the model include its reasoning trace in the response; treated as advisory metadata by the current endpoint.",
            example = "false")
    private Boolean reasoning;

    /**
     * Determine whether the client requested a streaming response.
     *
     * @return {@code true} only when the nullable stream flag is explicitly enabled
     */
    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }
}
