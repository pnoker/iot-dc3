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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.WindowModeEnum;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TsdbWindowDataSourceTest {
    private static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private ReactiveTsdbStore tsdbStore;

    @Spy
    private PointValueSampleConverter converter = new PointValueSampleConverter();

    @InjectMocks
    private TsdbWindowDataSource dataSource;

    private static RuleFact pointFact() {
        RuleFact fact = new RuleFact();
        fact.setTenantId(1L);
        fact.setEntityId(20L);
        fact.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.POINT);
        return fact;
    }

    private static WindowSpec specOf(java.time.Duration duration) {
        return new WindowSpec(null, duration, 0, true, null);
    }

    @Test
    void aggregatePushesFunctionAndWindowToThePort() {
        RuleFact fact = pointFact();
        fact.setValues(Map.of("deviceId", 10L));
        fact.setFactTime(LocalDateTime.parse("2026-08-20T12:00:00"));
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        when(tsdbStore.aggregate(eq(SeriesFilter.of(series)), eq(AggregateFunction.AVG), any(), isNull(), any()))
                .thenReturn(Mono.just(Map.of(series, new WindowAggregate(2.5d, 4L))));
        WindowDataSource.AggregateOutcome outcome = dataSource
                .aggregate(specOf(java.time.Duration.ofMinutes(30)), fact, WindowModeEnum.AVG)
                .block();
        assertThat(outcome.value()).isEqualByComparingTo("2.5");
        assertThat(outcome.sampleCount()).isEqualTo(4L);
    }

    @Test
    void aggregateWithoutSeriesAnswerIsEmpty() {
        RuleFact fact = pointFact();
        fact.setValues(Map.of("deviceId", 10L));
        when(tsdbStore.aggregate(any(), any(), any(), isNull(), any())).thenReturn(Mono.just(Map.of()));
        WindowDataSource.AggregateOutcome outcome = dataSource
                .aggregate(specOf(java.time.Duration.ofMinutes(30)), fact, WindowModeEnum.COUNT)
                .block();
        assertThat(outcome.value()).isNull();
        assertThat(outcome.sampleCount()).isZero();
    }

    @Test
    void nonPointFactsShortCircuit() {
        RuleFact fact = new RuleFact();
        fact.setTenantId(1L);
        fact.setEntityId(10L);
        fact.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.DEVICE);
        assertThat(dataSource
                        .aggregate(specOf(java.time.Duration.ofMinutes(30)), fact, WindowModeEnum.AVG)
                        .block()
                        .sampleCount())
                .isZero();
        assertThat(dataSource
                        .samples(specOf(java.time.Duration.ofMinutes(30)), fact)
                        .collectList()
                        .block())
                .isEmpty();
        verify(tsdbStore, never()).aggregate(any(), any(), any(), any(), any());
    }

    @Test
    void samplesDrainDescendingPagesAndReturnOldestFirst() {
        RuleFact fact = pointFact();
        fact.setValues(Map.of("deviceId", 10L));
        fact.setFactTime(LocalDateTime.parse("2026-08-20T12:00:00"));
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        SeriesFilter filter = SeriesFilter.of(series);
        PointValueSample newest = PointValueSample.simple(
                series,
                LocalDateTime.parse("2026-08-20T11:59:30").atZone(PLATFORM_ZONE).toInstant(),
                2);
        PointValueSample middle = PointValueSample.simple(
                series,
                LocalDateTime.parse("2026-08-20T11:59:20").atZone(PLATFORM_ZONE).toInstant(),
                1);
        PointValueSample oldest = PointValueSample.simple(
                series,
                LocalDateTime.parse("2026-08-20T11:59:10").atZone(PLATFORM_ZONE).toInstant(),
                0);
        when(tsdbStore.history(eq(filter), any(), isNull(), any(Integer.class), any()))
                .thenReturn(Mono.just(new CursorPage<>(
                        List.of(newest, middle), new Cursor(oldest.deviceTime(), oldest.messageId(), series))));
        when(tsdbStore.history(eq(filter), any(), any(Cursor.class), any(Integer.class), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(oldest), null)));
        List<WindowSample> samples = dataSource
                .samples(specOf(java.time.Duration.ofMinutes(30)), fact)
                .collectList()
                .block();
        assertThat(samples).hasSize(3);
        assertThat(samples.getFirst().numValue()).isEqualTo(0d);
        assertThat(samples.getLast().numValue()).isEqualTo(2d);
    }

    @Test
    void portFailurePropagates() {
        RuleFact fact = pointFact();
        fact.setValues(Map.of("deviceId", 10L));
        when(tsdbStore.aggregate(any(), any(), any(), isNull(), any()))
                .thenReturn(Mono.error(new IllegalStateException("store down")));
        assertThatThrownBy(() -> dataSource
                        .aggregate(specOf(java.time.Duration.ofMinutes(5)), fact, WindowModeEnum.AVG)
                        .block())
                .hasMessage("store down");
    }
}
