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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "conversation ID")

    private String conversationId;

    @Schema(description = "role")

    private String role;

    @Schema(description = "content")

    private String content;

    @Schema(description = "content extension information (JSON)")

    private AgenticMessageContent contentExt;

    @Schema(description = "model")

    private String model;

    @Schema(description = "message index")

    private Long messageIndex;

    @Schema(description = "status")

    private AgenticMessageStatusEnum status;

}
