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

import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAlarmServiceImplTest {

    @Mock
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @Mock
    private DeviceFacade deviceFacade;

    @InjectMocks
    private DeviceAlarmServiceImpl service;

    @Test
    void dropsAlarmWhenDtoIsNull() {
        service.alarm(null);
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService, deviceFacade);
    }

    @Test
    void dropsAlarmWhenDeviceIdMissing() {
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder().tenantId(7L).message("x").build();
        service.alarm(dto);
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService, deviceFacade);
    }

    @Test
    void persistsAlarmWhenDtoCarriesTenantAndDriverIds() {
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                .deviceId(10L)
                .driverId(3L)
                .tenantId(7L)
                .message("offline")
                .build();

        service.alarm(dto);

        // Facade not consulted because both ids are already valid.
        verifyNoInteractions(deviceFacade);

        ArgumentCaptor<EntityAlarmDO> captor = ArgumentCaptor.forClass(EntityAlarmDO.class);
        verify(entityAlarmManager).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getDriverId()).isEqualTo(3L);
        assertThat(captor.getValue().getDeviceId()).isEqualTo(10L);
        // Device-reported alarms default to P2 — the rule pipeline writes a
        // separate row with the rule-derived severity if a matching rule fires.
        assertThat(captor.getValue().getAlarmLevelFlag()).isEqualTo(AlarmMessageLevelFlagEnum.P2.getIndex());
        verify(alarmRuleTriggerService).processDeviceAlarm(dto);
    }

    @Test
    void backfillsTenantIdViaFacadeWhenDtoMissesIt() {
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                .deviceId(10L)
                .driverId(3L)
                .message("offline")
                .build(); // tenantId missing
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(10L);
        device.setTenantId(7L);
        device.setDriverId(3L);
        when(deviceFacade.getById(10L)).thenReturn(device);

        service.alarm(dto);

        ArgumentCaptor<EntityAlarmDO> captor = ArgumentCaptor.forClass(EntityAlarmDO.class);
        verify(entityAlarmManager).save(captor.capture());
        // Tenant id is now backfilled both on the persisted row and on the DTO so the
        // downstream rule trigger sees a valid tenant context.
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(dto.getTenantId()).isEqualTo(7L);
        verify(alarmRuleTriggerService).processDeviceAlarm(dto);
    }

    @Test
    void backfillsDriverIdViaFacadeWhenDtoMissesIt() {
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                .deviceId(10L)
                .tenantId(7L)
                .message("offline")
                .build(); // driverId missing
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(10L);
        device.setTenantId(7L);
        device.setDriverId(99L);
        when(deviceFacade.getById(10L)).thenReturn(device);

        service.alarm(dto);

        ArgumentCaptor<EntityAlarmDO> captor = ArgumentCaptor.forClass(EntityAlarmDO.class);
        verify(entityAlarmManager).save(captor.capture());
        assertThat(captor.getValue().getDriverId()).isEqualTo(99L);
        assertThat(dto.getDriverId()).isEqualTo(99L);
    }

    @Test
    void dropsAlarmWhenDeviceMetadataIsUnavailable() {
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder()
                .deviceId(10L)
                .message("offline")
                .build(); // missing tenantId + driverId
        when(deviceFacade.getById(10L)).thenReturn(null);

        service.alarm(dto);

        // Without a tenant id we must not write to dc3_entity_alarm with tenant_id=0
        // because the rule trigger then silently drops the event downstream.
        verify(entityAlarmManager, never()).save(any());
        verifyNoInteractions(alarmRuleTriggerService);
    }

    @Test
    void dropsAlarmWhenFacadeReturnsDeviceWithoutTenantId() {
        DeviceAlarmDTO dto = DeviceAlarmDTO.builder().deviceId(10L).message("x").build();
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(10L);
        // tenantId left null
        when(deviceFacade.getById(10L)).thenReturn(device);

        service.alarm(dto);

        verify(entityAlarmManager, never()).save(any());
        verifyNoInteractions(alarmRuleTriggerService);
    }

}
