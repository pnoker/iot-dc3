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

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.pnoker.common.constant.common.TimeConstant;
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
 * View object for point value API responses.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Point Value view object")
public class PointValueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Device ID
     */
    @Schema(description = "ID of the device that produced this value.", example = "1024")
    private Long deviceId;

    /**
     * Point ID
     */
    @Schema(description = "ID of the data point this value belongs to.", example = "2048")
    private Long pointId;

    /**
     * Raw value
     */
    @Schema(description = "Raw value as received directly from the device protocol.", example = "12345")
    private String rawValue;

    /**
     * Processed value
     */
    @Schema(description = "Calculated (engineering) value after applying base value, multiplier, and decimal formatting.", example = "25.5")
    private String calValue;

    /**
     * Numeric projection of calValue for aggregation queries.
     */
    @Schema(description = "Numeric representation of the calculated value for sorting and aggregation.", example = "25.5")
    private Double numValue;

    /**
     * Whether the latest-value query returned a real sampled value.
     */
    @Schema(description = "Whether the latest-value query returned a real sampled value (true) or a fallback placeholder (false).", example = "true")
    private Boolean hasLatestValue = Boolean.TRUE;

    /**
     * Driver ID
     */
    @Schema(description = "ID of the driver that collected this value.", example = "512")
    private Long driverId;

    /**
     * Tenant ID
     */
    @Schema(description = "Identifier of the tenant that owns this point value.", example = "1")
    private Long tenantId;

    /**
     * Create Time
     */
    @Schema(description = "Timestamp (ISO-8601) when this point value was first created.", example = "2025-09-01T12:00:00")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime createTime;

    /**
     * Operate Time
     */
    @Schema(description = "Timestamp (ISO-8601) of the most recent update or refresh of this point value.", example = "2025-09-01T12:30:00")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime operateTime;

}
