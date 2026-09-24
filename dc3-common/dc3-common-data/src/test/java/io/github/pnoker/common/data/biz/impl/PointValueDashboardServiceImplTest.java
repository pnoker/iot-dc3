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
package io.github.pnoker.common.data.biz.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.entity.vo.dashboard.PointValueDashboardVO;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Contract tests for the point data-dashboard service: bucket aggregation
 * merge, raw-walk histogram/interval/gap derivation, truncation flag and
 * scope validation.
 */
@ExtendWith(MockitoExtension.class)
class PointValueDashboardServiceImplTest {

    private static final long TENANT = 9L;
    private static final long DEVICE = 7L;
    private static final long POINT = 8L;
    private static final SeriesKey SERIES = new SeriesKey(TENANT, DEVICE, POINT);
    private static final Instant T0 = Instant.parse("2026-09-22T00:00:00Z");
    private static final Duration TREND_BUCKET = Duration.ofMinutes(15);
    private static final Duration HOUR = Duration.ofHours(1);

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private ReactiveTsdbStore tsdbStore;

    private PointValueDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PointValueDashboardServiceImpl(8000, deviceFacade, pointFacade, tsdbStore);
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(DEVICE);
        device.setProfileId(1L);
        FacadePointBO point = new FacadePointBO();
        point.setId(POINT);
        point.setProfileId(1L);
        lenient().when(deviceFacade.getByIdReactive(TENANT, DEVICE)).thenReturn(Mono.just(device));
        lenient().when(pointFacade.getByIdReactive(TENANT, POINT)).thenReturn(Mono.just(point));
    }

    @Test
    void dashboardMergesBucketAggregatesAndDerivesRawProfile() {
        // trend aggregates at the 15m bucket: aligned avg/min/max rows
        when(tsdbStore.bucketedAggregate(any(), eq(AggregateFunction.AVG), any(), eq(TREND_BUCKET), isNull(), any()))
                .thenReturn(Mono.just(Map.of(SERIES, List.of(
                        new BucketAggregate(T0, 42.0d, 4L), new BucketAggregate(T0.plus(TREND_BUCKET), 44.0d, 4L)))));
        when(tsdbStore.bucketedAggregate(any(), eq(AggregateFunction.MIN), any(), eq(TREND_BUCKET), isNull(), any()))
                .thenReturn(Mono.just(Map.of(SERIES, List.of(
                        new BucketAggregate(T0, 40.0d, 4L), new BucketAggregate(T0.plus(TREND_BUCKET), 41.0d, 4L)))));
        when(tsdbStore.bucketedAggregate(any(), eq(AggregateFunction.MAX), any(), eq(TREND_BUCKET), isNull(), any()))
                .thenReturn(Mono.just(Map.of(SERIES, List.of(
                        new BucketAggregate(T0, 45.0d, 4L), new BucketAggregate(T0.plus(TREND_BUCKET), 46.0d, 4L)))));
        // hourly volume + typical-day (1h AVG over 7 days)
        when(tsdbStore.bucketedAggregate(any(), eq(AggregateFunction.COUNT), any(), eq(HOUR), isNull(), any()))
                .thenReturn(Mono.just(Map.of(SERIES, List.of(new BucketAggregate(T0, null, 120L)))));
        when(tsdbStore.bucketedAggregate(any(), eq(AggregateFunction.AVG), any(), eq(HOUR), isNull(), any()))
                .thenReturn(Mono.just(Map.of(SERIES, List.of(new BucketAggregate(T0, 42.0d, 120L)))));
        // raw walk: two pages, the second ending the walk; samples at 1s cadence plus one 10-minute gap
        when(tsdbStore.history(any(), any(), isNull(), anyInt(), any()))
                .thenReturn(Mono.just(new CursorPage<>(
                        List.of(
                                PointValueSample.simple(SERIES, T0.plusSeconds(600), 43.0d),
                                PointValueSample.simple(SERIES, T0.plusSeconds(2), 44.0d),
                                PointValueSample.simple(SERIES, T0.plusSeconds(1), 42.0d)),
                        new Cursor(T0.plusSeconds(2), "m-2", SERIES, T0, T0.plusSeconds(600)))));
        when(tsdbStore.history(any(), any(), any(Cursor.class), anyInt(), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(PointValueSample.simple(SERIES, T0, 41.0d)), null)));

        PointValueDashboardVO vo = service.dashboard(TENANT, DEVICE, POINT, 24).block();

        assertNotNull(vo);
        assertEquals(2, vo.getTrend().size());
        assertEquals(42.0d, vo.getTrend().getFirst().getAvg());
        assertEquals(40.0d, vo.getTrend().getFirst().getMin());
        assertEquals(45.0d, vo.getTrend().getFirst().getMax());
        assertEquals(4L, vo.getTrend().getFirst().getSampleCount());
        assertEquals(1, vo.getHourlyVolume().size());
        assertEquals(120L, vo.getHourlyVolume().getFirst().getCount());
        assertEquals(1, vo.getTypicalDay().size());
        assertEquals(42.0d, vo.getTypicalDay().getFirst().getAvg());
        assertEquals(T0.atZone(java.time.ZoneId.systemDefault()).getHour(), vo.getTypicalDay().getFirst().getHourOfDay());
        // stats: 4 samples, median interval 1s, one gap of ~598s
        assertEquals(4L, vo.getStats().getSampleCount());
        assertEquals(41.0d, vo.getStats().getMin());
        assertEquals(44.0d, vo.getStats().getMax());
        assertFalse(vo.getStats().isTruncated());
        assertEquals(1, vo.getGaps().size());
        assertTrue(vo.getGaps().getFirst().getDurationMs() >= 590_000L);
        // value histogram: 4 numeric samples spread over 4 adaptive bins
        assertEquals(4, vo.getValueHistogram().size());
        assertEquals(4L, vo.getValueHistogram().stream().mapToLong(bin -> bin.getCount()).sum());
        // interval histogram: 2x ~1s intervals and 1x ~598s interval (3 deltas)
        assertEquals(3L, vo.getIntervalHistogram().stream()
                .mapToLong(bin -> bin.getCount())
                .sum());
    }

    @Test
    void emptySeriesYieldsEmptyDashboard() {
        when(tsdbStore.bucketedAggregate(any(), any(), any(), any(), isNull(), any()))
                .thenReturn(Mono.just(Map.of()));
        when(tsdbStore.history(any(), any(), isNull(), anyInt(), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(), null)));

        PointValueDashboardVO vo = service.dashboard(TENANT, DEVICE, POINT, 24).block();

        assertNotNull(vo);
        assertTrue(vo.getTrend().isEmpty());
        assertTrue(vo.getHourlyVolume().isEmpty());
        assertTrue(vo.getValueHistogram().isEmpty());
        assertTrue(vo.getGaps().isEmpty());
        assertEquals(0L, vo.getStats().getSampleCount());
        assertFalse(vo.getStats().isTruncated());
    }

    @Test
    void capTruncatesTheWalkAndReportsIt() {
        PointValueDashboardServiceImpl capped =
                new PointValueDashboardServiceImpl(2, deviceFacade, pointFacade, tsdbStore);
        when(tsdbStore.bucketedAggregate(any(), any(), any(), any(), isNull(), any()))
                .thenReturn(Mono.just(Map.of()));
        when(tsdbStore.history(any(), any(), isNull(), anyInt(), any()))
                .thenReturn(Mono.just(new CursorPage<>(
                        List.of(
                                PointValueSample.simple(SERIES, T0.plusSeconds(2), 44.0d),
                                PointValueSample.simple(SERIES, T0.plusSeconds(1), 42.0d)),
                        new Cursor(T0.plusSeconds(2), "m-2", SERIES, T0, T0.plusSeconds(600)))));

        PointValueDashboardVO vo = capped.dashboard(TENANT, DEVICE, POINT, 24).block();

        assertEquals(2L, vo.getStats().getSampleCount());
        assertTrue(vo.getStats().isTruncated());
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.dashboard(null, DEVICE, POINT, 24).block());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.dashboard(TENANT, DEVICE, POINT, 0).block());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.dashboard(TENANT, DEVICE, POINT, 169).block());
    }

    @Test
    void scopeMismatchIsRejected() {
        when(deviceFacade.getByIdReactive(TENANT, DEVICE)).thenReturn(Mono.empty());
        when(pointFacade.getByIdReactive(TENANT, POINT)).thenReturn(Mono.just(new FacadePointBO()));

        assertThrows(
                NotFoundException.class,
                () -> service.dashboard(TENANT, DEVICE, POINT, 24).block());
    }

    @Test
    void rawWalkFailureDegradesToPartialProfile() {
        when(tsdbStore.bucketedAggregate(any(), any(), any(), any(), isNull(), any()))
                .thenReturn(Mono.just(Map.of()));
        when(tsdbStore.history(any(), any(), isNull(), anyInt(), any()))
                .thenReturn(Mono.error(new RuntimeException("store unavailable")));

        PointValueDashboardVO vo = service.dashboard(TENANT, DEVICE, POINT, 24).block();

        assertNotNull(vo);
        assertEquals(0L, vo.getStats().getSampleCount());
    }
}
