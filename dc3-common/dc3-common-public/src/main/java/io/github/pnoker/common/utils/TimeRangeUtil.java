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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.enums.TimeRangeKeyEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Resolves UI time-range inputs ({@link TimeRangeKeyEnum} or legacy hours) into
 * concrete {@link LocalDateTime} lower bounds for {@code create_time >= ?} queries.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.3
 */
public final class TimeRangeUtil {

    /**
     * Default hour spans for each rolling preset.
     */
    public static final int HOURS_24H = 24;

    public static final int HOURS_7D = 24 * 7;

    public static final int HOURS_30D = 24 * 30;

    private TimeRangeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolve the lower-bound timestamp for a range query.
     *
     * @param rangeKey   parsed key enum; {@code null} falls back to {@code rangeHours}
     * @param rangeHours legacy integer fallback; {@code null} or non-positive means "no
     *                   bound"
     * @return lower-bound {@code LocalDateTime}, or {@code null} when the caller should
     * not filter by {@code create_time}
     */
    public static LocalDateTime resolveFrom(TimeRangeKeyEnum rangeKey, Integer rangeHours) {
        if (Objects.nonNull(rangeKey)) {
            return switch (rangeKey) {
                case TODAY -> LocalDate.now().atStartOfDay();
                case H24 -> LocalDateTime.now().minusHours(HOURS_24H);
                case D7 -> LocalDateTime.now().minusHours(HOURS_7D);
                case D30 -> LocalDateTime.now().minusHours(HOURS_30D);
            };
        }
        if (Objects.nonNull(rangeHours) && rangeHours > 0) {
            return LocalDateTime.now().minusHours(rangeHours);
        }
        return null;
    }

    /**
     * Convenience overload that accepts the raw wire-format code.
     */
    public static LocalDateTime resolveFrom(String rangeKeyCode, Integer rangeHours) {
        return resolveFrom(TimeRangeKeyEnum.ofCode(rangeKeyCode), rangeHours);
    }

    /**
     * Convert a range key to the hour span it represents, so existing mappers that only
     * accept {@code rangeHours} (e.g. {@code /top}, {@code /stats/timeseries} granularity
     * math) can keep working unchanged. {@code TODAY} is reported as the number of hours
     * since local midnight rounded up to the next whole hour (minimum 1), which lines up
     * with sparkline bucketing.
     *
     * @return effective hour span, or {@code null} when no bound applies
     */
    public static Integer resolveHours(TimeRangeKeyEnum rangeKey, Integer rangeHours) {
        if (Objects.nonNull(rangeKey)) {
            return switch (rangeKey) {
                case TODAY -> {
                    LocalDateTime midnight = LocalDate.now().atStartOfDay();
                    long minutes = java.time.Duration.between(midnight, LocalDateTime.now()).toMinutes();
                    // round up to the next whole hour so bucketed queries cover the full
                    // day so far
                    int hours = (int) Math.max(1, (minutes + 59) / 60);
                    yield hours;
                }
                case H24 -> HOURS_24H;
                case D7 -> HOURS_7D;
                case D30 -> HOURS_30D;
            };
        }
        if (Objects.nonNull(rangeHours) && rangeHours > 0) {
            return rangeHours;
        }
        return null;
    }

    /**
     * Convenience overload that accepts the raw wire-format code.
     */
    public static Integer resolveHours(String rangeKeyCode, Integer rangeHours) {
        return resolveHours(TimeRangeKeyEnum.ofCode(rangeKeyCode), rangeHours);
    }

    /**
     * Convert a range key to a day count for endpoints that bucket by day (e.g.
     * {@code /alert/trend?days=30}). {@code TODAY} and {@code H24} both map to 1 day; the
     * legacy {@code days} integer passes through.
     *
     * @return effective day count, or {@code null} when no bound applies
     */
    public static Integer resolveDays(TimeRangeKeyEnum rangeKey, Integer days) {
        if (Objects.nonNull(rangeKey)) {
            return switch (rangeKey) {
                case TODAY, H24 -> 1;
                case D7 -> 7;
                case D30 -> 30;
            };
        }
        if (Objects.nonNull(days) && days > 0) {
            return days;
        }
        return null;
    }

    /**
     * Convenience overload that accepts the raw wire-format code.
     */
    public static Integer resolveDays(String rangeKeyCode, Integer days) {
        return resolveDays(TimeRangeKeyEnum.ofCode(rangeKeyCode), days);
    }

}
