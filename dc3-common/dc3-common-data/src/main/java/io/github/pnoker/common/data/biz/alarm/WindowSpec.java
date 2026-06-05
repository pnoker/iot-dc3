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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.enums.WindowModeEnum;

import java.time.Duration;

/**
 * Parsed view of {@code RuleExt.Window}. {@link WindowSpecParser} turns the
 * raw JSON values into this typed shape; consumers (rule save validation,
 * runtime evaluator, data-source router) read it without re-parsing the
 * underlying strings.
 *
 * @param mode       evaluation mode; never null (defaults to LAST when the
 *                   rule does not declare a window).
 * @param duration   parsed ISO-8601 duration; null for LAST.
 * @param minSamples minimum samples that must fall in the window for the rule
 *                   to fire. Always &ge; 0; LAST treats &lt; 1 as 1.
 * @param valid      true when the spec is internally consistent. Invalid
 *                   specs carry the rejection reason in {@link #reason()}.
 * @param reason     human-readable rejection cause when {@code valid} is false;
 *                   null otherwise.
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
public record WindowSpec(WindowModeEnum mode, Duration duration, int minSamples, boolean valid, String reason) {

    public static WindowSpec last() {
        return new WindowSpec(WindowModeEnum.LAST, null, 1, true, null);
    }

    public static WindowSpec ok(WindowModeEnum mode, Duration duration, int minSamples) {
        return new WindowSpec(mode, duration, Math.max(0, minSamples), true, null);
    }

    public static WindowSpec invalid(String reason) {
        return new WindowSpec(null, null, 0, false, reason);
    }

}
