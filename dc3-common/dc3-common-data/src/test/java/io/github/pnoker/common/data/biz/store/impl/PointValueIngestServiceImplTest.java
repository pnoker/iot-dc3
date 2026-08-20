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

import io.github.pnoker.common.data.biz.store.IngestIdempotencyWindow;
import io.github.pnoker.common.data.biz.store.PointValueSampleConverter;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.mapper.PointValueMapper;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueIngestServiceImplTest {

    @Mock
    private PointValueMapper pointValueMapper;

    @Mock
    private PointValueBuilder pointValueBuilder;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private TsdbStore tsdbStore;

    @Mock
    private IngestIdempotencyWindow idempotencyWindow;

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
            return input.stream().map(bo -> new PointValueDO()).toList();
        });
        lenient().when(deviceFacade.getActiveOwner(1L, 10L))
                .thenReturn(new FacadeDeviceOwnerBO(5L, "node-a", 7L));
    }

    @Test
    void nullAndEmptyBatchesAreNoOps() {
        assertThat(service.saveValues(null)).isEmpty();
        assertThat(service.saveValues(List.of())).isEmpty();
        verify(tsdbStore, never()).append(anyList());
        verify(pointValueMapper, never()).upsertLatestBatch(anyList());
    }

    @Test
    void staleOwnerEnvelopeIsDroppedBeforeAnyWrite() {
        when(deviceFacade.getActiveOwner(1L, 10L)).thenReturn(new FacadeDeviceOwnerBO(5L, "node-b", 9L));

        List<PointValueBO> accepted = service.saveValues(List.of(value));

        assertThat(accepted).isEmpty();
        verify(tsdbStore, never()).append(anyList());
        verify(pointValueMapper, never()).upsertLatestBatch(anyList());
    }

    @Test
    void missingOwnerLeaseIsDropped() {
        when(deviceFacade.getActiveOwner(1L, 10L)).thenReturn(null);

        assertThat(service.saveValues(List.of(value))).isEmpty();
        verify(tsdbStore, never()).append(anyList());
    }

    @Test
    void incompleteSeriesKeyIsDropped() {
        PointValueBO broken = PointValueBO.builder()
                .tenantId(1L).deviceId(10L).messageId("m-broken").build();

        assertThat(service.saveValues(List.of(broken))).isEmpty();
        verify(tsdbStore, never()).append(anyList());
    }

    @Test
    void messageIdWithoutWindowEntryIsPersistedAndMarkedAfterwards() {
        when(idempotencyWindow.seen("m-1")).thenReturn(false);

        List<PointValueBO> accepted = service.saveValues(List.of(value));

        assertThat(accepted).containsExactly(value);
        InOrder order = inOrder(tsdbStore, pointValueMapper, idempotencyWindow);
        order.verify(tsdbStore).append(anyList());
        order.verify(pointValueMapper).upsertLatestBatch(anyList());
        order.verify(idempotencyWindow).mark("m-1");
    }

    @Test
    void seenMessageIdIsSkippedWithoutTouchingTheStore() {
        when(idempotencyWindow.seen("m-1")).thenReturn(true);

        assertThat(service.saveValues(List.of(value))).isEmpty();
        verify(tsdbStore, never()).append(anyList());
        verify(pointValueMapper, never()).upsertLatestBatch(anyList());
    }

    @Test
    void duplicateMessageIdWithinBatchKeepsOnlyTheFirst() {
        PointValueBO duplicate = value("m-1", 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:01"));
        when(idempotencyWindow.seen("m-1")).thenReturn(false);

        List<PointValueBO> accepted = service.saveValues(List.of(value, duplicate));

        assertThat(accepted).containsExactly(value);
        ArgumentCaptor<List<PointValueSample>> samples = ArgumentCaptor.forClass(List.class);
        verify(tsdbStore).append(samples.capture());
        assertThat(samples.getValue()).hasSize(1);
    }

    @Test
    void nullMessageIdIsDroppedWithTheRestPersisted() {
        PointValueBO noMessageId = value(null, 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        when(idempotencyWindow.seen("m-1")).thenReturn(false);

        List<PointValueBO> accepted = service.saveValues(List.of(noMessageId, value));

        assertThat(accepted).containsExactly(value);
        verify(tsdbStore).append(anyList());
    }

    @Test
    void upsertReceivesIngestOrderedRowsWhileAcceptedKeepsInputOrder() {
        // input order deliberately inverted relative to INGEST_ORDER (point asc)
        PointValueBO laterPoint = value("m-2", 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        when(idempotencyWindow.seen(any())).thenReturn(false);

        List<PointValueBO> accepted = service.saveValues(List.of(laterPoint, value));

        assertThat(accepted).containsExactly(laterPoint, value);
        ArgumentCaptor<List<PointValueSample>> appended = ArgumentCaptor.forClass(List.class);
        verify(tsdbStore).append(appended.capture());
        assertThat(appended.getValue().getFirst().series().pointId()).isEqualTo(20L);
        assertThat(appended.getValue().get(1).series().pointId()).isEqualTo(21L);
    }

    @Test
    void leaseOwnerIsResolvedOncePerDistinctDevice() {
        PointValueBO otherPointSameDevice = value("m-2", 21L, 7L, LocalDateTime.parse("2026-08-20T10:00:00"));
        when(idempotencyWindow.seen(any())).thenReturn(false);

        service.saveValues(List.of(value, otherPointSameDevice));

        verify(deviceFacade).getActiveOwner(1L, 10L);
    }

    @Test
    void storeFailurePropagatesAndNothingIsMarked() {
        when(idempotencyWindow.seen("m-1")).thenReturn(false);
        when(tsdbStore.append(anyList())).thenThrow(new IllegalStateException("store down"));

        assertThatThrownBy(() -> service.saveValues(List.of(value)))
                .isInstanceOf(IllegalStateException.class);
        verify(pointValueMapper, never()).upsertLatestBatch(anyList());
        verify(idempotencyWindow, never()).mark(any());
    }

}
