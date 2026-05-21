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

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityStateExpiryScannerTest {

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    @Mock
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

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
        state.setLeaseVersion(leaseVersion);
        state.setExpireTime(expireTime);
        state.setTenantId(100L);
        return state;
    }

    private Message mockMessage(long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message("tick".getBytes(), props);
    }

    private void stubDeviceQuery(List<EntityStateDO> results) {
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.in(any(), any(Object[].class))).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(results);
    }

    private void stubClaimSuccess() {
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);
    }

    private void stubClaimFailure() {
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(false);
    }

    @Test
    void noExpiredRowsDoesNothing() throws Exception {
        stubDeviceQuery(Collections.emptyList());

        scanner.onScanTick(channel, mockMessage(1L));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void alreadyOfflineDeviceSkipsAlarm() throws Exception {
        EntityStateDO offline = deviceState(10L, 7L,
                (byte) DeviceStatusEnum.OFFLINE.getIndex(),
                5L, LocalDateTime.now().minusSeconds(10));

        stubDeviceQuery(List.of(offline));
        // already offline path: lambdaUpdate for renewal
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);

        scanner.onScanTick(channel, mockMessage(1L));

        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDeviceAlarm(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void onlineDeviceExpiredWritesAlarmAndUpdatesState() throws Exception {
        EntityStateDO expired = deviceState(10L, 7L,
                (byte) DeviceStatusEnum.ONLINE.getIndex(),
                2L, LocalDateTime.now().minusSeconds(10));

        stubDeviceQuery(List.of(expired));
        stubClaimSuccess();
        when(entityAlarmManager.save(any())).thenReturn(true);

        scanner.onScanTick(channel, mockMessage(1L));

        verify(entityAlarmManager).save(any());
        verify(alarmRuleTriggerService).processDeviceAlarm(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void claimFailsWhenAnotherInstanceAlreadyProcessed() throws Exception {
        EntityStateDO expired = deviceState(10L, 7L,
                (byte) DeviceStatusEnum.ONLINE.getIndex(),
                3L, LocalDateTime.now().minusSeconds(10));

        stubDeviceQuery(List.of(expired));
        stubClaimFailure();

        scanner.onScanTick(channel, mockMessage(1L));

        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDeviceAlarm(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void scanTickPublishesNextTickOnSuccess() throws Exception {
        stubDeviceQuery(Collections.emptyList());

        scanner.onScanTick(channel, mockMessage(1L));

        verify(rabbitTemplate).convertAndSend(
                "dc3.e.state_timeout_delay",
                "state.timeout.device.scan.tick",
                "tick");
    }

    @Test
    void scanTickNacksAndRequeuesOnFailure() throws Exception {
        when(entityStateManager.lambdaQuery()).thenThrow(new RuntimeException("DB down"));

        scanner.onScanTick(channel, mockMessage(1L));

        verify(channel).basicNack(1L, false, true);
        // next tick NOT published on failure
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }
}
