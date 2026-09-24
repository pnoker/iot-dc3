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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Data-dashboard payload for a single point (位号看板). Everything the point
 * detail dashboard renders in one round trip: the value trend band, hourly
 * sample volume, value distribution, sampling-interval health, collection gaps,
 * the typical 24h curve and the window-level stats. Built from TSDB bucket
 * aggregates plus a bounded raw-sample walk; nothing here is derived from
 * platform-wide aggregates.
 *
 * @author pnoker
 * @since 2026.9.22
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Data-dashboard payload for a single point: trend band, sampling health, value profile and stats")
public class PointValueDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Per-bucket trend aggregates (min/max/avg) over the requested window, buckets ascending")
    private List<TrendBucket> trend = new ArrayList<>();

    @Schema(description = "Sample count per hour over the requested window, hours ascending")
    private List<HourVolume> hourlyVolume = new ArrayList<>();

    @Schema(description = "Numeric value histogram over the raw sample walk, bins ascending")
    private List<ValueBin> valueHistogram = new ArrayList<>();

    @Schema(description = "Sampling-interval histogram over fixed bins, bins ascending")
    private List<IntervalBin> intervalHistogram = new ArrayList<>();

    @Schema(description = "Collection gaps (consecutive samples farther apart than 5x the median interval), newest first")
    private List<Gap> gaps = new ArrayList<>();

    @Schema(description = "Typical day curve: per hour-of-day average over the last 7 days")
    private List<HourAverage> typicalDay = new ArrayList<>();

    @Schema(description = "Window-level stats of the raw sample walk")
    private Stats stats = new Stats();

    /**
     * One bucket of the value trend band.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One bucket of the value trend band")
    public static class TrendBucket implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Bucket start (inclusive)")
        private Instant from;

        @Schema(description = "Minimum numeric value in the bucket")
        private Double min;

        @Schema(description = "Maximum numeric value in the bucket")
        private Double max;

        @Schema(description = "Arithmetic mean of numeric values in the bucket")
        private Double avg;

        @Schema(description = "Raw sample count in the bucket")
        private long sampleCount;
    }

    /**
     * Sample volume of one hour.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "Sample volume of one hour")
    public static class HourVolume implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Hour start (inclusive)")
        private Instant hourStart;

        @Schema(description = "Sample count inside the hour")
        private long count;
    }

    /**
     * One bin of the numeric value histogram.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One bin of the numeric value histogram")
    public static class ValueBin implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Lower bound (inclusive)")
        private Double from;

        @Schema(description = "Upper bound (exclusive)")
        private Double to;

        @Schema(description = "Sample count in the bin")
        private long count;
    }

    /**
     * One bin of the sampling-interval histogram.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One bin of the sampling-interval histogram")
    public static class IntervalBin implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Lower bound in milliseconds (inclusive)")
        private long fromMs;

        @Schema(description = "Upper bound in milliseconds (exclusive); null for the open-ended top bin")
        private Long toMs;

        @Schema(description = "Interval count in the bin")
        private long count;
    }

    /**
     * One collection gap between consecutive samples.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One collection gap between consecutive samples")
    public static class Gap implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Gap start (older sample time)")
        private Instant from;

        @Schema(description = "Gap end (newer sample time)")
        private Instant to;

        @Schema(description = "Gap duration in milliseconds")
        private long durationMs;
    }

    /**
     * One hour of the typical day curve.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "One hour of the typical day curve")
    public static class HourAverage implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Hour of day from 0 through 23")
        private int hourOfDay;

        @Schema(description = "Average numeric value across the days with samples in this hour")
        private Double avg;
    }

    /**
     * Window-level stats of the raw sample walk.
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "Window-level stats of the raw sample walk")
    public static class Stats implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "Raw samples walked (bounded by the raw-cap; truncated reports whether the cap cut the walk short)")
        private long sampleCount;

        @Schema(description = "Minimum numeric value in the walk")
        private Double min;

        @Schema(description = "Maximum numeric value in the walk")
        private Double max;

        @Schema(description = "Arithmetic mean of numeric values in the walk")
        private Double avg;

        @Schema(description = "Median sampling interval in milliseconds")
        private Long medianIntervalMs;

        @Schema(description = "True when the walk hit the raw-cap before exhausting the window")
        private boolean truncated;
    }

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

        @Schema(description = "Calendar day")
        private LocalDate date;

        @Schema(description = "Alarm count on that day")
        private long count;
    }
}
