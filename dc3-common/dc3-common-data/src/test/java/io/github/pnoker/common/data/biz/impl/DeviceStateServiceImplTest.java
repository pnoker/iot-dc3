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

import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.enums.TimeoutSourceFlagEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceStateServiceImplTest {

    @Mock
    private DeviceAlarmService deviceAlarmService;

    @Mock
    private EntityStateMapper entityStateMapper;

    @InjectMocks
    private DeviceStateServiceImpl service;

    private DeviceStateDTO heartbeat(Long deviceId, String status, Long driverId, Long tenantId, int ttl, TimeUnit unit) {
        DeviceStateDTO dto = new DeviceStateDTO();
        dto.setDeviceId(deviceId);
        dto.setStatus(status);
        dto.setDriverId(driverId);
        dto.setTenantId(tenantId);
        dto.setTimeout(ttl);
        dto.setTimeoutUnit(unit);
        return dto;
    }

    private EntityStateDO persisted(byte stateFlag, byte lastStateFlag, long leaseVersion) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        state.setEntityId(10L);
        state.setParentEntityId(7L);
        state.setTenantId(100L);
        state.setStateFlag(stateFlag);
        state.setLastStateFlag(lastStateFlag);
        state.setLeaseVersion(leaseVersion);
        state.setTimeoutSeconds(25);
        state.setLastHeartbeatTime(LocalDateTime.now());
        state.setLastAlarmId(0L);
        return state;
    }

    private void stubUpsert(EntityStateDO state) {
        when(entityStateMapper.upsertEntityState(anyLong(), anyLong(), anyByte(), anyLong(), anyLong(), anyByte(),
                anyByte(), any(), anyInt(), anyByte(), anyString(), any())).thenReturn(state);
    }

    @Test
    void nullDtoDoesNothing() {
        service.heartbeat(null);

        verifyNoInteractions(entityStateMapper);
    }

    @Test
    void newDeviceUpsertsDbRowWithCustomTimeout() {
        stubUpsert(persisted((byte) EntityStatusEnum.ONLINE.getIndex(),
                (byte) EntityStatusEnum.OFFLINE.getIndex(), 1L));

        service.heartbeat(heartbeat(10L, EntityStatusEnum.ONLINE.getCode(), 7L, 100L, 25, TimeUnit.SECONDS));

        verify(entityStateMapper).upsertEntityState(anyLong(),
                eq(100L),
                eq((byte) EntityTypeFlagEnum.DEVICE.getIndex()),
                eq(10L),
                eq(7L),
                eq((byte) EntityStatusEnum.ONLINE.getIndex()),
                eq((byte) EntityStatusEnum.OFFLINE.getIndex()),
                any(LocalDateTime.class),
                eq(25),
                eq((byte) TimeoutSourceFlagEnum.DRIVER.getIndex()),
                eq("device-heartbeat"),
                any());
    }

    @Test
    void nullDriverIdDoesNothing() {
        service.heartbeat(heartbeat(10L, EntityStatusEnum.ONLINE.getCode(), null, 100L, 25, TimeUnit.SECONDS));

        verifyNoInteractions(entityStateMapper);
    }

    @Test
    void nonPositiveTimeoutDoesNothing() {
        service.heartbeat(heartbeat(10L, EntityStatusEnum.ONLINE.getCode(), 7L, 100L, 0, TimeUnit.SECONDS));

        verifyNoInteractions(entityStateMapper);
    }

    @Test
    void statusFlipTriggersAlarm() {
        stubUpsert(persisted((byte) EntityStatusEnum.OFFLINE.getIndex(),
                (byte) EntityStatusEnum.ONLINE.getIndex(), 3L));

        service.heartbeat(heartbeat(10L, EntityStatusEnum.OFFLINE.getCode(), 7L, 100L, 25, TimeUnit.SECONDS));

        verify(deviceAlarmService).alarm(any());
    }

    @Test
    void sameStatusNoAlarm() {
        stubUpsert(persisted((byte) EntityStatusEnum.ONLINE.getIndex(),
                (byte) EntityStatusEnum.ONLINE.getIndex(), 3L));

        service.heartbeat(heartbeat(10L, EntityStatusEnum.ONLINE.getCode(), 7L, 100L, 25, TimeUnit.SECONDS));

        verify(deviceAlarmService, never()).alarm(any());
    }
}
