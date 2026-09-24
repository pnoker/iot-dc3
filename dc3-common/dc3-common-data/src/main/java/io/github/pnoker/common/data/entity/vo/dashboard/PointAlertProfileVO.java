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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Alarm profile of a single point (告警画像): the alarm-type distribution and
 * the daily alarm counts inside the queried window. The recent-alert list is
 * served separately by the existing alert page endpoint with {@code source=point}.
 *
 * @author pnoker
 * @since 2026.9.22
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Alarm profile of a single point: type distribution and daily counts")
public class PointAlertProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Alarm-type distribution for the point, counts descending")
    private List<AlertTypeBucketVO> typeDistribution = new ArrayList<>();

    @Schema(description = "Daily alarm counts for the point, days ascending")
    private List<AlertDaily> dailyTrend = new ArrayList<>();

    /**
     * One day of the point-scoped alarm trend.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One day of the point-scoped alarm trend")
    public static class AlertDaily implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Calendar day, ISO date string", example = "2026-09-22")
        private String date;

        @Schema(description = "Alarm count on that day")
        private long count;
    }
}
