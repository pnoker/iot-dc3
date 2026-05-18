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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlarmRuleTriggerServiceImplTest {

    @Mock
    private AlarmRulePipelineService alarmRulePipelineService;

    @InjectMocks
    private AlarmRuleTriggerServiceImpl service;

    @Test
    void pointValueBuildsPointRuleFact() {
        PointValueBO pointValue = PointValueBO.builder()
                .tenantId(1L)
                .driverId(2L)
                .deviceId(3L)
                .pointId(4L)
                .rawValue("85")
                .calValue("86.5")
                .numValue(86.5D)
                .build();

        service.processPointValue(pointValue);

        ArgumentCaptor<RuleFact> captor = ArgumentCaptor.forClass(RuleFact.class);
        verify(alarmRulePipelineService).process(captor.capture());
        RuleFact fact = captor.getValue();
        assertThat(fact.getTenantId()).isEqualTo(1L);
        assertThat(fact.getAlarmTargetTypeFlag()).isEqualTo(AlarmTargetTypeFlagEnum.POINT);
        assertThat(fact.getEntityId()).isEqualTo(4L);
        assertThat(fact.value("deviceId")).isEqualTo(3L);
        assertThat(fact.value("numValue")).isEqualTo(86.5D);
        assertThat(fact.value("value")).isEqualTo("86.5");
    }

    @Test
    void deviceEventBuildsDeviceRuleFact() {
        DeviceEventDTO.DeviceStatus payload = new DeviceEventDTO.DeviceStatus(10L, DeviceStatusEnum.OFFLINE);
        payload.setTenantId(1L);
        payload.setDriverId(2L);
        payload.setMessage("device offline");

        service.processDeviceEvent(payload, DeviceEventTypeEnum.ALARM, "device-state-flip", 99L);

        ArgumentCaptor<RuleFact> captor = ArgumentCaptor.forClass(RuleFact.class);
        verify(alarmRulePipelineService).process(captor.capture());
        RuleFact fact = captor.getValue();
        assertThat(fact.getAlarmTargetTypeFlag()).isEqualTo(AlarmTargetTypeFlagEnum.DEVICE);
        assertThat(fact.getEntityId()).isEqualTo(10L);
        assertThat(fact.getEventId()).isEqualTo(99L);
        assertThat(fact.value("status")).isEqualTo(DeviceStatusEnum.OFFLINE.getCode());
        assertThat(fact.value("message")).isEqualTo("device offline");
    }

    @Test
    void driverEventBuildsDriverRuleFact() {
        DriverEventDTO.DriverStatus payload = new DriverEventDTO.DriverStatus(7L, DriverStatusEnum.FAULT);
        payload.setTenantId(1L);
        payload.setMessage("driver fault");

        service.processDriverEvent(payload, DriverEventTypeEnum.ALARM, "driver-alarm", 88L);

        ArgumentCaptor<RuleFact> captor = ArgumentCaptor.forClass(RuleFact.class);
        verify(alarmRulePipelineService).process(captor.capture());
        RuleFact fact = captor.getValue();
        assertThat(fact.getAlarmTargetTypeFlag()).isEqualTo(AlarmTargetTypeFlagEnum.DRIVER);
        assertThat(fact.getEntityId()).isEqualTo(7L);
        assertThat(fact.getEventId()).isEqualTo(88L);
        assertThat(fact.value("status")).isEqualTo(DriverStatusEnum.FAULT.getCode());
    }

    @Test
    void invalidPointValueDoesNotTriggerPipeline() {
        service.processPointValue(PointValueBO.builder().tenantId(1L).build());
        verify(alarmRulePipelineService, never()).process(org.mockito.ArgumentMatchers.any());
    }

}
