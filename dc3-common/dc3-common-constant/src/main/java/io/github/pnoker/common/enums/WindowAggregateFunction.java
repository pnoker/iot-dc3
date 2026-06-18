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

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * SQL aggregate functions valid for repository window aggregation queries.
 *
 * <p>These map directly to PostgreSQL aggregate functions used by
 * {@code PointValueMapper.aggregateInWindow}. AVG / MIN / MAX / SUM target
 * {@code num_value}; COUNT counts every row in the window.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Getter
public enum WindowAggregateFunction {

    /**
     * Arithmetic mean: {@code AVG(num_value)}.
     */
    AVG,

    /**
     * Minimum value: {@code MIN(num_value)}.
     */
    MIN,

    /**
     * Maximum value: {@code MAX(num_value)}.
     */
    MAX,

    /**
     * Sum: {@code SUM(num_value)}.
     */
    SUM,

    /**
     * Row count: {@code CAST(COUNT(*) AS NUMERIC)}.
     */
    COUNT;

    /**
     * Resolves a free-form string ({@code "AVG"}, {@code "avg"}, whitespace ok)
     * to the corresponding function, returning {@code null} when unrecognized.
     */
    public static WindowAggregateFunction ofCode(String function) {
        if (StringUtils.isBlank(function)) {
            return null;
        }
        String normalized = function.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .orElse(null);
    }

}
