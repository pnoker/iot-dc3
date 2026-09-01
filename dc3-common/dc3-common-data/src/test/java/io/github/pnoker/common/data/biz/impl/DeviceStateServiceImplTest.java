package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.TimeoutSourceTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceStateServiceImplTest {
    @Mock DeviceAlarmService alarmService;
    @Mock ReactiveEntityStateStore stateStore;

    @Test
    void heartbeatUpsertsReactiveLease() {
        DeviceStateServiceImpl service = new DeviceStateServiceImpl(alarmService, stateStore);
        when(alarmService.alarm(any())).thenReturn(Mono.empty());
        when(stateStore.upsert(any(), eq(100L), eq(EntityTypeEnum.DEVICE), eq(10L), eq(7L), any(byte.class),
                any(byte.class), any(), eq(25), eq((byte) TimeoutSourceTypeEnum.DRIVER.getIndex()), any()))
                .thenReturn(Mono.just(lease(EntityStatusEnum.ONLINE.getIndex(), EntityStatusEnum.OFFLINE.getIndex())));
        service.heartbeat(event(10L, 7L, 100L, EntityStatusEnum.ONLINE.getCode(), 25)).block();
        verify(stateStore).upsert(any(), eq(100L), eq(EntityTypeEnum.DEVICE), eq(10L), eq(7L),
                eq((byte) EntityStatusEnum.ONLINE.getIndex()), eq((byte) EntityStatusEnum.OFFLINE.getIndex()),
                any(), eq(25), eq((byte) TimeoutSourceTypeEnum.DRIVER.getIndex()), any());
    }

    @Test
    void statusFlipTriggersAlarm() {
        DeviceStateServiceImpl service = new DeviceStateServiceImpl(alarmService, stateStore);
        when(alarmService.alarm(any())).thenReturn(Mono.empty());
        when(stateStore.upsert(any(), any(), any(), any(), any(), any(byte.class), any(byte.class), any(), any(Integer.class), any(byte.class), any()))
                .thenReturn(Mono.just(lease(EntityStatusEnum.OFFLINE.getIndex(), EntityStatusEnum.ONLINE.getIndex())));
        service.heartbeat(event(10L, 7L, 100L, EntityStatusEnum.OFFLINE.getCode(), 25)).block();
        verify(alarmService).alarm(any());
    }

    @Test
    void statusFlipWaitsForAlarmCompletion() {
        DeviceStateServiceImpl service = new DeviceStateServiceImpl(alarmService, stateStore);
        AtomicBoolean completed = new AtomicBoolean();
        when(stateStore.upsert(any(), any(), any(), any(), any(), any(byte.class), any(byte.class), any(), any(Integer.class), any(byte.class), any()))
                .thenReturn(Mono.just(lease(EntityStatusEnum.OFFLINE.getIndex(), EntityStatusEnum.ONLINE.getIndex())));
        when(alarmService.alarm(any())).thenReturn(Mono.defer(() -> {
            completed.set(true);
            return Mono.empty();
        }));

        service.heartbeat(event(10L, 7L, 100L, EntityStatusEnum.OFFLINE.getCode(), 25)).block();

        org.assertj.core.api.Assertions.assertThat(completed).isTrue();
    }

    @Test
    void invalidHeartbeatIsIgnored() {
        DeviceStateServiceImpl service = new DeviceStateServiceImpl(alarmService, stateStore);
        service.heartbeat(null).block();
        verifyNoInteractions(stateStore, alarmService);
    }

    private DeviceStateDTO event(Long device, Long driver, Long tenant, String status, int timeout) {
        DeviceStateDTO value = new DeviceStateDTO(); value.setDeviceId(device); value.setDriverId(driver);
        value.setTenantId(tenant); value.setStatus(status); value.setTimeout(timeout); value.setTimeoutUnit(TimeUnit.SECONDS); return value;
    }
    private ReactiveEntityStateStore.EntityStateLease lease(byte state, byte previous) {
        return new ReactiveEntityStateStore.EntityStateLease(1L, 100L, EntityTypeEnum.DEVICE, 10L, 7L, state,
                previous, 2L, Instant.now().plusSeconds(25), 25, Instant.now(), 0L,
                (byte) TimeoutSourceTypeEnum.DRIVER.getIndex(), "{}");
    }
}
