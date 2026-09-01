package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.entity.bo.dashboard.AlertItemRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertCountersRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.HourCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceStatsRow;
import io.github.pnoker.common.data.entity.vo.dashboard.AlertBulkConfirmVO;
import io.github.pnoker.common.data.repository.ReactiveAlertAnalyticsStore;
import io.github.pnoker.common.data.repository.ReactiveAlertStore;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAlertReactiveTest {

    @Mock
    private ReactiveAlertStore alertStore;

    @Mock
    private ReactiveAlertAnalyticsStore alertAnalyticsStore;


    @Mock
    private PointValueLatestService pointValueLatestService;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private PointFacade pointFacade;

    @Mock
    private DriverFacade driverFacade;

    @InjectMocks
    private DashboardServiceImpl service;

    @Test
    void latestStreamUsesReactiveProjectionAndBulkMetadataLookups() {
        PointValueBO value = PointValueBO.builder().deviceId(7L).pointId(8L).driverId(9L)
                .rawValue("41").calValue("41").numValue(41d).build();
        FacadeDeviceBO device = new FacadeDeviceBO(); device.setId(7L); device.setDeviceName("device");
        FacadePointBO point = new FacadePointBO(); point.setId(8L); point.setPointName("point");
        FacadeDriverBO driver = new FacadeDriverBO(); driver.setId(9L); driver.setDriverName("driver");
        when(pointValueLatestService.listLatestStream(3L, 20)).thenReturn(Flux.just(value));
        when(deviceFacade.listByIdsReactive(eq(3L), any())).thenReturn(Flux.just(device));
        when(pointFacade.listByIdsReactive(eq(3L), any())).thenReturn(Flux.just(point));
        when(driverFacade.listByIdsReactive(eq(3L), any())).thenReturn(Flux.just(driver));

        var result = service.latestStream(3L, 20).block();

        assertEquals("device", result.getFirst().getDeviceName());
        assertEquals("point", result.getFirst().getPointName());
        assertEquals("driver", result.getFirst().getDriverName());
    }

    @Test
    void listUsesOffsetPageAndMapsRows() {
        AlertItemRow row = new AlertItemRow();
        row.setId(42L);
        row.setSource("device");
        row.setSourceId(7L);
        row.setPointId(8L);
        row.setAlarmTypeFlag(1);
        row.setConfirmFlag(0);
        row.setMessage("offline");
        PageRequest request = new PageRequest(20, 10);
        when(alertStore.list(eq(9L), eq("device"), eq(1), eq(0), any(), eq(request)))
                .thenReturn(Mono.just(OffsetPage.of(List.of(row), 20, 10, 21)));

        var result = service.alertPage(9L, "device", 1, 0, java.time.LocalDateTime.now(), request).block();

        assertEquals(20, result.offset());
        assertEquals(21, result.total());
        assertEquals("42", result.items().getFirst().getId());
        assertEquals("offline", result.items().getFirst().getMessage());
    }

    @Test
    void invalidSourceIsRejectedInsteadOfExpandingScope() {
        assertThrows(IllegalArgumentException.class,
                () -> service.alertPage(9L, "invalid", null, null, null, PageRequest.firstPage()));
    }

    @Test
    void bulkConfirmSkipsMalformedItemsAndCountsChangedRows() {
        when(alertStore.updateConfirm(9L, "device", 42L, (byte) 1)).thenReturn(Mono.just(true));
        when(alertStore.updateConfirm(9L, "driver", 43L, (byte) 1)).thenReturn(Mono.just(false));
        List<AlertBulkConfirmVO.Item> items = List.of(
                new AlertBulkConfirmVO.Item("device", "42"),
                new AlertBulkConfirmVO.Item("driver", "43"),
                new AlertBulkConfirmVO.Item("device", "not-a-number"),
                new AlertBulkConfirmVO.Item("unknown", "44"));

        Integer changed = service.bulkConfirmAlert(9L, items, true).block();

        assertEquals(1, changed);
        verify(alertStore).updateConfirm(9L, "device", 42L, (byte) 1);
        verify(alertStore).updateConfirm(9L, "driver", 43L, (byte) 1);
    }

    @Test
    void alertStatsUsesReactiveAggregatesAndPadsSparkline() {
        AlertCountersRow counters = new AlertCountersRow();
        counters.setTotal(12);
        counters.setUnconfirmed(4);
        BucketRow type = new BucketRow();
        type.setBucketKey("1");
        type.setCount(8);
        SourceStatsRow device = new SourceStatsRow();
        device.setSource("device");
        device.setTotal(9);
        device.setUnconfirmed(3);
        SourceStatsRow driver = new SourceStatsRow();
        driver.setSource("driver");
        driver.setTotal(3);
        driver.setUnconfirmed(1);
        HourCountRow hour = new HourCountRow();
        hour.setBucket(java.time.LocalDateTime.now().withMinute(0).withSecond(0).withNano(0));
        hour.setCount(2);

        when(alertAnalyticsStore.countAll(9L)).thenReturn(Mono.just(counters));
        when(alertAnalyticsStore.countByType(9L)).thenReturn(Flux.just(type));
        when(alertAnalyticsStore.countBySource(9L)).thenReturn(Flux.just(device, driver));
        when(alertAnalyticsStore.todayBySource(eq(9L), any())).thenReturn(Flux.empty());
        when(alertAnalyticsStore.hourlyCounts(eq(9L), any())).thenReturn(Flux.just(hour));

        var result = service.alertStats(9L).block();

        assertEquals(12, result.getTotal());
        assertEquals(4, result.getUnconfirmed());
        assertEquals(9, result.getDeviceAlerts());
        assertEquals(3, result.getDriverAlerts());
        assertEquals(1, result.getByType().size());
        assertEquals(24, result.getSparkline24h().size());
    }

    @Test
    void alertTrendReturnsZeroFilledCalendarDays() {
        AlertTrendRow row = new AlertTrendRow();
        String today = java.time.LocalDate.now().toString();
        row.setDate(today);
        row.setDeviceCount(5);
        row.setDriverCount(2);
        when(alertAnalyticsStore.dailyTrend(eq(9L), any())).thenReturn(Flux.just(row));

        var result = service.alertTrend(9L, 2).block();

        assertEquals(3, result.size());
        assertEquals(today, result.getLast().getDate());
        assertEquals(5, result.getLast().getDeviceCount());
        assertEquals(0, result.getFirst().getDeviceCount());
    }
}
