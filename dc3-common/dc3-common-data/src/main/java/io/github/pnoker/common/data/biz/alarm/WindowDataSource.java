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

import io.github.pnoker.common.enums.WindowMode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pluggable backend for windowed alarm evaluation. Two production
 * implementations exist:
 *
 * <ul>
 *   <li>{@link LocalWindowDataSource} reads from {@link WindowSampleBuffer}
 *       — fast, but bounded by retention.</li>
 *   <li>{@link RepositoryWindowDataSource} pushes the aggregate to the
 *       time-series store — durable, but with per-evaluation latency.</li>
 * </ul>
 *
 * <p>{@link HybridWindowDataSource} routes between them by window duration.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
public interface WindowDataSource {

    /**
     * Return the scalar aggregate (AVG/MIN/MAX/SUM/COUNT) for the rule's
     * window. The result also carries the sample count so callers can enforce
     * the {@code minSamples} guard. Modes that do not reduce to a scalar
     * (LAST/ALL/ANY) should not be passed in.
     */
    AggregateOutcome aggregate(WindowSpec spec, RuleFact fact, WindowMode mode);

    /**
     * Pull the raw samples in the rule's window, ordered oldest → newest.
     * Used by ALL/ANY where the rule condition runs sample-by-sample.
     */
    List<WindowSample> samples(WindowSpec spec, RuleFact fact);

    /**
     * Aggregate result + sample count. The value is null when the window had
     * no usable samples (numeric aggregate over an empty / all-null window).
     */
    record AggregateOutcome(BigDecimal value, long sampleCount) {

        public static AggregateOutcome empty() {
            return new AggregateOutcome(null, 0L);
        }

    }

}
