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

import io.github.pnoker.common.constant.common.TimeConstant;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.WindowModeEnum;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
@Component
@RequiredArgsConstructor
public class TsdbWindowDataSource implements WindowDataSource {

    private static final TsdbDeadline DEADLINE = TsdbDeadline.ofSeconds(30);

    /**
     * Pull page for the ALL/ANY sample path — window scans stay bounded.
     */
    private static final int SAMPLE_PAGE_SIZE = 5000;

    private final ReactiveTsdbStore tsdbStore;

    private final PointValueSampleConverter converter;

    private static boolean isPointFact(RuleFact fact) {
        return Objects.nonNull(fact)
                && fact.getAlarmTargetTypeFlag() == AlarmTargetTypeEnum.POINT
                && Objects.nonNull(fact.getTenantId())
                && Objects.nonNull(fact.getEntityId());
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
    public Mono<AggregateOutcome> aggregate(WindowSpec spec, RuleFact fact, WindowModeEnum mode) {
        if (!isPointFact(fact) || Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return Mono.just(AggregateOutcome.empty());
        }
        Long deviceId = longValue(fact.value("deviceId"));
        if (Objects.isNull(deviceId) || deviceId <= 0) {
            return Mono.just(AggregateOutcome.empty());
        }
        LocalDateTime to =
                Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now(TimeConstant.DEFAULT_ZONEID));
        LocalDateTime from = to.minus(spec.duration());

        // Store failures propagate on purpose: swallowing them here would make
        // an outage look like "no data in window", which the rule engine reads
        // as non-exceeding — silent missed alarms. Failing the evaluation keeps
        // the breakage visible.
        SeriesKey series = new SeriesKey(fact.getTenantId(), deviceId, fact.getEntityId());
        return tsdbStore
                .aggregate(
                        SeriesFilter.of(series),
                        AggregateFunction.valueOf(mode.toAggregateFunction().name()),
                        new TimeWindow(converter.toInstant(from), converter.toInstant(to)),
                        null,
                        DEADLINE)
                .flatMap(values -> Mono.justOrEmpty(values.get(series)))
                .map(result -> new AggregateOutcome(
                        Objects.isNull(result.value()) ? null : BigDecimal.valueOf(result.value()),
                        result.sampleCount()))
                .defaultIfEmpty(AggregateOutcome.empty());
    }

    @Override
    public Flux<WindowSample> samples(WindowSpec spec, RuleFact fact) {
        if (!isPointFact(fact) || Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return Flux.empty();
        }
        Long deviceId = longValue(fact.value("deviceId"));
        if (Objects.isNull(deviceId) || deviceId <= 0) {
            return Flux.empty();
        }
        LocalDateTime to =
                Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now(TimeConstant.DEFAULT_ZONEID));
        LocalDateTime from = to.minus(spec.duration());
        // Same propagation policy as aggregate(): silent empty = missed alarms.
        SeriesKey series = new SeriesKey(fact.getTenantId(), deviceId, fact.getEntityId());
        TimeWindow window = new TimeWindow(converter.toInstant(from), converter.toInstant(to));

        return tsdbStore
                .history(SeriesFilter.of(series), window, null, SAMPLE_PAGE_SIZE, DEADLINE)
                .expand(page -> Objects.isNull(page.nextCursor())
                        ? Mono.empty()
                        : tsdbStore.history(
                                SeriesFilter.of(series), window, page.nextCursor(), SAMPLE_PAGE_SIZE, DEADLINE))
                .concatMapIterable(page -> page.items())
                .take((long) SAMPLE_PAGE_SIZE * 20)
                .sort(Comparator.comparing(PointValueSample::deviceTime))
                .map(sample -> new WindowSample(
                        sample.numericValue(), sample.calValue(), converter.toWallClock(sample.deviceTime())));
    }
}
