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

import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * View object for agentic action API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Response view of an agentic action execution record, including its lifecycle status, payload, and expiry.")
public class ActionVO extends BaseVO {

    @Schema(description = "Unique string identifier for the agent action execution instance.", example = "act_abc123")
    private String actionId;

    @Schema(description = "Identifier of the conversation session this action belongs to.", example = "conv_xyz789")
    private String conversationId;

    @Schema(description = "Classification of the action kind, e.g. the tool or function category that was invoked.", example = "TOOL_CALL")
    private String actionType;

    @Schema(description = "Human-readable title summarizing the action intent, shown in UI and audit logs.", example = "Query device temperature")
    private String title;

    @Schema(description = "Detailed description of what the action performs and its expected outcome.", example = "Reads the latest temperature reading from device 1024 via the sensor query tool.")
    private String description;

    @Schema(description = "Execution payload including tool name, input parameters, and metadata passed to the execution handler; structure varies by actionType.")
    private Map<String, Object> payload;

    @Schema(description = "Current lifecycle status of the action: PENDING (awaiting confirmation), CONFIRMED (claimed for execution), REJECTED (denied by user), EXECUTED (completed successfully), or FAILED (execution error).", example = "PENDING")
    private AgenticActionStatusEnum status;

    @Schema(description = "Timestamp after which the action is automatically cancelled if not yet confirmed or executed.", example = "2026-06-18T12:00:00")
    private LocalDateTime expireTime;

}
