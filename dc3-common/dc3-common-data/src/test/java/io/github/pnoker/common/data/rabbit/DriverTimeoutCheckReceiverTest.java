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

package io.github.pnoker.common.data.rabbit;

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.entity.dto.DriverTimeoutCheckDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverTimeoutCheckReceiverTest {

    @Mock ReactiveEntityStateStore entityStateStore;
    @Mock ReactiveEntityAlarmStore entityAlarmStore;
    @Mock AlarmRuleTriggerService alarmRuleTriggerService;
    @Mock TransactionalOperator transactionalOperator;
    @Mock Acknowledgment acknowledgment;

    private DriverTimeoutCheckReceiver receiver;

    @BeforeEach
    void setUp() {
        receiver = new DriverTimeoutCheckReceiver(entityStateStore, entityAlarmStore,
                alarmRuleTriggerService, transactionalOperator);
    }

    @Test
    void rejectsInvalidPayload() {
        StepVerifier.create(receiver.driverTimeoutCheck(received(timeoutCheck(null, 100L, 1L)), acknowledgment))
                .verifyComplete();

        verify(acknowledgment).reject(false);
        verifyNoInteractions(entityStateStore, entityAlarmStore, alarmRuleTriggerService, transactionalOperator);
    }

    @Test
    void lostClaimCompletesWithoutAlarm() {
        when(entityStateStore.claimExpired(100L, EntityTypeEnum.DRIVER, 7L, 4L, 300))
                .thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(receiver.driverTimeoutCheck(received(timeoutCheck(7L, 100L, 4L)), acknowledgment))
                .verifyComplete();

        verifyNoInteractions(entityAlarmStore, alarmRuleTriggerService);
    }

    @Test
    void claimedLeaseCommitsAlarmBeforeTriggeringRules() {
        when(entityStateStore.claimExpired(100L, EntityTypeEnum.DRIVER, 7L, 4L, 300))
                .thenReturn(Mono.just(lease(0L)));
        when(entityAlarmStore.insert(any(EntityAlarmDO.class))).thenAnswer(invocation -> {
            EntityAlarmDO alarm = invocation.getArgument(0);
            alarm.setId(55L);
            return Mono.just(alarm);
        });
        when(entityStateStore.markAlarm(100L, EntityTypeEnum.DRIVER, 7L, 5L, 55L))
                .thenReturn(Mono.just(true));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alarmRuleTriggerService.processDriverAlarm(any())).thenReturn(Mono.empty());

        StepVerifier.create(receiver.driverTimeoutCheck(received(timeoutCheck(7L, 100L, 4L)), acknowledgment))
                .verifyComplete();

        verify(entityAlarmStore).insert(any(EntityAlarmDO.class));
        verify(entityStateStore).markAlarm(100L, EntityTypeEnum.DRIVER, 7L, 5L, 55L);
        verify(alarmRuleTriggerService).processDriverAlarm(any());
    }

    @Test
    void redeliveryResumesRulesFromPersistedAlarm() {
        when(entityStateStore.claimExpired(100L, EntityTypeEnum.DRIVER, 7L, 4L, 300))
                .thenReturn(Mono.just(lease(55L)));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alarmRuleTriggerService.processDriverAlarm(any())).thenReturn(Mono.empty());

        StepVerifier.create(receiver.driverTimeoutCheck(received(timeoutCheck(7L, 100L, 4L)), acknowledgment))
                .verifyComplete();

        verify(entityAlarmStore, never()).insert(any());
        verify(transactionalOperator).transactional(any(Mono.class));
        verify(alarmRuleTriggerService).processDriverAlarm(any());
    }

    @Test
    void persistenceFailurePropagatesForAdapterRequeue() {
        when(entityStateStore.claimExpired(100L, EntityTypeEnum.DRIVER, 7L, 4L, 300))
                .thenReturn(Mono.just(lease(0L)));
        when(entityAlarmStore.insert(any())).thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(receiver.driverTimeoutCheck(received(timeoutCheck(7L, 100L, 4L)), acknowledgment))
                .expectErrorMessage("database unavailable")
                .verify();

        verify(alarmRuleTriggerService, never()).processDriverAlarm(any());
    }

    private ReactiveEntityStateStore.EntityStateLease lease(Long alarmId) {
        return new ReactiveEntityStateStore.EntityStateLease(9L, 100L, EntityTypeEnum.DRIVER, 7L, 0L,
                EntityStatusEnum.OFFLINE.getIndex(), EntityStatusEnum.ONLINE.getIndex(), 5L,
                Instant.now().plusSeconds(300), 45, Instant.now(), alarmId, (byte) 0, "{}");
    }

    private DriverTimeoutCheckDTO timeoutCheck(Long driverId, Long tenantId, Long leaseVersion) {
        return DriverTimeoutCheckDTO.builder().driverId(driverId).tenantId(tenantId)
                .leaseVersion(leaseVersion).build();
    }

    private MqReceived<DriverTimeoutCheckDTO> received(DriverTimeoutCheckDTO value) {
        return new MqReceived<>(value, Map.of(), false);
    }
}
