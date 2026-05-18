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

import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.DeviceEventManager;
import io.github.pnoker.common.data.dal.DriverEventManager;
import io.github.pnoker.common.data.entity.model.DeviceEventDO;
import io.github.pnoker.common.data.entity.model.DriverEventDO;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfflineExpiryListenerTest {

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private DriverEventManager driverEventManager;

    @Mock
    private DeviceEventManager deviceEventManager;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @InjectMocks
    private OfflineExpiryListener listener;

    @Test
    void driverExpiryPersistsOfflineEventAndTriggersRules() throws Exception {
        FacadeDriverBO driver = new FacadeDriverBO();
        driver.setTenantId(1L);
        when(driverFacade.getById(7L)).thenReturn(driver);
        doAnswer(invocation -> {
            DriverEventDO event = invocation.getArgument(0);
            event.setId(701L);
            return true;
        }).when(driverEventManager).save(any(DriverEventDO.class));

        invoke("handleDriverExpiry", PrefixConstant.DRIVER_STATUS_KEY_PREFIX + 7L, DriverStatusEnum.ONLINE.getCode());

        ArgumentCaptor<DriverEventDO> eventCaptor = ArgumentCaptor.forClass(DriverEventDO.class);
        verify(driverEventManager).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDriverId()).isEqualTo(7L);
        assertThat(eventCaptor.getValue().getEventTypeFlag()).isEqualTo(DriverEventTypeEnum.ALARM.getIndex());

        ArgumentCaptor<DriverEventDTO.DriverStatus> payloadCaptor =
                ArgumentCaptor.forClass(DriverEventDTO.DriverStatus.class);
        verify(alarmRuleTriggerService).processDriverEvent(payloadCaptor.capture(), eq(DriverEventTypeEnum.ALARM),
                eq("driver-offline"), eq(701L));
        assertThat(payloadCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(payloadCaptor.getValue().getStatus()).isEqualTo(DriverStatusEnum.OFFLINE);
    }

    @Test
    void deviceExpiryPersistsOfflineEventAndTriggersRules() throws Exception {
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setTenantId(1L);
        device.setDriverId(2L);
        when(deviceFacade.getById(10L)).thenReturn(device);
        doAnswer(invocation -> {
            DeviceEventDO event = invocation.getArgument(0);
            event.setId(801L);
            return true;
        }).when(deviceEventManager).save(any(DeviceEventDO.class));

        invoke("handleDeviceExpiry", PrefixConstant.DEVICE_STATUS_KEY_PREFIX + 10L,
                DeviceStatusEnum.ONLINE.getCode());

        ArgumentCaptor<DeviceEventDO> eventCaptor = ArgumentCaptor.forClass(DeviceEventDO.class);
        verify(deviceEventManager).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDeviceId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().getEventTypeFlag()).isEqualTo(DeviceEventTypeEnum.ALARM.getIndex());

        ArgumentCaptor<DeviceEventDTO.DeviceStatus> payloadCaptor =
                ArgumentCaptor.forClass(DeviceEventDTO.DeviceStatus.class);
        verify(alarmRuleTriggerService).processDeviceEvent(payloadCaptor.capture(), eq(DeviceEventTypeEnum.ALARM),
                eq("device-offline"), eq(801L));
        assertThat(payloadCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(payloadCaptor.getValue().getDriverId()).isEqualTo(2L);
        assertThat(payloadCaptor.getValue().getStatus()).isEqualTo(DeviceStatusEnum.OFFLINE);
    }

    private void invoke(String methodName, String key, String lastStatus) throws Exception {
        Method method = OfflineExpiryListener.class.getDeclaredMethod(methodName, String.class, String.class);
        method.setAccessible(true);
        method.invoke(listener, key, lastStatus);
    }

}
