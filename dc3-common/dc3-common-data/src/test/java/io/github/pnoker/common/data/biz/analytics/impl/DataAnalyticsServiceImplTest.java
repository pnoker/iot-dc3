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

package io.github.pnoker.common.data.biz.analytics.impl;

import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.CorrelationResult;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.DimensionCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataAnalyticsServiceImplTest {

    private static final long TENANT = 1L;
    private static final SeriesKey SERIES = new SeriesKey(TENANT, 10L, 20L);

    @Mock
    private TsdbStore tsdbStore;

    @Mock
    private PointValueLatestService pointValueLatestService;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private DriverFacade driverFacade;

    @Spy
    private PointValueSampleConverter converter = new PointValueSampleConverter();

    @InjectMocks
    private DataAnalyticsServiceImpl service;

    @BeforeEach
    void stubCapabilities() {
        lenient().when(tsdbStore.capabilities()).thenReturn(new TsdbStore.TsdbCapabilities(
                false, true, true, true, true,
                TsdbStore.RollupSupport.NONE, 5000, true,
                TsdbStore.OrderingGuarantee.PER_SERIES, TsdbStore.Precision.MICRO, true, true));
        lenient().when(deviceFacade.getById(TENANT, 10L)).thenReturn(device(10L, "boiler-1"));
        lenient().when(pointFacade.getById(TENANT, 20L)).thenReturn(point(20L, "temp"));
    }

    private static FacadeDeviceBO device(Long id, String name) {
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(id);
        device.setDeviceName(name);
        return device;
    }

    private static FacadePointBO point(Long id, String name) {
        FacadePointBO point = new FacadePointBO();
        point.setId(id);
        point.setPointName(name);
        return point;
    }

    private static PointValueSample sample(SeriesKey series, Instant time, double value) {
        return PointValueSample.simple(series, time, value);
    }

    @Test
    void tenantGuardRejectsMissingTenantOnEveryOperation() {
        AnalyticsModel.QueryLatestRequest request =
                new AnalyticsModel.QueryLatestRequest(List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)));
        assertThatThrownBy(() -> service.queryLatest(null, request)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.queryLatest(0L, request)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.computeStats(-1L, null)).isInstanceOf(ServiceException.class);
        verify(tsdbStore, never()).history(any(), any(), any(), anyInt(), any());
    }

    @Test
    void seriesBudgetCapsAtTwentySelectors() {
        List<AnalyticsModel.SeriesSelector> tooMany = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add(new AnalyticsModel.SeriesSelector(10L, 20L, null, null));
        }
        AnalyticsModel.QueryLatestRequest request = new AnalyticsModel.QueryLatestRequest(tooMany);

        assertThatThrownBy(() -> service.queryLatest(TENANT, request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("at most 20 series");
    }

    @Test
    void ambiguousDeviceNameSurfacesCandidates() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of(device(10L, "boiler"), device(11L, "boiler")));
        page.setTotal(2);
        when(deviceFacade.listByPage(any(FacadeDeviceQuery.class))).thenReturn(page);
        AnalyticsModel.QueryLatestRequest request = new AnalyticsModel.QueryLatestRequest(
                List.of(new AnalyticsModel.SeriesSelector(null, 20L, "boiler", null)));

        assertThatThrownBy(() -> service.queryLatest(TENANT, request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("10:boiler")
                .hasMessageContaining("11:boiler");
    }

    @Test
    void uniqueNameResolvesThroughTheFacade() {
        FacadePage<FacadeDeviceBO> devicePage = new FacadePage<>();
        devicePage.setRecords(List.of(device(10L, "boiler-1")));
        devicePage.setTotal(1);
        FacadePage<FacadePointBO> pointPage = new FacadePage<>();
        pointPage.setRecords(List.of(point(20L, "temp")));
        pointPage.setTotal(1);
        when(deviceFacade.listByPage(any(FacadeDeviceQuery.class))).thenReturn(devicePage);
        when(pointFacade.listByPage(any(FacadePointQuery.class))).thenReturn(pointPage);
        when(pointValueLatestService.listLatest(eq(TENANT), eq(10L), anyList())).thenReturn(List.of(
                PointValueBO.builder().tenantId(TENANT).deviceId(10L).pointId(20L)
                        .rawValue("42").calValue("42").numValue(42d).build()));

        AnalyticsModel.LatestValuesResponse response = service.queryLatest(TENANT,
                new AnalyticsModel.QueryLatestRequest(
                        List.of(new AnalyticsModel.SeriesSelector(null, null, "boiler-1", "temp"))));

        assertThat(response.values()).hasSize(1);
        assertThat(response.values().getFirst().hasValue()).isTrue();
        assertThat(response.values().getFirst().numericValue()).isEqualTo(42d);
        assertThat(response.conclusion()).contains("boiler-1/temp = 42");
    }

    @Test
    void computeStatsMathIsExactOnAFixture() {
        Instant base = Instant.parse("2026-08-21T10:00:00Z");
        List<PointValueSample> samples = List.of(
                sample(SERIES, base, 1), sample(SERIES, base.plusSeconds(10), 2),
                sample(SERIES, base.plusSeconds(20), 3), sample(SERIES, base.plusSeconds(30), 10));
        when(tsdbStore.history(eq(SeriesFilter.of(SERIES)), any(), isNull(), anyInt(), any()))
                .thenReturn(new CursorPage<>(samples, null));

        AnalyticsModel.StatsResponse response = service.computeStats(TENANT,
                new AnalyticsModel.ComputeStatsRequest(
                        List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)), null, List.of(0.5)));

        AnalyticsModel.StatItem stat = response.stats().get("boiler-1/temp");
        assertThat(stat.count()).isEqualTo(4);
        assertThat(stat.mean()).isEqualTo(4.0);
        assertThat(stat.min()).isEqualTo(1.0);
        assertThat(stat.max()).isEqualTo(10.0);
        // population std-dev of 1,2,3,10 = sqrt(12.5)
        assertThat(stat.stdDev()).isCloseTo(Math.sqrt(12.5), within(0.0001));
        assertThat(stat.percentiles()).containsEntry(0.5, 2.5);
    }

    @Test
    void thresholdReportMergesConsecutiveExceedances() {
        Instant base = Instant.parse("2026-08-21T10:00:00Z");
        List<PointValueSample> samples = List.of(
                sample(SERIES, base, 5), sample(SERIES, base.plusSeconds(10), 15),
                sample(SERIES, base.plusSeconds(20), 25), sample(SERIES, base.plusSeconds(30), 5),
                sample(SERIES, base.plusSeconds(40), 12));
        when(tsdbStore.history(eq(SeriesFilter.of(SERIES)), any(), isNull(), anyInt(), any()))
                .thenReturn(new CursorPage<>(samples, null));

        AnalyticsModel.ThresholdResponse response = service.thresholdReport(TENANT,
                new AnalyticsModel.ThresholdReportRequest(
                        List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)),
                        null, "GREATER", 10d));

        AnalyticsModel.ThresholdItem item = response.report().get("boiler-1/temp");
        assertThat(item.exceedCount()).isEqualTo(3);
        assertThat(item.peak()).isEqualTo(25d);
        // merged run 10:00:10 → 10:00:30 plus the trailing 12 at 10:00:40
        assertThat(item.intervals()).hasSize(2);
        assertThat(item.totalSeconds()).isEqualTo(20);
    }

    @Test
    void correlateFallsBackToBucketedPullsWhenStoreDeclaresNoSupport() {
        when(tsdbStore.capabilities()).thenReturn(new TsdbStore.TsdbCapabilities(
                false, true, true, true, true,
                TsdbStore.RollupSupport.NONE, 5000, true,
                TsdbStore.OrderingGuarantee.PER_SERIES, TsdbStore.Precision.MICRO, true, false));
        SeriesKey b = new SeriesKey(TENANT, 10L, 21L);
        Instant base = Instant.parse("2026-08-21T10:00:00Z");
        // perfectly correlated ramps
        lenient().when(tsdbStore.bucketedAggregate(eq(SeriesFilter.of(SERIES)), any(), any(), eq(Duration.ofMinutes(5)),
                isNull(), any())).thenReturn(Map.of(SERIES, List.of(
                new BucketAggregate(base, 1d, 1), new BucketAggregate(base.plusSeconds(300), 2d, 1),
                new BucketAggregate(base.plusSeconds(600), 3d, 1))));
        when(tsdbStore.bucketedAggregate(eq(SeriesFilter.of(b)), any(), any(), eq(Duration.ofMinutes(5)),
                isNull(), any())).thenReturn(Map.of(b, List.of(
                new BucketAggregate(base, 2d, 1), new BucketAggregate(base.plusSeconds(300), 4d, 1),
                new BucketAggregate(base.plusSeconds(600), 6d, 1))));

        AnalyticsModel.CorrelationResponse response = service.correlate(TENANT,
                new AnalyticsModel.CorrelateRequest(
                        new AnalyticsModel.SeriesSelector(10L, 20L, null, null),
                        new AnalyticsModel.SeriesSelector(10L, 21L, null, null), null, 300L));

        assertThat(response.method()).isEqualTo("FACADE");
        assertThat(response.pearson()).isEqualTo(1.0);
        assertThat(response.alignedBuckets()).isEqualTo(3);
        assertThat(response.degradation()).isNotNull();
        verify(tsdbStore, never()).correlation(any(), any(), any(), any(), any());
    }

    @Test
    void correlateUsesStorePrimitiveWhenDeclared() {
        when(tsdbStore.correlation(eq(SERIES), any(), any(), any(), any()))
                .thenReturn(new CorrelationResult(0.8, 42));

        AnalyticsModel.CorrelationResponse response = service.correlate(TENANT,
                new AnalyticsModel.CorrelateRequest(
                        new AnalyticsModel.SeriesSelector(10L, 20L, null, null),
                        new AnalyticsModel.SeriesSelector(10L, 20L, null, null), null, 60L));

        assertThat(response.method()).isEqualTo("STORE");
        assertThat(response.pearson()).isEqualTo(0.8);
        assertThat(response.alignedBuckets()).isEqualTo(42);
        assertThat(response.degradation()).isNull();
    }

    @Test
    void trendAnalysisDetectsRiseViaLeastSquares() {
        Instant base = Instant.parse("2026-08-21T10:00:00Z");
        when(tsdbStore.bucketedAggregate(eq(SeriesFilter.of(SERIES)), any(), any(), any(), isNull(), any()))
                .thenReturn(Map.of(SERIES, List.of(
                        new BucketAggregate(base, 1d, 1), new BucketAggregate(base.plusSeconds(60), 3d, 1),
                        new BucketAggregate(base.plusSeconds(120), 5d, 1))));

        AnalyticsModel.TrendResponse response = service.trendAnalysis(TENANT,
                new AnalyticsModel.TrendAnalysisRequest(
                        List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)), null, 3));

        AnalyticsModel.TrendItem trend = response.trends().get("boiler-1/temp");
        assertThat(trend.slopePerBucket()).isEqualTo(2.0);
        assertThat(trend.totalChangePct()).isEqualTo(400.0);
        assertThat(response.conclusion()).contains("rising");
    }

    @Test
    void rankEntitiesByActivityUsesCountByDimension() {
        when(tsdbStore.countByDimension(eq(TENANT), any(), eq(GroupDimension.DEVICE), eq(10), any()))
                .thenReturn(List.of(new DimensionCount(GroupDimension.DEVICE, 10L, 500)));

        AnalyticsModel.RankResponse response = service.rankEntities(TENANT,
                new AnalyticsModel.RankEntitiesRequest("device", "ACTIVITY", null, null));

        assertThat(response.ranked()).hasSize(1);
        assertThat(response.ranked().getFirst().entityId()).isEqualTo("10");
        assertThat(response.ranked().getFirst().label()).isEqualTo("boiler-1");
        assertThat(response.ranked().getFirst().count()).isEqualTo(500);
    }

    @Test
    void qualityReportFlagsSilenceAndSamplesQualityCodes() {
        Instant now = Instant.now();
        when(tsdbStore.lastSeenPerSeries(eq(TENANT), any(), any())).thenReturn(List.of(
                new SeriesLastSeen(SERIES, now.minusSeconds(60)),
                new SeriesLastSeen(new SeriesKey(TENANT, 10L, 21L), now.minus(Duration.ofHours(2)))));
        List<PointValueSample> probe = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            probe.add(sample(SERIES, now.minusSeconds(i), i));
        }
        when(tsdbStore.history(eq(SeriesFilter.tenantWide(TENANT)), any(), isNull(), anyInt(), any()))
                .thenReturn(new CursorPage<>(probe, null));
        FacadePage<FacadePointBO> pointPage = new FacadePage<>();
        pointPage.setRecords(List.of());
        pointPage.setTotal(10);
        when(pointFacade.listByPage(any(FacadePointQuery.class))).thenReturn(pointPage);

        AnalyticsModel.QualityResponse response = service.qualityReport(TENANT,
                new AnalyticsModel.QualityReportRequest(null, 30));

        assertThat(response.seriesWithSamples()).isEqualTo(2);
        assertThat(response.totalPoints()).isEqualTo(10);
        assertThat(response.silentSeries()).hasSize(1);
        assertThat(response.qualityDistribution()).containsEntry("0", 100L);
        assertThat(response.degradation()).isNull();
    }

    @Test
    void windowResolutionCapsSpanAndDefaultsTo24h() {
        assertThatThrownBy(() -> service.computeStats(TENANT,
                new AnalyticsModel.ComputeStatsRequest(
                        List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)),
                        new AnalyticsModel.TimeRange(Instant.now().minus(Duration.ofDays(365)).toString(),
                                Instant.now().toString(), null), null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("span");

        when(tsdbStore.history(eq(SeriesFilter.of(SERIES)), any(), isNull(), anyInt(), any()))
                .thenReturn(new CursorPage<>(List.of(), null));
        service.computeStats(TENANT, new AnalyticsModel.ComputeStatsRequest(
                List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)), null, null));
        verify(tsdbStore).history(any(), any(TimeWindow.class), isNull(), anyInt(), any());
    }
}
