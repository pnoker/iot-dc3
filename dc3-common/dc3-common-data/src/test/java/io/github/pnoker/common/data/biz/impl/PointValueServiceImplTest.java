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

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.biz.store.PointValueIngestService;
import io.github.pnoker.common.data.biz.store.PointValueLatestService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.data.support.PointValueCursorCodec;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.PointValueVolumeBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private ReactiveTsdbStore reactiveTsdbStore;

    @Mock
    private PointValueCursorCodec pointValueCursorCodec;

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
        lenient().when(pointValueCursorCodec.encode(anyLong(), anyString(), any())).thenAnswer(invocation -> "cursor");
        lenient().when(pointValueCursorCodec.decode(anyString(), anyLong(), anyString()))
                .thenReturn(new io.github.pnoker.common.tsdb.model.TsdbModel.Cursor(at("2026-08-20T10:00:01"), "m-previous",
                        new SeriesKey(1L, 10L, 20L)));
    }

    @Test
    void singleSaveIgnoresNullPayload() {
        StepVerifier.create(service.save((PointValueBO) null)).verifyComplete();
        verify(pointValueIngestService, never()).saveValue(any());
    }

    @Test
    void batchSaveIgnoresNullAndEmptyList() {
        StepVerifier.create(service.save((List<PointValueBO>) null)).verifyComplete();
        StepVerifier.create(service.save(List.of())).verifyComplete();
        verify(pointValueIngestService, never()).saveValues(any());
    }

    @Test
    void singleSaveStampsTimestampsPersistsAndTriggersAlarm() {
        when(pointValueIngestService.saveValue(pv)).thenReturn(Mono.just(true));
        when(alarmRuleTriggerService.processPointValue(pv)).thenReturn(Mono.empty());

        StepVerifier.create(service.save(pv)).verifyComplete();

        assertThat(pv.getCreateTime()).isNotNull();
        assertThat(pv.getOperateTime()).isNotNull();
        verify(pointValueIngestService).saveValue(pv);
        verify(alarmRuleTriggerService).processPointValue(pv);
        verify(pointValueIngestService).markProcessed(List.of(pv));
    }

    @Test
    void singleSaveSkipsAlarmWhenIngestRejects() {
        when(pointValueIngestService.saveValue(pv)).thenReturn(Mono.just(false));

        StepVerifier.create(service.save(pv)).verifyComplete();

        verify(alarmRuleTriggerService, never()).processPointValue(any());
        verify(pointValueIngestService, never()).markProcessed(any());
    }

    @Test
    void batchSaveTriggersAlarmOnlyForAcceptedValues() {
        PointValueBO accepted = PointValueBO.builder().tenantId(1L).deviceId(10L).pointId(21L).messageId("m-2").build();
        when(pointValueIngestService.saveValues(any())).thenReturn(Mono.just(List.of(accepted)));
        when(alarmRuleTriggerService.processPointValues(List.of(accepted))).thenReturn(Mono.empty());

        StepVerifier.create(service.save(List.of(pv, accepted))).verifyComplete();

        verify(alarmRuleTriggerService).processPointValues(List.of(accepted));
        verify(alarmRuleTriggerService, never()).processPointValue(any());
        verify(pointValueIngestService).markProcessed(List.of(accepted));
    }

    @Test
    void batchAlarmFailurePropagatesWithoutMarkingProcessed() {
        PointValueBO accepted = PointValueBO.builder().tenantId(1L).deviceId(10L).pointId(21L).messageId("m-2").build();
        when(pointValueIngestService.saveValues(any())).thenReturn(Mono.just(List.of(accepted)));
        when(alarmRuleTriggerService.processPointValues(any()))
                .thenReturn(Mono.error(new IllegalStateException("boom")));

        StepVerifier.create(service.save(List.of(accepted)))
                .expectErrorMessage("boom")
                .verify();
        verify(pointValueIngestService).saveValues(any());
        verify(pointValueIngestService, never()).markProcessed(any());
    }

    @Test
    void historyRequiresCompleteSeriesKey() {
        assertThatThrownBy(() -> service.history(1L, null, 20L, null, 10).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId, deviceId and pointId must be positive");
        assertThatThrownBy(() -> service.history(null, 10L, 20L, null, 10).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId, deviceId and pointId must be positive");
        verifyNoTsdbReads();
    }

    @Test
    void historyValidatesScopeBeforeReading() {
        when(deviceFacade.getByIdReactive(1L, 10L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> service.history(1L, 10L, 20L, null, 10).block())
                .isInstanceOf(NotFoundException.class);
        verifyNoTsdbReads();
    }

    @Test
    void historyValidatesLimitAndMapsSamplesToBO() {
        FacadeDeviceBO device = stubDevice(10L, 1L, 100L);
        FacadePointBO point = stubPoint(20L, 1L, 100L);
        lenient().when(deviceFacade.getByIdReactive(1L, 10L)).thenReturn(Mono.just(device));
        lenient().when(pointFacade.getByIdReactive(1L, 20L)).thenReturn(Mono.just(point));
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        PointValueSample sample = new PointValueSample(series, at("2026-08-20T10:00:00"),
                at("2026-08-20T10:00:01"), "raw", "2.5", 2.5d, 0, "m-1", 1, "node", 1L, 1L, 5L);
        when(reactiveTsdbStore.history(eq(SeriesFilter.of(series)), any(), isNull(), eq(1), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(sample), null)));

        List<PointValueBO> result = service.history(1L, 10L, 20L, null, 1).block().items();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCalValue()).isEqualTo("2.5");
        assertThat(result.getFirst().getCreateTime()).isEqualTo(LocalDateTime.parse("2026-08-20T10:00:00"));
    }

    @Test
    void latestKeepsPointOrderAndFillsMissingWithPlaceholder() {
                FacadePointBO known = stubPoint(20L, 1L, 100L);
        FacadePointBO missing = stubPoint(21L, 1L, 100L);
        when(pointFacade.listReactive(any())).thenReturn(Mono.just(OffsetPage.of(List.of(known, missing), 0, 50, 2)));
        when(deviceFacade.getByIdReactive(eq(1L), eq(10L))).thenReturn(Mono.just(stubDevice(10L, 1L, 100L)));
        PointValueBO existing = PointValueBO.builder().tenantId(1L).deviceId(10L).pointId(20L).build();
        when(pointValueLatestService.listLatest(eq(1L), eq(10L), any())).thenReturn(reactor.core.publisher.Flux.just(existing));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).deviceId(10L).build();
        OffsetPage<PointValueBO> page = service.latest(query).block();

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().getFirst().getHasLatestValue()).isTrue();
        assertThat(page.items().get(1).getHasLatestValue()).isFalse();
    }

    @Test
    void pageUnfilteredUsesTenantWideSeries() {
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        PointValueSample newest = PointValueSample.simple(series, at("2026-08-20T10:00:01"), 2);
        PointValueSample older = PointValueSample.simple(series, at("2026-08-20T10:00:00"), 1);
        when(reactiveTsdbStore.history(any(), any(), any(), anyInt(), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(newest, older), null)));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).build();
        io.github.pnoker.db.r2dbc.core.page.CursorPage<PointValueBO> page = service.page(query).block();

        ArgumentCaptor<SeriesFilter> filter = ArgumentCaptor.forClass(SeriesFilter.class);
        verify(reactiveTsdbStore).history(filter.capture(), any(), isNull(), anyInt(), any());
        assertThat(filter.getValue().tenantWide()).isTrue();
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().getFirst().getMessageId()).isEqualTo(newest.messageId());
    }

    @Test
    void pageUsesSignedCursorForSubsequentWindow() {
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        PointValueSample sample = PointValueSample.simple(series, at("2026-08-20T10:00:00"), 19);
        when(reactiveTsdbStore.history(any(), any(), any(), eq(20), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(sample), null)));

        String fingerprint = "tenant=1;series=*;rangeKey=;rangeHours=0;from=;sort=create_time.desc,tenant_id.desc,device_id.desc,point_id.desc,message_id.desc";
        String cursor = pointValueCursorCodec.encode(1L, fingerprint,
                new io.github.pnoker.common.tsdb.model.TsdbModel.Cursor(at("2026-08-20T10:00:01"), "m-previous", series));
        PointValueQuery query = PointValueQuery.builder().tenantId(1L).limit(20).cursor(cursor).build();
        io.github.pnoker.db.r2dbc.core.page.CursorPage<PointValueBO> page = service.page(query).block();

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().getNumValue()).isEqualTo(19d);
        verify(reactiveTsdbStore).history(any(), any(), any(), eq(20), any());
    }

    @Test
    void pageSignsStableSeriesAndSnapshotWindow() {
        SeriesKey series = new SeriesKey(1L, 10L, 20L);
        PointValueSample sample = PointValueSample.simple(series, at("2026-08-20T10:00:00"), 19);
        io.github.pnoker.common.tsdb.model.TsdbModel.Cursor next =
                new io.github.pnoker.common.tsdb.model.TsdbModel.Cursor(
                        sample.deviceTime(), sample.messageId(), series);
        when(reactiveTsdbStore.history(any(), any(), isNull(), eq(20), any()))
                .thenReturn(Mono.just(new CursorPage<>(List.of(sample), next)));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).limit(20)
                .createTimeFrom(LocalDateTime.parse("2026-08-20T00:00:00"))
                .build();
        service.page(query).block();

        ArgumentCaptor<io.github.pnoker.common.tsdb.model.TsdbModel.Cursor> cursor =
                ArgumentCaptor.forClass(io.github.pnoker.common.tsdb.model.TsdbModel.Cursor.class);
        verify(pointValueCursorCodec).encode(eq(1L), anyString(), cursor.capture());
        assertThat(cursor.getValue().series()).isEqualTo(series);
        assertThat(cursor.getValue().windowFrom()).isEqualTo(at("2026-08-20T00:00:00"));
        assertThat(cursor.getValue().windowTo()).isAfter(cursor.getValue().windowFrom());
    }

    @Test
    void pageRejectsOffsetPagination() {
        PointValueQuery query = PointValueQuery.builder().tenantId(1L).offset(1).build();
        assertThatThrownBy(() -> service.page(query).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("offset is not supported for point-value history; use cursor");
        verifyNoTsdbReads();
    }

    @Test
    void pageResolvesSeriesThroughProfileBindingsWhenOnlyPointFiltered() {
        when(pointFacade.listReactive(any())).thenReturn(Mono.just(OffsetPage.of(List.of(stubPoint(20L, 1L, 100L)), 0, 200, 1)));
        when(deviceFacade.listByProfileIdReactive(1L, 100L)).thenReturn(reactor.core.publisher.Flux.just(stubDevice(10L, 1L, 100L)));
        when(reactiveTsdbStore.history(any(), any(), any(), anyInt(), any())).thenReturn(Mono.just(new CursorPage<>(List.of(), null)));

        PointValueQuery query = PointValueQuery.builder().tenantId(1L).pointName("temp").build();
        service.page(query).block();

        ArgumentCaptor<SeriesFilter> filter = ArgumentCaptor.forClass(SeriesFilter.class);
        verify(reactiveTsdbStore).history(filter.capture(), any(), any(), anyInt(), any());
        assertThat(filter.getValue().series()).containsExactly(new SeriesKey(1L, 10L, 20L));
    }

    @Test
    void pageWithMissingScopedPointStillValidatesScopeFirst() {
        when(pointFacade.getByIdReactive(1L, 20L)).thenReturn(Mono.empty());
        PointValueQuery query = PointValueQuery.builder().tenantId(1L).pointId(20L).build();

        // Scope validation runs before any series resolution — a missing point
        // is a client error, not an empty page (pre-existing contract).
        assertThatThrownBy(() -> service.page(query).block())
                .isInstanceOf(NotFoundException.class);
        verifyNoTsdbReads();
    }

    @Test
    void seriesVolumesMapsPortRows() {
        when(reactiveTsdbStore.seriesCounts(eq(1L), any(), any()))
                .thenReturn(reactor.core.publisher.Flux.just(new SeriesCount(new SeriesKey(1L, 10L, 20L), 7L)));

        List<PointValueVolumeBO> volumes = service.seriesVolumes(1L, Instant.EPOCH).block();

        assertThat(volumes).hasSize(1);
        assertThat(volumes.getFirst().deviceId()).isEqualTo(10L);
        assertThat(volumes.getFirst().count()).isEqualTo(7L);
    }

    private void verifyNoTsdbReads() {
        verify(reactiveTsdbStore, never()).history(any(), any(), any(), anyInt(), any());
        verify(reactiveTsdbStore, never()).count(any(), any(), any());
    }

}
