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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.PointValueVolumeBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueServiceImplTest {

    private static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private PointFacade pointFacade;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private PointValueIngestService pointValueIngestService;

    @Mock
    private PointValueLatestService pointValueLatestService;

    @Spy
    private PointValueSampleConverter converter = new PointValueSampleConverter();

    @Mock
    private TsdbStore tsdbStore;

    @InjectMocks
    private PointValueServiceImpl service;

    private PointValueBO pv;

    private static FacadeDeviceBO stubDevice(Long id, Long tenantId, Long profileId) {
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(id);
        device.setTenantId(tenantId);
        device.setProfileId(profileId);
        return device;
    }

    private static FacadePointBO stubPoint(Long id, Long tenantId, Long profileId) {
        FacadePointBO point = new FacadePointBO();
        point.setId(id);
        point.setTenantId(tenantId);
        point.setProfileId(profileId);
        return point;
    }

    private static FacadePage<FacadePointBO> pageOf(List<FacadePointBO> records) {
        FacadePage<FacadePointBO> page = new FacadePage<>();
        page.setRecords(records);
        page.setTotal(records.size());
        page.setCurrent(1);
        page.setSize(records.size());
        return page;
    }

    private static Instant at(String wallClock) {
        return LocalDateTime.parse(wallClock).atZone(PLATFORM_ZONE).toInstant();
    }

    @BeforeEach
    void setUp() {
        pv = PointValueBO.builder()
                .tenantId(1L)
                .deviceId(10L)
                .pointId(20L)
                .messageId("m-1")
                .build();
    }

    @Test
    void singleSaveIgnoresNullPayload() {
        assertThatNoException().isThrownBy(() -> service.save((PointValueBO) null));
        verify(pointValueIngestService, never()).saveValue(any());
    }

    @Test
    void batchSaveIgnoresNullAndEmptyList() {
        assertThatNoException().isThrownBy(() -> service.save((List<PointValueBO>) null));
        assertThatNoException().isThrownBy(() -> service.save(List.of()));
        verify(pointValueIngestService, never()).saveValues(any());
    }

    @Test
    void singleSaveStampsTimestampsPersistsAndTriggersAlarm() {
        when(pointValueIngestService.saveValue(pv)).thenReturn(true);

        service.save(pv);

        assertThat(pv.getCreateTime()).isNotNull();
        assertThat(pv.getOperateTime()).isNotNull();
        verify(pointValueIngestService).saveValue(pv);
        verify(alarmRuleTriggerService).processPointValue(pv);
    }

    @Test
    void singleSaveSkipsAlarmWhenIngestRejects() {
        when(pointValueIngestService.saveValue(pv)).thenReturn(false);

        service.save(pv);

        verify(alarmRuleTriggerService, never()).processPointValue(any());
    }

    @Test
    void batchSaveTriggersAlarmOnlyForAcceptedValues() {
        PointValueBO accepted = PointValueBO.builder().tenantId(1L).deviceId(10L).pointId(21L).messageId("m-2").build();
        when(pointValueIngestService.saveValues(any())).thenReturn(List.of(accepted));

        service.save(List.of(pv, accepted));

        verify(alarmRuleTriggerService).processPointValues(List.of(accepted));
        verify(alarmRuleTriggerService, never()).processPointValue(any());
    }

    @Test
    void batchAlarmFailureIsSwallowedAfterPersistence() {
        PointValueBO accepted = PointValueBO.builder().tenantId(1L).deviceId(10L).pointId(21L).messageId("m-2").build();
        when(pointValueIngestService.saveValues(any())).thenReturn(List.of(accepted));
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(alarmRuleTriggerService).processPointValues(any());

        assertThatNoException().isThrownBy(() -> service.save(List.of(accepted)));
        verify(pointValueIngestService).saveValues(any());
    }

    @Test
    void historyRequiresCompleteSeriesKey() {
        assertThat(service.history(1L, null, 20L, 10)).isEmpty();
        assertThat(service.history(null, 10L, 20L, 10)).isEmpty();
        verifyNoTsdbReads();
    }

    @Test
    void historyValidatesScopeBeforeReading() {
        when(deviceFacade.getById(1L, 10L)).thenReturn(null);

        assertThatThrownBy(() -> service.history(1L, 10L, 20L, 10))
                .isInstanceOf(NotFoundException.class);
        verifyNoTsdbReads();
    }

    @Test
    void historyClampsCountAndMapsSamplesToBO() {
        FacadeDeviceBO device = stubDevice(10L, 1L, 100L);
        FacadePointBO point = stubPoint(20L, 1L, 100L);
        lenient().when(deviceFacade.getById(1L, 10L)).thenReturn(device);
        lenient().when(pointFacade.getById(1L, 20L)).thenReturn(point);
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        PointValueSample sample = new PointValueSample(series, at("2026-08-20T10:00:00"),
                at("2026-08-20T10:00:01"), "raw", "2.5", 2.5d, 0, "m-1", 1, "node", 1L, 1L, 5L);
        when(tsdbStore.last(eq(SeriesFilter.of(series)), eq(100), any())).thenReturn(Map.of(series, List.of(sample)));

        List<PointValueBO> result = service.history(1L, 10L, 20L, 0);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCalValue()).isEqualTo("2.5");
        assertThat(result.getFirst().getCreateTime()).isEqualTo(LocalDateTime.parse("2026-08-20T10:00:00"));
    }

    @Test
    void latestKeepsPointOrderAndFillsMissingWithPlaceholder() {
        lenient().when(deviceFacade.getById(1L, 10L)).thenReturn(stubDevice(10L, 1L, 100L));
        FacadePointBO known = stubPoint(20L, 1L, 100L);
        FacadePointBO missing = stubPoint(21L, 1L, 100L);
        when(pointFacade.listByPage(any())).thenReturn(pageOf(List.of(known, missing)));
        PointValueBO existing = PointValueBO.builder().tenantId(1L).deviceId(10L).pointId(20L).build();
        when(pointValueLatestService.listLatest(eq(1L), eq(10L), any())).thenReturn(List.of(existing));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).deviceId(10L).page(new Pages()).build();
        Page<PointValueBO> page = service.latest(query);

        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.getRecords().getFirst().getHasLatestValue()).isTrue();
        assertThat(page.getRecords().get(1).getHasLatestValue()).isFalse();
    }

    @Test
    void pageUnfilteredUsesTenantWideSeries() {
        when(tsdbStore.count(any(), any(), any())).thenReturn(2L);
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        PointValueSample newest = PointValueSample.simple(series, at("2026-08-20T10:00:01"), 2);
        PointValueSample older = PointValueSample.simple(series, at("2026-08-20T10:00:00"), 1);
        when(tsdbStore.history(any(), any(), any(), anyInt(), any()))
                .thenReturn(new CursorPage<>(List.of(newest, older), null));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).page(new Pages()).build();
        Page<PointValueBO> page = service.page(query);

        ArgumentCaptor<SeriesFilter> filter = ArgumentCaptor.forClass(SeriesFilter.class);
        verify(tsdbStore).count(filter.capture(), any(), any());
        assertThat(filter.getValue().tenantWide()).isTrue();
        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.getRecords().getFirst().getMessageId()).isEqualTo(newest.messageId());
    }

    @Test
    void pageSlicesTheSecondWindow() {
        when(tsdbStore.count(any(), any(), any())).thenReturn(30L);
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        List<PointValueSample> samples = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            samples.add(PointValueSample.simple(series, at("2026-08-20T10:00:00").plusSeconds(i), i));
        }
        java.util.Collections.reverse(samples);
        when(tsdbStore.history(any(), any(), any(), eq(20), any()))
                .thenReturn(new CursorPage<>(samples, null));

        Pages pages = new Pages();
        pages.setCurrent(2);
        pages.setSize(10);
        PointValueQuery query = PointValueQuery.builder().tenantId(1L).page(pages).build();
        Page<PointValueBO> page = service.page(query);

        assertThat(page.getRecords()).hasSize(10);
        // offset 10 over the descending list — eleventh newest
        assertThat(page.getRecords().getFirst().getNumValue()).isEqualTo(19d);
    }

    @Test
    void pageResolvesSeriesThroughProfileBindingsWhenOnlyPointFiltered() {
        when(pointFacade.listByPage(any())).thenReturn(pageOf(List.of(stubPoint(20L, 1L, 100L))));
        when(deviceFacade.listByProfileId(1L, 100L)).thenReturn(List.of(stubDevice(10L, 1L, 100L)));
        when(tsdbStore.count(any(), any(), any())).thenReturn(0L);
        when(tsdbStore.history(any(), any(), any(), anyInt(), any())).thenReturn(new CursorPage<>(List.of(), null));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).pointName("temp").page(new Pages()).build();
        service.page(query);

        ArgumentCaptor<SeriesFilter> filter = ArgumentCaptor.forClass(SeriesFilter.class);
        verify(tsdbStore).history(filter.capture(), any(), any(), anyInt(), any());
        assertThat(filter.getValue().series()).containsExactly(new SeriesKey(1L, 10L, 20L));
    }

    @Test
    void pageWithMissingScopedPointStillValidatesScopeFirst() {
        when(pointFacade.getById(1L, 20L)).thenReturn(null);
        PointValueQuery query = PointValueQuery.builder().tenantId(1L).pointId(20L).page(new Pages()).build();

        // Scope validation runs before any series resolution — a missing point
        // is a client error, not an empty page (pre-existing contract).
        assertThatThrownBy(() -> service.page(query))
                .isInstanceOf(NotFoundException.class);
        verifyNoTsdbReads();
    }

    @Test
    void seriesVolumesMapsPortRows() {
        when(tsdbStore.seriesCounts(eq(1L), any(), any()))
                .thenReturn(List.of(new SeriesCount(new SeriesKey(1L, 10L, 20L), 7L)));

        List<PointValueVolumeBO> volumes = service.seriesVolumes(1L, Instant.EPOCH);

        assertThat(volumes).hasSize(1);
        assertThat(volumes.getFirst().deviceId()).isEqualTo(10L);
        assertThat(volumes.getFirst().count()).isEqualTo(7L);
    }

    private void verifyNoTsdbReads() {
        verify(tsdbStore, never()).last(any(), anyInt(), any());
        verify(tsdbStore, never()).history(any(), any(), any(), anyInt(), any());
        verify(tsdbStore, never()).count(any(), any(), any());
    }

}
