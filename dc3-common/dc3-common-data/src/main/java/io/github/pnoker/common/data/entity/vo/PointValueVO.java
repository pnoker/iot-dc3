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
    @Schema(description = "device ID")
    private Long deviceId;

    /**
     * Point ID
     */
    @Schema(description = "point ID")
    private Long pointId;

    /**
     * Raw value
     */
    @Schema(description = "raw value")
    private String rawValue;

    /**
     * Processed value
     */
    @Schema(description = "cal value")
    private String calValue;

    /**
     * Numeric projection of calValue for aggregation queries.
     */
    @Schema(description = "num value")
    private Double numValue;

    /**
     * Whether the latest-value query returned a real sampled value.
     */
    @Schema(description = "whether the latest-value query returned a real sampled value")
    private Boolean hasLatestValue = Boolean.TRUE;

    /**
     * Driver ID
     */
    @Schema(description = "driver ID")
    private Long driverId;

    /**
     * Tenant ID
     */
    @Schema(description = "Tenant ID")
    private Long tenantId;

    /**
     * Create Time
     */
    @Schema(description = "Creation time")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime createTime;

    /**
     * Operate Time
     */
    @Schema(description = "Last operation time")
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime operateTime;

}
