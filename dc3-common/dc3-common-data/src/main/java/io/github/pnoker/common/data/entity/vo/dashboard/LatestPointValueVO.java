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

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.pnoker.common.constant.common.TimeConstant;
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
 * One row in the dashboard live-data feed — the most recent N point-value entries across
 * every typed hypertable.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One row in the dashboard live-data feed")
public class LatestPointValueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "device ID")
    private Long deviceId;

    @Schema(description = "point ID")
    private Long pointId;

    @Schema(description = "driver ID")
    private Long driverId;

    /**
     * Display name for the device, resolved via {@code DeviceFacade}. May be {@code null}
     * when the device has been deleted but historical point values still reference it.
     */
    @Schema(description = "display name for the device, may be null if the device was deleted")
    private String deviceName;

    /**
     * Display name for the point.
     */
    @Schema(description = "display name for the point")
    private String pointName;

    /**
     * Display name for the driver that owns the device.
     */
    @Schema(description = "display name for the driver that owns the device")
    private String driverName;

    @Schema(description = "raw value")
    private String rawValue;

    @Schema(description = "calculated value")
    private String calValue;

    /**
     * Which hypertable the row came from: STRING / INT / LONG / BOOL / FLOAT / DOUBLE /
     * JSON. Lets the UI pick formatting without fetching the point's metadata.
     */
    @Schema(description = "value type: STRING, INT, LONG, BOOL, FLOAT, DOUBLE or JSON")
    private String valueType;

    @Schema(description = "sample creation time")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime createTime;

}
