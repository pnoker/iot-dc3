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
package io.github.pnoker.common.data.biz.store.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.repository.ReactivePointValueIngestOutbox;
import io.github.pnoker.common.data.repository.ReactivePointValueLatestStore;
import io.github.pnoker.common.data.repository.ReactiveTsdbStore;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PointValueIngestServiceImplTest {

    @Mock
    private ReactivePointValueLatestStore latestStore;

    @Mock
    private PointValueBuilder pointValueBuilder;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private ReactiveTsdbStore reactiveTsdbStore;

    @Mock
    private ReactivePointValueIngestOutbox ingestOutbox;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Spy
    private PointValueSampleConverter converter = new PointValueSampleConverter();

    @InjectMocks
    private PointValueIngestServiceImpl service;

    private PointValueBO value;

    private static PointValueBO value(String messageId, long pointId, long fencingToken, LocalDateTime createTime) {
        return PointValueBO.builder()
                .tenantId(1L)
                .deviceId(10L)
                .pointId(pointId)
                .messageId(messageId)
                .schemaVersion(1)
                .sequence(1L)
                .driverId(5L)
                .driverNode("node-a")
                .fencingToken(fencingToken)
                .rawValue("1")
                .calValue("1")
                .numValue(1d)
                .createTime(createTime)
                .operateTime(createTime)
                .build();
    }

    @BeforeEach
    void setUp() {
        value = value("m-1", 20L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        // The latest-projection DO conversion is pass-through plumbing — a
        // same-size DO list keeps the test off MapStruct internals.
        lenient().when(pointValueBuilder.buildDOListByBOList(anyList())).thenAnswer(invocation -> {
            List<PointValueBO> input = invocation.getArgument(0);
            return input.stream()
                    .map(bo -> {
                        PointValueDO row = new PointValueDO();
                        row.setTenantId(bo.getTenantId());
                        row.setMessageId(bo.getMessageId());
                        row.setDeviceId(bo.getDeviceId());
                        row.setPointId(bo.getPointId());
                        row.setCreateTime(bo.getCreateTime());
                        return row;
                    })
                    .toList();
        });
        lenient().when(pointValueBuilder.buildDOByBO(any())).thenAnswer(invocation -> {
            PointValueBO bo = invocation.getArgument(0);
            PointValueDO row = new PointValueDO();
            row.setTenantId(bo.getTenantId());
            row.setMessageId(bo.getMessageId());
            return row;
        });
        lenient()
                .when(deviceFacade.getActiveOwnerReactive(1L, 10L))
                .thenReturn(Mono.just(new FacadeDeviceOwnerBO(5L, "node-a", 7L)));
        lenient().when(reactiveTsdbStore.append(anyList())).thenReturn(Mono.just(1));
        lenient().when(latestStore.upsertBatch(anyList())).thenReturn(Mono.just(1));
        lenient()
                .when(ingestOutbox.enqueue(anyList(), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        lenient().when(ingestOutbox.findPersisted(anyList())).thenReturn(reactor.core.publisher.Flux.empty());
        lenient().when(ingestOutbox.markPersisted(any(), any())).thenReturn(Mono.just(1));
        lenient().when(ingestOutbox.markProcessed(any())).thenReturn(Mono.just(1));
        lenient().when(ingestOutbox.markFailed(any(), any(), any())).thenReturn(Mono.just(1));
        lenient().when(alarmRuleTriggerService.processPointValue(any())).thenReturn(Mono.empty());
    }

    @Test
    void nullAndEmptyBatchesAreNoOps() {
        assertThat(service.saveValues(null).block()).isEmpty();
        assertThat(service.saveValues(List.of()).block()).isEmpty();
        verify(reactiveTsdbStore, never()).append(anyList());
        verify(latestStore, never()).upsertBatch(anyList());
    }

    @Test
    void staleOwnerEnvelopeIsDroppedBeforeAnyWrite() {
        when(deviceFacade.getActiveOwnerReactive(1L, 10L))
                .thenReturn(Mono.just(new FacadeDeviceOwnerBO(5L, "node-b", 9L)));

        List<PointValueBO> accepted = service.saveValues(List.of(value)).block();

        assertThat(accepted).isEmpty();
        verify(reactiveTsdbStore, never()).append(anyList());
        verify(latestStore, never()).upsertBatch(anyList());
    }

    @Test
    void missingOwnerLeaseIsDropped() {
        when(deviceFacade.getActiveOwnerReactive(1L, 10L)).thenReturn(Mono.empty());

        assertThat(service.saveValues(List.of(value)).block()).isEmpty();
        verify(reactiveTsdbStore, never()).append(anyList());
    }

    @Test
    void incompleteSeriesKeyIsDropped() {
        PointValueBO broken = PointValueBO.builder()
                .tenantId(1L)
                .deviceId(10L)
                .messageId("m-broken")
                .build();

        assertThat(service.saveValues(List.of(broken)).block()).isEmpty();
        verify(reactiveTsdbStore, never()).append(anyList());
    }

    @Test
    void messageIdIsMarkedOnlyAfterDownstreamCompletion() {
        List<PointValueBO> accepted = service.saveValues(List.of(value)).block();

        assertThat(accepted).containsExactly(value);
        InOrder order = inOrder(reactiveTsdbStore, latestStore);
        order.verify(reactiveTsdbStore).append(anyList());
        order.verify(latestStore).upsertBatch(anyList());
        service.markProcessed(accepted).block();
        verify(ingestOutbox).markProcessed(any());
    }

    @Test
    void durableReceiptIsEnqueuedBeforeTsdbWrite() {
        service.saveValues(List.of(value)).block();

        InOrder order = inOrder(ingestOutbox, reactiveTsdbStore);
        order.verify(ingestOutbox).enqueue(anyList(), any());
        order.verify(reactiveTsdbStore).append(anyList());
    }

    @Test
    void duplicateDurableReceiptIsNotWrittenAgain() {
        when(ingestOutbox.enqueue(anyList(), any())).thenReturn(Mono.just(List.of()));

        assertThat(service.saveValues(List.of(value)).block()).isEmpty();
        verify(reactiveTsdbStore, never()).append(anyList());
        verify(latestStore, never()).upsertBatch(anyList());
    }

    @Test
    void replayFailureMovesReceiptToRetryState() {
        PointValueDO row = new PointValueDO();
        row.setTenantId(1L);
        row.setMessageId("m-replay");
        when(ingestOutbox.claim(any(), any(Integer.class))).thenReturn(reactor.core.publisher.Flux.just(row));
        when(pointValueBuilder.buildBOByDO(row))
                .thenReturn(value("m-replay", 20L, 7L, LocalDateTime.parse("2026-08-20T10:00:00")));
        when(reactiveTsdbStore.append(anyList())).thenReturn(Mono.error(new IllegalStateException("tsdb down")));
        assertThat(service.replayPending().block()).isZero();
        verify(ingestOutbox).markFailed(eq(row), any(), eq("tsdb down"));
    }

    @Test
    void replayRunsAlarmPipelineAndMarksReceiptProcessed() {
        PointValueDO row = new PointValueDO();
        row.setTenantId(1L);
        row.setMessageId("m-replay-ok");
        PointValueBO replayValue = value("m-replay-ok", 20L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        when(ingestOutbox.claim(any(), any(Integer.class))).thenReturn(reactor.core.publisher.Flux.just(row));
        when(pointValueBuilder.buildBOByDO(row)).thenReturn(replayValue);
        when(alarmRuleTriggerService.processPointValue(replayValue)).thenReturn(Mono.empty());

        assertThat(service.replayPending().block()).isEqualTo(1);

        verify(alarmRuleTriggerService).processPointValue(replayValue);
        InOrder order = inOrder(ingestOutbox, alarmRuleTriggerService);
        order.verify(alarmRuleTriggerService).processPointValue(replayValue);
        order.verify(ingestOutbox).markPersisted(eq(row), any());
        order.verify(ingestOutbox).markProcessed(row);
    }

    @Test
    void replayAlarmFailureRequeuesAndDoesNotMarkProcessed() {
        PointValueDO row = new PointValueDO();
        row.setTenantId(1L);
        row.setMessageId("m-replay-alarm-fail");
        PointValueBO replayValue = value("m-replay-alarm-fail", 20L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        when(ingestOutbox.claim(any(), any(Integer.class))).thenReturn(reactor.core.publisher.Flux.just(row));
        when(pointValueBuilder.buildBOByDO(row)).thenReturn(replayValue);
        when(alarmRuleTriggerService.processPointValue(replayValue))
                .thenReturn(Mono.error(new IllegalStateException("alarm down")));

        assertThat(service.replayPending().block()).isZero();

        verify(ingestOutbox).markFailed(eq(row), any(), eq("alarm down"));
        verify(ingestOutbox, never()).markProcessed(row);
    }

    @Test
    void replayCompletionFailureRequeuesReceipt() {
        PointValueDO row = new PointValueDO();
        row.setTenantId(1L);
        row.setMessageId("m-replay-complete-fail");
        PointValueBO replayValue = value("m-replay-complete-fail", 20L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        when(ingestOutbox.claim(any(), any(Integer.class))).thenReturn(reactor.core.publisher.Flux.just(row));
        when(pointValueBuilder.buildBOByDO(row)).thenReturn(replayValue);
        when(ingestOutbox.markProcessed(row)).thenReturn(Mono.just(0));

        assertThat(service.replayPending().block()).isZero();

        verify(ingestOutbox).markFailed(eq(row), any(), any());
    }

    @Test
    void duplicateMessageIdWithinBatchKeepsOnlyTheFirst() {
        PointValueBO duplicate = value("m-1", 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:01"));
        List<PointValueBO> accepted =
                service.saveValues(List.of(value, duplicate)).block();

        assertThat(accepted).containsExactly(value);
        ArgumentCaptor<List<PointValueSample>> samples = ArgumentCaptor.forClass(List.class);
        verify(reactiveTsdbStore).append(samples.capture());
        assertThat(samples.getValue()).hasSize(1);
    }

    @Test
    void nullMessageIdIsDroppedWithTheRestPersisted() {
        PointValueBO noMessageId = value(null, 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        List<PointValueBO> accepted =
                service.saveValues(List.of(noMessageId, value)).block();

        assertThat(accepted).containsExactly(value);
        verify(reactiveTsdbStore).append(anyList());
    }

    @Test
    void upsertReceivesIngestOrderedRowsWhileAcceptedKeepsInputOrder() {
        // input order deliberately inverted relative to INGEST_ORDER (point asc)
        PointValueBO laterPoint = value("m-2", 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        List<PointValueBO> accepted =
                service.saveValues(List.of(laterPoint, value)).block();

        assertThat(accepted).containsExactly(laterPoint, value);
        ArgumentCaptor<List<PointValueSample>> appended = ArgumentCaptor.forClass(List.class);
        verify(reactiveTsdbStore).append(appended.capture());
        assertThat(appended.getValue().getFirst().series().pointId()).isEqualTo(20L);
        assertThat(appended.getValue().get(1).series().pointId()).isEqualTo(21L);
    }

    @Test
    void leaseOwnerIsResolvedOncePerDistinctDevice() {
        PointValueBO otherPointSameDevice = value("m-2", 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        service.saveValues(List.of(value, otherPointSameDevice)).block();

        verify(deviceFacade).getActiveOwnerReactive(1L, 10L);
    }

    @Test
    void storeFailurePropagatesAndNothingIsMarked() {
        when(reactiveTsdbStore.append(anyList())).thenReturn(Mono.error(new IllegalStateException("store down")));

        assertThatThrownBy(() -> service.saveValues(List.of(value)).block()).isInstanceOf(IllegalStateException.class);
        verify(latestStore, never()).upsertBatch(anyList());
        verify(ingestOutbox, never()).markPersisted(any(), any());
    }
}
