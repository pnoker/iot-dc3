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
import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceStateServiceImplTest {

    @Mock
    private DeviceAlarmService deviceAlarmService;

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

    @InjectMocks
    private DeviceStateServiceImpl service;

    private DeviceStateDTO heartbeat(Long deviceId, String status, Long driverId, Long tenantId, int ttl, TimeUnit unit) {
        DeviceStateDTO dto = new DeviceStateDTO();
        dto.setDeviceId(deviceId);
        dto.setStatus(status);
        dto.setDriverId(driverId);
        dto.setTenantId(tenantId);
        dto.setTimeOut(ttl);
        dto.setTimeUnit(unit);
        return dto;
    }

    @Test
    void nullDtoDoesNothing() {
        service.heartbeat(null);
        verify(entityStateManager, never()).saveOrUpdate(any());
    }

    @Test
    void newDeviceCreatesDbRowWithCustomTimeout() {
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(10L, "online", 7L, 100L, 25, TimeUnit.SECONDS));

        ArgumentCaptor<EntityStateDO> captor = ArgumentCaptor.forClass(EntityStateDO.class);
        verify(entityStateManager).saveOrUpdate(captor.capture());

        EntityStateDO saved = captor.getValue();
        assertThat(saved.getEntityTypeFlag()).isEqualTo((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        assertThat(saved.getEntityId()).isEqualTo(10L);
        assertThat(saved.getParentEntityId()).isEqualTo(7L);
        assertThat(saved.getTenantId()).isEqualTo(100L);
        assertThat(saved.getLeaseVersion()).isEqualTo(1L);
        assertThat(saved.getStateFlag()).isEqualTo((byte) DeviceStatusEnum.ONLINE.getIndex());
        assertThat(saved.getTimeoutSeconds()).isEqualTo(25);
        assertThat(saved.getLastStateFlag()).isEqualTo((byte) DeviceStatusEnum.OFFLINE.getIndex());
        assertThat(saved.getLastHeartbeatTime()).isNotNull();
        assertThat(saved.getLastAlarmId()).isEqualTo(0L);
        assertThat(saved.getStateExt()).isNotNull();
    }

    @Test
    void existingDeviceIncrementsLeaseVersion() {
        EntityStateDO existing = new EntityStateDO();
        existing.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        existing.setEntityId(10L);
        existing.setLeaseVersion(3L);
        existing.setStateFlag((byte) DeviceStatusEnum.ONLINE.getIndex());

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(existing);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(10L, "online", 7L, 100L, 25, TimeUnit.SECONDS));

        ArgumentCaptor<EntityStateDO> captor = ArgumentCaptor.forClass(EntityStateDO.class);
        verify(entityStateManager).saveOrUpdate(captor.capture());
        assertThat(captor.getValue().getLeaseVersion()).isEqualTo(4L);
        assertThat(captor.getValue().getLastStateFlag()).isEqualTo((byte) DeviceStatusEnum.ONLINE.getIndex());
    }

    @Test
    void nullDriverIdDefaultsToZero() {
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(10L, "online", null, 100L, 25, TimeUnit.SECONDS));

        ArgumentCaptor<EntityStateDO> captor = ArgumentCaptor.forClass(EntityStateDO.class);
        verify(entityStateManager).saveOrUpdate(captor.capture());
        assertThat(captor.getValue().getParentEntityId()).isEqualTo(0L);
    }

    @Test
    void statusFlipTriggersAlarm() {
        EntityStateDO existing = new EntityStateDO();
        existing.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        existing.setEntityId(10L);
        existing.setLeaseVersion(2L);
        existing.setStateFlag((byte) DeviceStatusEnum.ONLINE.getIndex());

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(existing);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(10L, "offline", 7L, 100L, 25, TimeUnit.SECONDS));

        verify(deviceAlarmService).alarm(any());
    }

    @Test
    void sameStatusNoAlarm() {
        EntityStateDO existing = new EntityStateDO();
        existing.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        existing.setEntityId(10L);
        existing.setLeaseVersion(2L);
        existing.setStateFlag((byte) DeviceStatusEnum.ONLINE.getIndex());

        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(existing);
        when(entityStateManager.saveOrUpdate(any())).thenReturn(true);

        service.heartbeat(heartbeat(10L, "online", 7L, 100L, 25, TimeUnit.SECONDS));

        verify(deviceAlarmService, never()).alarm(any());
    }
}
