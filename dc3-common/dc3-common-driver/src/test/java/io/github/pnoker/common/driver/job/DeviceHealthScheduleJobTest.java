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

package io.github.pnoker.common.driver.job;

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceHealthScheduleJobTest {

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private DriverCustomService driverCustomService;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private JobExecutionContext jobContext;

    private DriverProperties driverProperties;
    private DriverMetadata driverMetadata;
    private DeviceHealthScheduleJob job;

    private static DeviceBO enabledDevice(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        device.setEnableFlag(EnableFlagEnum.ENABLE);
        return device;
    }

    @BeforeEach
    void setUp() {
        driverProperties = new DriverProperties();
        driverProperties.getHealth().getDevice().setTimeoutSeconds(60);
        driverMetadata = new DriverMetadata();
        job = new DeviceHealthScheduleJob(driverProperties, driverMetadata, deviceMetadata, driverCustomService,
                driverSenderService);
    }

    @Test
    void reportsOnlineWhenDeviceHealthReturnsOnline() {
        DeviceBO device = enabledDevice(10L);
        driverMetadata.setDeviceIds(Set.of(10L));
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(Map.of());
        when(driverCustomService.health(Map.of(), device)).thenReturn(DeviceHealthState.online());

        job.executeInternal(jobContext);

        verify(driverSenderService).deviceStatusSender(10L, DeviceStatusEnum.ONLINE, 60, TimeUnit.SECONDS);
    }

    @Test
    void reportsOfflineWhenDeviceHealthReturnsOffline() {
        DeviceBO device = enabledDevice(11L);
        driverMetadata.setDeviceIds(Set.of(11L));
        when(deviceMetadata.getCache(11L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(11L)).thenReturn(Map.of());
        when(driverCustomService.health(Map.of(), device)).thenReturn(DeviceHealthState.offline());

        job.executeInternal(jobContext);

        verify(driverSenderService).deviceStatusSender(11L, DeviceStatusEnum.OFFLINE, 60, TimeUnit.SECONDS);
    }

    @Test
    void reportsOfflineWhenDeviceHealthThrows() {
        DeviceBO device = enabledDevice(12L);
        driverMetadata.setDeviceIds(Set.of(12L));
        when(deviceMetadata.getCache(12L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(12L)).thenReturn(Map.of());
        doThrow(new IllegalStateException("session down")).when(driverCustomService).health(Map.of(), device);

        job.executeInternal(jobContext);

        verify(driverSenderService).deviceStatusSender(12L, DeviceStatusEnum.OFFLINE, 60, TimeUnit.SECONDS);
    }

    @Test
    void reportsDeviceSpecificTimeoutWhenProvidedByHealthHook() {
        DeviceBO device = enabledDevice(15L);
        driverMetadata.setDeviceIds(Set.of(15L));
        when(deviceMetadata.getCache(15L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(15L)).thenReturn(Map.of());
        when(driverCustomService.health(Map.of(), device))
                .thenReturn(DeviceHealthState.online(2, TimeUnit.MINUTES));

        job.executeInternal(jobContext);

        verify(driverSenderService).deviceStatusSender(15L, DeviceStatusEnum.ONLINE, 2, TimeUnit.MINUTES);
    }

    @Test
    void skipsDisabledDevice() {
        DeviceBO device = enabledDevice(13L);
        device.setEnableFlag(EnableFlagEnum.DISABLE);
        driverMetadata.setDeviceIds(Set.of(13L));
        when(deviceMetadata.getCache(13L)).thenReturn(device);

        job.executeInternal(jobContext);

        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void skipsDeviceWhenRequiredDriverConfigIsIncomplete() {
        driverMetadata.getDriverAttributeIdMap().put(1L, new DriverAttributeDTO());
        DeviceBO device = enabledDevice(14L);
        driverMetadata.setDeviceIds(Set.of(14L));
        when(deviceMetadata.getCache(14L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(14L)).thenReturn(Map.of());

        job.executeInternal(jobContext);

        verify(driverCustomService, never()).health(any(), any());
        verifyNoInteractions(driverSenderService);
    }

}
