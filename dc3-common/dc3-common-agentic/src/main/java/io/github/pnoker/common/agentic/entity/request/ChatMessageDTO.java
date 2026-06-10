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

import io.github.pnoker.common.constant.service.AgenticConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single message in the OpenAI-compatible chat completion request.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "OpenAI-compatible chat message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    /**
     * Message role: {@link AgenticConstant.Chat#ROLE_SYSTEM},
     * {@link AgenticConstant.Chat#ROLE_USER},
     * {@link AgenticConstant.Chat#ROLE_ASSISTANT}, or
     * {@link AgenticConstant.Chat#ROLE_TOOL}.
     */
    @Schema(description = "Message role: system, user, assistant, or tool", example = "user")
    private String role;

    /**
     * Message content text.
     */
    @Schema(description = "Message content text", example = "Show me the latest device status.")
    private String content;

}
