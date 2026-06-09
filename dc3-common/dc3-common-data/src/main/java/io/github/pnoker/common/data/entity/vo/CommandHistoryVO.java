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
import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for command history API responses.
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
@Schema(description = "Command History view object")
public class CommandHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary key")

    private Long id;

    @Schema(description = "record ID")

    private String recordId;

    @Schema(description = "Tenant ID")

    private Long tenantId;

    @Schema(description = "device ID")

    private Long deviceId;

    @Schema(description = "command ID")

    private Long commandId;

    @Schema(description = "command code")

    private String commandCode;

    @Schema(description = "param values")

    private String paramValues;

    @Schema(description = "result values")

    private String resultValues;

    @Schema(description = "config snapshot")

    private String configSnapshot;

    @Schema(description = "Command status", example = "SUCCESS")
    private PointCommandStatusEnum status;

    @Schema(description = "error code")

    private String errorCode;

    @Schema(description = "error message")

    private String errorMessage;

    @Schema(description = "source")

    private CommandHistorySourceEnum source;

    @Schema(description = "source user ID")

    private Long sourceUserId;

    @Schema(description = "occur time")

    private LocalDateTime occurTime;

    @Schema(description = "send time")

    private LocalDateTime sendTime;

    @Schema(description = "finish time")

    private LocalDateTime finishTime;

    @Schema(description = "expire time")

    private LocalDateTime expireTime;

    @Schema(description = "schema version")

    private Short schemaVersion;

    @Schema(description = "Creation time")

    private LocalDateTime createTime;

    @Schema(description = "Last operation time")

    private LocalDateTime operateTime;

}
