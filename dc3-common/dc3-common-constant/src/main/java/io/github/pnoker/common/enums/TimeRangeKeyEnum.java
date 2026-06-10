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

package io.github.pnoker.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Time-range preset used by dashboard / alert / point-value list endpoints.
 * <p>
 * The frontend sends {@code rangeKey} as one of these codes and the backend resolves it
 * to a concrete {@code from} timestamp. When {@code rangeKey} is absent, the endpoint
 * falls back to the legacy {@code rangeHours} integer.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.3
 */
@Getter
@AllArgsConstructor
public enum TimeRangeKeyEnum {

    /**
     * From local midnight of today to now.
     */
    TODAY("today", "Today"),

    /**
     * Rolling 24 hours ending now.
     */
    H24("24h", "Last 24 hours"),

    /**
     * Rolling 7 days ending now.
     */
    D7("7d", "Last 7 days"),

    /**
     * Rolling 30 days ending now.
     */
    D30("30d", "Last 30 days"),
    ;

    private final String code;

    private final String remark;

    /**
     * Resolve an enum instance by its wire-format code (e.g. {@code "today"}). Matching
     * is case-insensitive; {@code null} / blank input returns {@code null}.
     */
    public static TimeRangeKeyEnum ofCode(String code) {
        if (Objects.isNull(code) || code.isBlank()) {
            return null;
        }
        String normalized = code.trim();
        Optional<TimeRangeKeyEnum> any = Arrays.stream(values())
                .filter(e -> e.getCode().equalsIgnoreCase(normalized))
                .findFirst();
        return any.orElse(null);
    }

}
