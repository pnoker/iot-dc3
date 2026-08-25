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

import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.WindowModeEnum;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Long-window backend. Pushes the aggregate to the time-series store through
 * the TSDB port so the alarm engine doesn't have to materialize an unbounded
 * sample list in memory. ALL/ANY still pull raw rows here because the rule's
 * condition is sample-by-sample.
 *
 * <p>Only POINT facts are supported — the time-series store is keyed on
 * (tenantId, deviceId, pointId). Device/driver windowed alarms over long
 * spans aren't a current use case; supporting them would require a different
 * storage layout.
 *
 * @author pnoker
 * @since 2026.5.21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TsdbWindowDataSource implements WindowDataSource {

    private static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(30);

    /**
     * Pull page for the ALL/ANY sample path — window scans stay bounded.
     */
    private static final int SAMPLE_PAGE_SIZE = 5000;

    private final TsdbStore tsdbStore;

    private final PointValueSampleConverter converter;

    private static boolean isPointFact(RuleFact fact) {
        return Objects.nonNull(fact) && fact.getAlarmTargetTypeFlag() == AlarmTargetTypeEnum.POINT
                && Objects.nonNull(fact.getTenantId()) && Objects.nonNull(fact.getEntityId());
    }

    private static Long longValue(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @Override
    public AggregateOutcome aggregate(WindowSpec spec, RuleFact fact, WindowModeEnum mode) {
        if (!isPointFact(fact) || Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return AggregateOutcome.empty();
        }
        Long deviceId = longValue(fact.value("deviceId"));
        if (Objects.isNull(deviceId) || deviceId <= 0) {
            return AggregateOutcome.empty();
        }
        LocalDateTime to = Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now());
        LocalDateTime from = to.minus(spec.duration());

        try {
            SeriesKey series = new SeriesKey(fact.getTenantId(), deviceId, fact.getEntityId());
            WindowAggregate result = tsdbStore.aggregate(SeriesFilter.of(series),
                    AggregateFunction.valueOf(mode.toAggregateFunction().name()),
                    new TimeWindow(converter.toInstant(from), converter.toInstant(to)),
                    null, DEADLINE).get(series);
            if (Objects.isNull(result)) {
                return AggregateOutcome.empty();
            }
            BigDecimal value = Objects.isNull(result.value()) ? null : BigDecimal.valueOf(result.value());
            return new AggregateOutcome(value, result.sampleCount());
        } catch (RuntimeException e) {
            log.warn("TSDB window aggregate failed, treating as empty; tenantId={}, pointId={}, mode={}",
                    fact.getTenantId(), fact.getEntityId(), mode, e);
            return AggregateOutcome.empty();
        }
    }

    @Override
    public List<WindowSample> samples(WindowSpec spec, RuleFact fact) {
        if (!isPointFact(fact) || Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return List.of();
        }
        Long deviceId = longValue(fact.value("deviceId"));
        if (Objects.isNull(deviceId) || deviceId <= 0) {
            return List.of();
        }
        LocalDateTime to = Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now());
        LocalDateTime from = to.minus(spec.duration());
        try {
            SeriesKey series = new SeriesKey(fact.getTenantId(), deviceId, fact.getEntityId());
            TimeWindow window = new TimeWindow(converter.toInstant(from), converter.toInstant(to));

            // The port pages descending; ALL/ANY evaluates oldest → newest, so
            // drain the window and flip once at the end.
            List<PointValueSample> descending = new ArrayList<>();
            CursorPage<PointValueSample> page = tsdbStore.history(SeriesFilter.of(series), window,
                    null, SAMPLE_PAGE_SIZE, DEADLINE);
            descending.addAll(page.items());
            while (Objects.nonNull(page.nextCursor()) && descending.size() < SAMPLE_PAGE_SIZE * 20) {
                page = tsdbStore.history(SeriesFilter.of(series), window, page.nextCursor(),
                        SAMPLE_PAGE_SIZE, DEADLINE);
                descending.addAll(page.items());
            }
            descending.sort(Comparator.comparing(PointValueSample::deviceTime));
            return descending.stream()
                    .map(sample -> new WindowSample(sample.numericValue(), sample.calValue(),
                            converter.toWallClock(sample.deviceTime())))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("TSDB window samples failed; tenantId={}, pointId={}",
                    fact.getTenantId(), fact.getEntityId(), e);
            return List.of();
        }
    }

}
