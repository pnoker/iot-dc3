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

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.mq.sender.MessageSender;

import java.util.Map;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityStateExpiryScannerTest {

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private EntityStateMapper entityStateMapper;

    @Mock
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Mock
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private MessageSender messageSender;

    @Mock
    private Acknowledgment ack;

    @Mock
    private LambdaUpdateChainWrapper<EntityStateDO> updateWrapper;

    @InjectMocks
    private EntityStateExpiryScanner scanner;

    private EntityStateDO deviceState(Long deviceId, Long driverId, byte statusFlag, long leaseVersion, LocalDateTime expireTime) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeEnum.DEVICE.getIndex());
        state.setEntityId(deviceId);
        state.setParentEntityId(driverId);
        state.setStateFlag(statusFlag);
        state.setLastStateFlag(statusFlag);
        state.setLeaseVersion(leaseVersion);
        state.setExpireTime(expireTime);
        state.setTenantId(100L);
        state.setId(deviceId);
        return state;
    }


    private void stubClaimedDevices(List<EntityStateDO> results) {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((org.springframework.transaction.support.TransactionCallback<List<EntityStateDO>>)
                        invocation.getArgument(0)).doInTransaction(null));
        when(entityStateMapper.selectExpiredForClaim(anyByte(), anyByte(), anyByte(), anyByte(), anyInt()))
                .thenReturn(results);
        // empty batches never reach the update — lenient for the skip cases
        org.mockito.Mockito.lenient().when(entityStateMapper.markClaimedOffline(anyList(), anyByte(), anyInt()))
                .thenReturn(results.size());
    }

    private void stubLastAlarmUpdate() {
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);
    }

    @Test
    void noExpiredRowsDoesNothing() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(new MqReceived<>("tick", Map.of(), false), ack);

        verify(messageSender).send(argThat(m -> m.getTopic() == MqTopic.DEVICE_SCAN));
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void emptyClaimSkipsAlarm() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(new MqReceived<>("tick", Map.of(), false), ack);

        verify(entityStateMapper).selectExpiredForClaim(
                eq((byte) EntityTypeEnum.DEVICE.getIndex()),
                eq((byte) EntityStatusEnum.ONLINE.getIndex()),
                eq((byte) EntityStatusEnum.MAINTAIN.getIndex()),
                eq((byte) EntityStatusEnum.FAULT.getIndex()),
                anyInt());
        verify(entityStateMapper, never()).markClaimedOffline(anyList(), anyByte(), anyInt());
        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDeviceAlarm(any());
        verify(messageSender).send(argThat(m -> m.getTopic() == MqTopic.DEVICE_SCAN));
    }

    @Test
    void onlineDeviceExpiredWritesAlarmAndUpdatesState() throws Exception {
        EntityStateDO expired = deviceState(10L, 7L,
                (byte) EntityStatusEnum.ONLINE.getIndex(),
                2L, LocalDateTime.now().minusSeconds(10));

        stubClaimedDevices(List.of(expired));
        stubLastAlarmUpdate();
        when(entityAlarmManager.saveBatch(any())).thenReturn(true);

        scanner.onScanTick(new MqReceived<>("tick", Map.of(), false), ack);

        verify(entityAlarmManager).saveBatch(any());
        verify(alarmRuleTriggerService).processDeviceAlarm(any());
        verify(messageSender).send(argThat(m -> m.getTopic() == MqTopic.DEVICE_SCAN));
    }

    @Test
    void anotherInstanceAlreadyClaimedRowsDoesNothing() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(new MqReceived<>("tick", Map.of(), false), ack);

        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDeviceAlarm(any());
        verify(messageSender).send(argThat(m -> m.getTopic() == MqTopic.DEVICE_SCAN));
    }

    @Test
    void scanTickPublishesNextTickOnSuccess() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(new MqReceived<>("tick", Map.of(), false), ack);

        verify(messageSender).send(argThat(m -> m.getTopic() == MqTopic.DEVICE_SCAN));
    }

    @Test
    void scanTickNacksAndRequeuesOnFailure() throws Exception {
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("DB down"));

        scanner.onScanTick(new MqReceived<>("tick", Map.of(), false), ack);

        verify(ack).reject(true);
        // next tick NOT published on failure
        verify(messageSender, never()).send(any());
    }
}
