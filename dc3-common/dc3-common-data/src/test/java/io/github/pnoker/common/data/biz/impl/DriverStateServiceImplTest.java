package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverStateServiceImplTest {
    @Mock DriverAlarmService alarmService;
    @Mock ReactiveEntityStateStore stateStore;
    @Mock ReactiveMessageSender sender;

    @Test
    void heartbeatUpsertsAndPublishes() {
        DriverStateServiceImpl service = new DriverStateServiceImpl(alarmService, stateStore, sender);
        when(alarmService.alarm(any())).thenReturn(Mono.empty());
        when(stateStore.upsert(any(), eq(100L), eq(EntityTypeEnum.DRIVER), eq(1L), eq(0L), any(byte.class),
                any(byte.class), any(), eq(45), any(byte.class), any())).thenReturn(Mono.just(lease((byte) 2, (byte) 1)));
        when(sender.sendConfirmed(any())).thenReturn(Mono.empty());
        service.heartbeat(event(1L, 100L, EntityStatusEnum.ONLINE.getCode())).block();
        verify(stateStore).upsert(any(), eq(100L), eq(EntityTypeEnum.DRIVER), eq(1L), eq(0L),
                eq((byte) EntityStatusEnum.ONLINE.getIndex()), eq((byte) EntityStatusEnum.OFFLINE.getIndex()),
                any(), eq(45), any(byte.class), any());
        verify(sender).sendConfirmed(any());
    }

    @Test
    void statusFlipTriggersAlarm() {
        DriverStateServiceImpl service = new DriverStateServiceImpl(alarmService, stateStore, sender);
        when(alarmService.alarm(any())).thenReturn(Mono.empty());
        when(stateStore.upsert(any(), any(), any(), any(), any(), any(byte.class), any(byte.class), any(), any(Integer.class), any(byte.class), any()))
                .thenReturn(Mono.just(lease((byte) 1, (byte) 2)));
        when(sender.sendConfirmed(any())).thenReturn(Mono.empty());
        service.heartbeat(event(1L, 100L, EntityStatusEnum.OFFLINE.getCode())).block();
        verify(alarmService).alarm(any());
    }

    @Test
    void statusFlipWaitsForAlarmCompletion() {
        DriverStateServiceImpl service = new DriverStateServiceImpl(alarmService, stateStore, sender);
        AtomicBoolean completed = new AtomicBoolean();
        when(stateStore.upsert(any(), any(), any(), any(), any(), any(byte.class), any(byte.class), any(), any(Integer.class), any(byte.class), any()))
                .thenReturn(Mono.just(lease((byte) 1, (byte) 2)));
        when(sender.sendConfirmed(any())).thenReturn(Mono.empty());
        when(alarmService.alarm(any())).thenReturn(Mono.defer(() -> {
            completed.set(true);
            return Mono.empty();
        }));

        service.heartbeat(event(1L, 100L, EntityStatusEnum.OFFLINE.getCode())).block();

        org.assertj.core.api.Assertions.assertThat(completed).isTrue();
    }

    @Test
    void invalidHeartbeatIsIgnored() {
        DriverStateServiceImpl service = new DriverStateServiceImpl(alarmService, stateStore, sender);
        service.heartbeat(null).block();
        verifyNoInteractions(stateStore, sender, alarmService);
    }

    private DriverStateDTO event(Long driver, Long tenant, String status) {
        DriverStateDTO value = new DriverStateDTO(); value.setDriverId(driver); value.setTenantId(tenant); value.setStatus(status); return value;
    }
    private ReactiveEntityStateStore.EntityStateLease lease(byte state, byte previous) {
        return new ReactiveEntityStateStore.EntityStateLease(1L, 100L, EntityTypeEnum.DRIVER, 1L, 0L, state,
                previous, 2L, Instant.now().plusSeconds(45), 45, Instant.now(), 0L, (byte) 0, "{}");
    }
}
