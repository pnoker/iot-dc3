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

package io.github.pnoker.common.entity.bo;

import java.math.BigDecimal;

/**
 * Result of a window aggregation query.
 *
 * @param value       scalar aggregate from the SQL function — null when the
 *                    window contains no numeric samples (or, for COUNT, the
 *                    integer count packaged as a BigDecimal).
 * @param sampleCount number of rows that fell inside {@code [from, to)}.
 *                    Aggregators use this to enforce {@code minSamples}.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
public record WindowAggregateResult(BigDecimal value, long sampleCount) {

    public static WindowAggregateResult empty() {
        return new WindowAggregateResult(null, 0L);
    }

}
