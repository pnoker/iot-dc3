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

import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for agentic message API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Message view object")
public class MessageVO extends BaseVO {

    @Schema(description = "ID of the conversation session this message belongs to.", example = "conv_xyz789")
    private String conversationId;

    @Schema(description = "Role of the message sender: user (human input), assistant (AI response), or system (instruction).", example = "assistant")
    private String role;

    @Schema(description = "Plain-text message body representing the conversation turn.")
    private String content;

    @Schema(description = "Structured content extension including tool calls, citations, or rich media fragments.")
    private AgenticMessageContent contentExt;

    @Schema(description = "Identifier of the AI model that generated this message (e.g. gpt-4o, claude-3.5-sonnet).", example = "gpt-4o")
    private String model;

    @Schema(description = "Zero-based ordinal position of this message within the conversation history.", example = "0")
    private Long messageIndex;

    @Schema(description = "Persistence status: OK (stored successfully) or ERROR (storage failed).", example = "OK")
    private AgenticMessageStatusEnum status;

}
