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

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Data-quality snapshot for one device inside a rolling window: how many stored rows
 * carry a numeric projection versus non-numeric payloads, plus the newest sample time
 * across the device's points.
 *
 * @author pnoker
 * @since 2026.9.24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Data-quality snapshot for one device inside a rolling window")
public class DeviceQualityVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "total stored rows in the window", example = "10000")
    private long totalSamples;

    @Schema(description = "rows with a numeric projection", example = "9800")
    private long numericSamples;

    @Schema(description = "rows without a numeric projection", example = "200")
    private long nonNumericSamples;

    /**
     * Percentage of numeric rows in [0, 100], 0 when there are no rows at all.
     */
    @Schema(description = "percentage of numeric rows in [0, 100]; 0 when the window has no rows", example = "98.0")
    private double numericRatio;

    /**
     * Newest sample time across the device's points, null when the device never reported.
     */
    @Schema(description = "newest sample time across the device's points; null when the device never reported")
    private LocalDateTime latestSeen;
}
