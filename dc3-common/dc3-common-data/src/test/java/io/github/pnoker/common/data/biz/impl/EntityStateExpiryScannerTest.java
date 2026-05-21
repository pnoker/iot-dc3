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
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

    @Mock
    private LambdaUpdateChainWrapper<EntityStateDO> updateWrapper;

    @InjectMocks
    private EntityStateExpiryScanner scanner;

    private EntityStateDO driverState(Long driverId, byte statusFlag, long leaseVersion, LocalDateTime expireTime) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DRIVER.getIndex());
        state.setEntityId(driverId);
        state.setDriverId(driverId);
        state.setStateFlag(statusFlag);
        state.setLeaseVersion(leaseVersion);
        state.setExpireTime(expireTime);
        state.setTenantId(100L);
        return state;
    }

    private EntityStateDO deviceState(Long deviceId, Long driverId, byte statusFlag, long leaseVersion, LocalDateTime expireTime) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        state.setEntityId(deviceId);
        state.setDriverId(driverId);
        state.setStateFlag(statusFlag);
        state.setLeaseVersion(leaseVersion);
        state.setExpireTime(expireTime);
        state.setTenantId(100L);
        return state;
    }

    @Test
    void noExpiredRowsDoesNothing() {
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(Collections.emptyList());

        scanner.scanExpiredLeases();

        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void alreadyOfflineDriverSkipsAlarm() {
        EntityStateDO offline = driverState(1L,
                (byte) DriverStatusEnum.OFFLINE.getIndex(),
                5L, LocalDateTime.now().minusSeconds(10));

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(List.of(offline));
        // already offline path: lambdaUpdate for renewal
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);

        scanner.scanExpiredLeases();

        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDriverAlarm(any());
    }

    @Test
    void onlineDriverExpiredWritesAlarmAndUpdatesState() {
        EntityStateDO expired = driverState(1L,
                (byte) DriverStatusEnum.ONLINE.getIndex(),
                3L, LocalDateTime.now().minusSeconds(10));

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(List.of(expired));
        // atomic claim
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);
        when(entityAlarmManager.save(any())).thenReturn(true);

        scanner.scanExpiredLeases();

        verify(entityAlarmManager).save(any());
        verify(alarmRuleTriggerService).processDriverAlarm(any());
    }

    @Test
    void claimFailsWhenAnotherInstanceAlreadyProcessed() {
        EntityStateDO expired = driverState(1L,
                (byte) DriverStatusEnum.ONLINE.getIndex(),
                3L, LocalDateTime.now().minusSeconds(10));

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(List.of(expired));
        // atomic UPDATE returns false = another instance won
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(false);

        scanner.scanExpiredLeases();

        verify(entityAlarmManager, never()).save(any());
        verify(alarmRuleTriggerService, never()).processDriverAlarm(any());
    }

    @Test
    void onlineDeviceExpiredWritesAlarmAndUpdatesState() {
        EntityStateDO expired = deviceState(10L, 7L,
                (byte) DeviceStatusEnum.ONLINE.getIndex(),
                2L, LocalDateTime.now().minusSeconds(10));

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(List.of(expired));
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);
        when(entityAlarmManager.save(any())).thenReturn(true);

        scanner.scanExpiredLeases();

        verify(entityAlarmManager).save(any());
        verify(alarmRuleTriggerService).processDeviceAlarm(any());
    }

    @Test
    void multipleExpiredRowsAllProcessed() {
        EntityStateDO driver = driverState(1L,
                (byte) DriverStatusEnum.ONLINE.getIndex(),
                1L, LocalDateTime.now().minusSeconds(10));
        EntityStateDO device = deviceState(10L, 1L,
                (byte) DeviceStatusEnum.ONLINE.getIndex(),
                1L, LocalDateTime.now().minusSeconds(10));

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.lt(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.last(any())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(List.of(driver, device));
        when(entityStateManager.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.set(any(), any())).thenReturn(updateWrapper);
        when(updateWrapper.update()).thenReturn(true);
        when(entityAlarmManager.save(any())).thenReturn(true);

        scanner.scanExpiredLeases();

        InOrder inOrder = inOrder(alarmRuleTriggerService);
        inOrder.verify(alarmRuleTriggerService).processDriverAlarm(any());
        inOrder.verify(alarmRuleTriggerService).processDeviceAlarm(any());
    }
}
