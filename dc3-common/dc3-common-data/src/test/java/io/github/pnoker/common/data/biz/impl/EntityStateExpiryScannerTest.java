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
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    @Mock
    private LambdaUpdateChainWrapper<EntityStateDO> updateWrapper;

    @InjectMocks
    private EntityStateExpiryScanner scanner;

    private EntityStateDO deviceState(Long deviceId, Long driverId, byte statusFlag, long leaseVersion, LocalDateTime expireTime) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
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

    private Message mockMessage(long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message("tick".getBytes(), props);
    }

    private void stubClaimedDevices(List<EntityStateDO> results) {
        when(entityStateMapper.claimExpiredDevices(anyByte(), anyByte(), anyByte(), anyByte(), anyByte(), anyInt(), anyInt()))
                .thenReturn(results);
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

        scanner.onScanTick(channel, mockMessage(1L));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void emptyClaimSkipsAlarm() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(channel, mockMessage(1L));

        verify(entityStateMapper).claimExpiredDevices(
                eq((byte) EntityTypeFlagEnum.DEVICE.getIndex()),
                eq((byte) EntityStatusEnum.ONLINE.getIndex()),
                eq((byte) EntityStatusEnum.MAINTAIN.getIndex()),
                eq((byte) EntityStatusEnum.FAULT.getIndex()),
                eq((byte) EntityStatusEnum.OFFLINE.getIndex()),
                anyInt(),
                anyInt());
        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDeviceAlarm(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void onlineDeviceExpiredWritesAlarmAndUpdatesState() throws Exception {
        EntityStateDO expired = deviceState(10L, 7L,
                (byte) EntityStatusEnum.ONLINE.getIndex(),
                2L, LocalDateTime.now().minusSeconds(10));

        stubClaimedDevices(List.of(expired));
        stubLastAlarmUpdate();
        when(entityAlarmManager.save(any())).thenReturn(true);

        scanner.onScanTick(channel, mockMessage(1L));

        verify(entityAlarmManager).save(any());
        verify(alarmRuleTriggerService).processDeviceAlarm(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void anotherInstanceAlreadyClaimedRowsDoesNothing() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(channel, mockMessage(1L));

        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDeviceAlarm(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void scanTickPublishesNextTickOnSuccess() throws Exception {
        stubClaimedDevices(Collections.emptyList());

        scanner.onScanTick(channel, mockMessage(1L));

        verify(rabbitTemplate).convertAndSend(
                "dc3.e.state_timeout_delay",
                "state.timeout.device.scan.tick",
                "tick");
    }

    @Test
    void scanTickNacksAndRequeuesOnFailure() throws Exception {
        when(entityStateMapper.claimExpiredDevices(anyByte(), anyByte(), anyByte(), anyByte(), anyByte(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("DB down"));

        scanner.onScanTick(channel, mockMessage(1L));

        verify(channel).basicNack(1L, false, true);
        // next tick NOT published on failure
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }
}
