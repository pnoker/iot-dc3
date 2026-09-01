package io.github.pnoker.common.data.biz.analytics.impl;

import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.entity.vo.analytics.AnalyticsModel;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.tsdb.model.TsdbModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataAnalyticsServiceImplTest {
    private static final long TENANT = 1L;
    private static final TsdbModel.SeriesKey SERIES = new TsdbModel.SeriesKey(TENANT, 10L, 20L);
    @Mock private ReactiveTsdbStore tsdbStore;
    @Mock private PointValueLatestService latestService;
    @Mock private DeviceFacade deviceFacade;
    @Mock private PointFacade pointFacade;
    @Mock private DriverFacade driverFacade;
    @Spy private PointValueSampleConverter converter = new PointValueSampleConverter();
    private DataAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataAnalyticsServiceImpl(tsdbStore, latestService, converter, deviceFacade, pointFacade, driverFacade);
        FacadeDeviceBO device = new FacadeDeviceBO(); device.setId(10L); device.setDeviceName("boiler-1");
        FacadePointBO point = new FacadePointBO(); point.setId(20L); point.setPointName("temp");
        lenient().when(deviceFacade.getByIdReactive(TENANT, 10L)).thenReturn(Mono.just(device));
        lenient().when(pointFacade.getByIdReactive(TENANT, 20L)).thenReturn(Mono.just(point));
    }

    private static TsdbModel.PointValueSample sample(double value, long seconds) {
        return TsdbModel.PointValueSample.simple(SERIES, Instant.parse("2026-08-20T00:00:00Z").plusSeconds(seconds), value);
    }

    @Test
    void rawHistoryUsesReactiveStoreAndSeriesLabels() {
        TsdbModel.CursorPage<TsdbModel.PointValueSample> page = new TsdbModel.CursorPage<>(List.of(sample(1, 1), sample(2, 2)), null);
        when(tsdbStore.history(eq(TsdbModel.SeriesFilter.of(SERIES)), any(), isNull(), eq(10), any())).thenReturn(Mono.just(page));
        AnalyticsModel.HistoryResponse response = service.queryHistory(TENANT,
                new AnalyticsModel.QueryHistoryRequest(List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)), null, "RAW", 10)).block();
        assertThat(response.series()).containsKey("boiler-1/temp");
        assertThat(response.series().get("boiler-1/temp")).hasSize(2);
    }

    @Test
    void m4HistoryCombinesBucketAggregates() {
        TsdbModel.BucketAggregate bucket = new TsdbModel.BucketAggregate(Instant.parse("2026-08-20T00:00:00Z"), 4d, 3L);
        when(tsdbStore.bucketedAggregate(eq(TsdbModel.SeriesFilter.of(SERIES)), any(), any(), any(), isNull(), any()))
                .thenReturn(Mono.just(Map.of(SERIES, List.of(bucket))));
        AnalyticsModel.HistoryResponse response = service.queryHistory(TENANT,
                new AnalyticsModel.QueryHistoryRequest(List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)), null, "M4", 1)).block();
        assertThat(response.series().get("boiler-1/temp")).hasSize(1);
    }

    @Test
    void computeStatsIsReactiveAndCalculatesPercentiles() {
        when(tsdbStore.history(eq(TsdbModel.SeriesFilter.of(SERIES)), any(), isNull(), any(Integer.class), any()))
                .thenReturn(Mono.just(new TsdbModel.CursorPage<>(List.of(sample(1, 1), sample(3, 2), sample(5, 3)), null)));
        AnalyticsModel.StatsResponse response = service.computeStats(TENANT,
                new AnalyticsModel.ComputeStatsRequest(List.of(new AnalyticsModel.SeriesSelector(10L, 20L, null, null)), null, List.of(0.5))).block();
        assertThat(response.stats().get("boiler-1/temp").mean()).isEqualTo(3d);
        assertThat(response.stats().get("boiler-1/temp").percentiles().get(0.5)).isEqualTo(3d);
    }

    @Test
    void nameSelectorsResolveThroughReactiveFacade() {
        FacadeDeviceBO device = new FacadeDeviceBO(); device.setId(10L); device.setDeviceName("boiler-1");
        FacadePointBO point = new FacadePointBO(); point.setId(20L); point.setPointName("temp");
        when(deviceFacade.listReactive(any())).thenReturn(Mono.just(io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(List.of(device), 0, 50, 1)));
        when(pointFacade.listReactive(any())).thenReturn(Mono.just(io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(List.of(point), 0, 50, 1)));
        when(tsdbStore.history(any(), any(), isNull(), any(Integer.class), any())).thenReturn(Mono.just(new TsdbModel.CursorPage<>(List.of(), null)));
        AnalyticsModel.HistoryResponse response = service.queryHistory(TENANT,
                new AnalyticsModel.QueryHistoryRequest(List.of(new AnalyticsModel.SeriesSelector(null, null, "boiler-1", "temp")), null, "RAW", 10)).block();
        assertThat(response.series()).isEmpty();
    }
}
