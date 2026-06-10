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
package io.github.pnoker.common.agentic.entity.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAI-compatible chat completion request body.
 *
 * @author pnoker
 * @version 2025.9.0
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI API
 * Reference</a>
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat Completion request body")
public class ChatCompletionRequest {

    /**
     * Model identifier (advisory — the actual model is configured server-side).
     */
    @Schema(description = "Requested model identifier")
    private String model;

    /**
     * Conversation messages in chronological order.
     */
    @Schema(description = "Conversation messages in chronological order")
    private List<ChatMessageDTO> messages;

    /**
     * Sampling temperature override (0.0–2.0). Null uses the server default.
     */
    @Schema(description = "Sampling temperature override")
    private Double temperature;

    /**
     * Maximum tokens to generate. Null uses the server default.
     */
    @Schema(description = "Maximum generated tokens override")
    private Integer maxTokens;

    /**
     * Whether to stream the response as SSE events.
     */
    @Schema(description = "Whether to stream the response as SSE events")
    private Boolean stream;

    /**
     * Conversation ID for chat memory correlation. If omitted, a new conversation is
     * started each request.
     */
    @Schema(description = "Conversation ID for chat memory correlation")
    private String conversationId;

    /**
     * Uploaded attachment database IDs associated with this request.
     */
    @Schema(description = "Uploaded attachment IDs associated with this request")
    private List<Long> attachments;

    /**
     * Whether the client asked for model reasoning. Current OpenAI-compatible text
     * endpoint treats this as advisory metadata.
     */
    @Schema(description = "Whether reasoning output is requested")
    private Boolean reasoning;

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

}
