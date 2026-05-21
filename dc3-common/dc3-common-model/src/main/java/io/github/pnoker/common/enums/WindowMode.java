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
 * Evaluation modes supported by {@code RuleExt.Window.mode}. All modes other
 * than {@link #LAST} require a non-null duration and produce an aggregate /
 * fold over the samples that fall in {@code (now - duration, now]}.
 *
 * <p>Sample-aggregating modes (AVG/MIN/MAX/SUM/COUNT) reduce the window to a
 * single numeric value that the rule's threshold operator is evaluated against.
 * Boolean-fold modes (ALL/ANY) keep the rule's existing operator and apply it
 * sample-by-sample.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Getter
public enum WindowMode {

    /**
     * Evaluate against the most recent fact only.
     */
    LAST,

    /**
     * Arithmetic mean of {@code numValue} across the window. Numeric only.
     */
    AVG,

    /**
     * Minimum {@code numValue} across the window. Numeric only.
     */
    MIN,

    /**
     * Maximum {@code numValue} across the window. Numeric only.
     */
    MAX,

    /**
     * Sum of {@code numValue} across the window. Numeric only.
     */
    SUM,

    /**
     * Count of samples in the window (numeric or text).
     */
    COUNT,

    /**
     * Every sample must satisfy the rule condition.
     */
    ALL,

    /**
     * At least one sample must satisfy the rule condition.
     */
    ANY;

    /**
     * Resolves a free-form string ({@code "AVG"}, {@code "avg"}, whitespace ok)
     * to the corresponding mode, returning {@code null} when unrecognized.
     */
    public static WindowMode ofCode(String mode) {
        if (StringUtils.isBlank(mode)) {
            return null;
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.name().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether the mode aggregates the window into a scalar before applying the
     * rule's operator. AVG/MIN/MAX/SUM/COUNT do; LAST is single-sample;
     * ALL/ANY apply the operator per-sample and fold the booleans.
     */
    public boolean reducesToScalar() {
        return this == AVG || this == MIN || this == MAX || this == SUM || this == COUNT;
    }

    /**
     * Whether the mode requires a non-null window duration. Only LAST is
     * exempt; everything else needs a defined window to bound its scope.
     */
    public boolean requiresDuration() {
        return this != LAST;
    }

}
