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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One cell in the event-overview alarm heatmap (dow × hour). Service layer always returns
 * a fully-padded 7 × 24 grid.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.3
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Event-overview alarm heatmap cell (day-of-week by hour-of-day)")
public class AlertActivityCellVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Day of week, 0..6 matching Postgres EXTRACT(DOW) (0 = Sunday).
     */
    @Schema(description = "day of week, 0 to 6 matching Postgres EXTRACT(DOW), 0=Sunday")
    private int dow;

    /**
     * Hour of day, 0..23.
     */
    @Schema(description = "hour of day, 0 to 23")
    private int hour;

    /**
     * Alarm count in that cell.
     */
    @Schema(description = "alarm count in this cell")
    private long count;

}
