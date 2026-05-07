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

/**
 * OpenAI-compatible chat completion request body.
 *
 * @author pnoker
 * @version 2025.9.0
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI API
 * Reference</a>
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    /**
     * Model identifier (advisory — the actual model is configured server-side).
     */
    private String model;

    /**
     * Conversation messages in chronological order.
     */
    private List<ChatMessageDTO> messages;

    /**
     * Sampling temperature override (0.0–2.0). Null uses the server default.
     */
    private Double temperature;

    /**
     * Maximum tokens to generate. Null uses the server default.
     */
    private Integer maxTokens;

    /**
     * Whether to stream the response as SSE events.
     */
    private Boolean stream;

    /**
     * Conversation ID for chat memory correlation. If omitted, a new conversation is
     * started each request.
     */
    private String conversationId;

    /**
     * Skill name to activate for this request. Null = all tools available.
     */
    private String skill;

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

}
