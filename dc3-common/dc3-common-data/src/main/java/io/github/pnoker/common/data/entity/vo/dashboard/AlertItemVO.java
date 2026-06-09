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

package io.github.pnoker.common.data.entity.vo.dashboard;

import io.github.pnoker.common.enums.AlarmTypeEnum;
import io.github.pnoker.common.enums.ConfirmFlagEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row in the alert list panel on the home page. Source is either {@code device} (with
 * point_id) or {@code driver}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "One row in the home page alert list panel")
public class AlertItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "alert row ID")
    private Long id;

    @Schema(description = "alert source: device or driver")
    private String source;

    @Schema(description = "source entity ID (device ID or driver ID)")
    private Long sourceId;

    @Schema(description = "point ID, present when source is device")
    private Long pointId;

    @Schema(description = "Alarm type flag", example = "OFFLINE")
    private AlarmTypeEnum alarmTypeFlag;

    @Schema(description = "Confirm flag", example = "UNCONFIRMED")
    private ConfirmFlagEnum confirmFlag;

    @Schema(description = "alarm creation time")
    private LocalDateTime createTime;

    /**
     * Human-readable message extracted from alarm_ext->>'content'. Populated by the
     * paging / list endpoints; latest(size=N) leaves it null.
     */
    @Schema(description = "human-readable alarm message")
    private String message;

}
