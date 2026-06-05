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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Reads window samples out of {@link WindowSampleBuffer} and folds them in
 * Java. Fast enough for short windows (≤ a few thousand samples) — long
 * windows route to {@link RepositoryWindowDataSource} instead.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Component
@RequiredArgsConstructor
public class LocalWindowDataSource implements WindowDataSource {

    /**
     * Scale used for AVG so callers don't have to deal with infinite-precision
     * results. Mirrors the typical PostgreSQL {@code AVG(numeric)} behavior.
     */
    private static final int AVG_SCALE = 6;

    private final WindowSampleBuffer windowSampleBuffer;

    @Override
    public AggregateOutcome aggregate(WindowSpec spec, RuleFact fact, WindowModeEnum mode) {
        List<WindowSample> samples = samples(spec, fact);
        long count = samples.size();
        if (mode == WindowModeEnum.COUNT) {
            return new AggregateOutcome(BigDecimal.valueOf(count), count);
        }
        long numericCount = samples.stream().filter(WindowSample::isNumeric).count();
        if (numericCount == 0) {
            return new AggregateOutcome(null, count);
        }
        BigDecimal value = switch (mode) {
            case SUM -> samples.stream().filter(WindowSample::isNumeric)
                    .map(s -> BigDecimal.valueOf(s.numValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case MIN -> samples.stream().filter(WindowSample::isNumeric)
                    .map(s -> BigDecimal.valueOf(s.numValue()))
                    .min(BigDecimal::compareTo).orElse(null);
            case MAX -> samples.stream().filter(WindowSample::isNumeric)
                    .map(s -> BigDecimal.valueOf(s.numValue()))
                    .max(BigDecimal::compareTo).orElse(null);
            case AVG -> samples.stream().filter(WindowSample::isNumeric)
                    .map(s -> BigDecimal.valueOf(s.numValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(numericCount), AVG_SCALE, RoundingMode.HALF_UP);
            default -> null;
        };
        return new AggregateOutcome(value, count);
    }

    @Override
    public List<WindowSample> samples(WindowSpec spec, RuleFact fact) {
        if (Objects.isNull(spec) || Objects.isNull(spec.duration())
                || Objects.isNull(fact) || Objects.isNull(fact.getEntityId())
                || Objects.isNull(fact.getAlarmTargetTypeFlag())) {
            return List.of();
        }
        LocalDateTime to = Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now());
        LocalDateTime from = to.minus(spec.duration());
        WindowSampleKey key = WindowSampleKey.of(fact.getTenantId(), fact.getAlarmTargetTypeFlag(), fact.getEntityId());
        return windowSampleBuffer.snapshot(key, from, to);
    }

}
