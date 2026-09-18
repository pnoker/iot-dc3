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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.mq.sender.MessageSender;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class EntityStateExpiryScannerTest {
    @Mock
    ReactiveEntityStateStore stateStore;

    @Mock
    ReactiveEntityAlarmStore alarmStore;

    @Mock
    AlarmRuleTriggerService triggerService;

    @Mock
    MessageSender sender;

    @Mock
    Acknowledgment ack;

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
        ReactiveEntityStateStore.EntityStateLease lease = new ReactiveEntityStateStore.EntityStateLease(
                1L,
                100L,
                EntityTypeEnum.DEVICE,
                10L,
                7L,
                (byte) EntityStatusEnum.OFFLINE.getIndex(),
                (byte) EntityStatusEnum.ONLINE.getIndex(),
                3L,
                Instant.now(),
                300,
                Instant.now(),
                0L,
                (byte) 1,
                "{}");
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
        when(stateStore.claimExpired(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(Flux.error(new IllegalStateException("down")));
        StepVerifier.create(scanner().onScanTick(new MqReceived<>("tick", Map.of(), false), ack))
                .expectErrorMessage("down")
                .verify();
        verify(sender, never()).send(any());
    }

    private EntityStateExpiryScanner scanner() {
        return new EntityStateExpiryScanner(stateStore, alarmStore, triggerService, sender);
    }
}
