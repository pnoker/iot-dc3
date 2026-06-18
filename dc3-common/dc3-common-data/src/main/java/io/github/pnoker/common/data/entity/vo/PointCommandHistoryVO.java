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

package io.github.pnoker.common.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View object for point command history API responses.
 *
 * @author pnoker
 * @version 2026.6.5
 * @since 2026.6.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Point Command History view object")
public class PointCommandHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary key")

    private Long id;

    @Schema(description = "ID of the command definition invoked.", example = "4096")

    private String commandId;

    @Schema(description = "Tenant ID")

    private Long tenantId;

    @Schema(description = "Point command type")

    private PointCommandTypeEnum type;

    @Schema(description = "ID of the device the point command was sent to.", example = "1024")

    private Long deviceId;

    @Schema(description = "ID of the target data point.", example = "2048")

    private Long pointId;

    @Schema(description = "Command value sent to the data point.")

    private String requestValue;

    @Schema(description = "Response value received from the data point after command execution.")

    private String responseValue;

    @Schema(description = "Command status", example = "SUCCESS")
    private PointCommandStatusEnum status;

    @Schema(description = "Error code if the point command execution failed. Null on success.", example = "ERR_TIMEOUT")

    private String errorCode;

    @Schema(description = "Human-readable error message if the point command execution failed. Null on success.")

    private String errorMessage;

    @Schema(description = "Source identifier")

    private PointCommandSourceEnum source;

    @Schema(description = "ID of the user who issued this point command.")

    private Long sourceUserId;

    @Schema(description = "Timestamp when the point command was issued.")

    private LocalDateTime occurTime;

    @Schema(description = "Timestamp when the point command was dispatched.")

    private LocalDateTime sendTime;

    @Schema(description = "Timestamp when the point command execution completed.")

    private LocalDateTime finishTime;

    @Schema(description = "Timestamp when the point command expires without a response.")

    private LocalDateTime expireTime;

    @Schema(description = "Schema version")

    private Short schemaVersion;

    @Schema(description = "Creation time")

    private LocalDateTime createTime;

    @Schema(description = "Last operation time")

    private LocalDateTime operateTime;

}
