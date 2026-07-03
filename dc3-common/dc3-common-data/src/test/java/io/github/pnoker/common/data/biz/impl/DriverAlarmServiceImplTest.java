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
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
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

@ExtendWith(MockitoExtension.class)
class DriverAlarmServiceImplTest {

    @Mock
    private EntityAlarmManager entityAlarmManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @InjectMocks
    private DriverAlarmServiceImpl service;

    @Test
    void dropsAlarmWhenDtoIsNull() {
        service.alarm(null);
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void dropsAlarmWhenDriverIdMissing() {
        DriverAlarmDTO dto = DriverAlarmDTO.builder().tenantId(7L).message("x").build();
        service.alarm(dto);
        verifyNoInteractions(entityAlarmManager, alarmRuleTriggerService);
    }

    @Test
    void persistsAlarmWhenDtoCarriesTenantId() {
        DriverAlarmDTO dto = DriverAlarmDTO.builder()
                .driverId(3L)
                .tenantId(7L)
                .message("offline")
                .build();

        service.alarm(dto);

        ArgumentCaptor<EntityAlarmDO> captor = ArgumentCaptor.forClass(EntityAlarmDO.class);
        verify(entityAlarmManager).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        verify(alarmRuleTriggerService).processDriverAlarm(dto);
    }

    @Test
    void dropsAlarmWhenTenantIdIsMissing() {
        // The fail-closed tenant-line interceptor forbids reverse-resolving the tenant from
        // the driver, so a tenant-less alarm is dropped rather than persisted as tenant_id=0.
        DriverAlarmDTO dto = DriverAlarmDTO.builder()
                .driverId(3L)
                .message("offline")
                .build(); // tenantId missing

        service.alarm(dto);

        verify(entityAlarmManager, never()).save(any());
        verifyNoInteractions(alarmRuleTriggerService);
    }

}
