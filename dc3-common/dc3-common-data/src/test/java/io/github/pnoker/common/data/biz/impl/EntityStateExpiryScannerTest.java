package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.mq.sender.MessageSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityStateExpiryScannerTest {
    @Mock ReactiveEntityStateStore stateStore;
    @Mock ReactiveEntityAlarmStore alarmStore;
    @Mock AlarmRuleTriggerService triggerService;
    @Mock MessageSender sender;
    @Mock Acknowledgment ack;

    @Test
    void emptyScanPublishesNextTickAndAcknowledges() {
        when(stateStore.claimExpired(EntityTypeEnum.DEVICE, 500, 300)).thenReturn(Flux.empty());
        StepVerifier.create(scanner().onScanTick(new MqReceived<>("tick", Map.of(), false), ack))
                .verifyComplete();
        verify(sender).send(any());
        verify(alarmStore, never()).insertBatch(any());
    }

    @Test
    void expiredLeasePersistsAlarmAndFencedUpdate() {
        ReactiveEntityStateStore.EntityStateLease lease = new ReactiveEntityStateStore.EntityStateLease(1L, 100L,
                EntityTypeEnum.DEVICE, 10L, 7L, (byte) EntityStatusEnum.OFFLINE.getIndex(),
                (byte) EntityStatusEnum.ONLINE.getIndex(), 3L, Instant.now(), 300, Instant.now(), 0L, (byte) 1, "{}");
        when(stateStore.claimExpired(EntityTypeEnum.DEVICE, 500, 300)).thenReturn(Flux.just(lease));
        when(alarmStore.insertBatch(any())).thenAnswer(invocation -> {
            java.util.List<io.github.pnoker.common.data.entity.model.EntityAlarmDO> alarms = invocation.getArgument(0);
            alarms.get(0).setId(1L);
            return Mono.just(alarms);
        });
        when(triggerService.processDeviceAlarm(any())).thenReturn(Mono.empty());
        when(stateStore.markAlarm(100L, EntityTypeEnum.DEVICE, 10L, 3L, 1L)).thenReturn(Mono.just(true));
        StepVerifier.create(scanner().onScanTick(new MqReceived<>("tick", Map.of(), false), ack))
                .verifyComplete();
        verify(alarmStore).insertBatch(any());
        verify(stateStore).markAlarm(100L, EntityTypeEnum.DEVICE, 10L, 3L, 1L);
        verify(triggerService).processDeviceAlarm(any());
    }

    @Test
    void claimFailureRequeuesTick() {
        when(stateStore.claimExpired(any(), any(Integer.class), any(Integer.class))).thenReturn(Flux.error(new IllegalStateException("down")));
        StepVerifier.create(scanner().onScanTick(new MqReceived<>("tick", Map.of(), false), ack))
                .expectErrorMessage("down")
                .verify();
        verify(sender, never()).send(any());
    }

    private EntityStateExpiryScanner scanner() {
        return new EntityStateExpiryScanner(stateStore, alarmStore, triggerService, sender);
    }
}
