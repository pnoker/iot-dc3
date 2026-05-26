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

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.enums.EnableFlagEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverReadScheduleJobTest {

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private DriverReadService driverReadService;

    private DriverMetadata driverMetadata;
    private ThreadPoolExecutor executor;
    private DriverReadScheduleJob job;

    @BeforeEach
    void setUp() {
        driverMetadata = new DriverMetadata();
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        job = new DriverReadScheduleJob(driverMetadata, deviceMetadata, driverReadService, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void enabledPointsAreSubmittedToExecutor() {
        driverMetadata.setDeviceIds(Set.of(10L));
        when(deviceMetadata.getCache(10L)).thenReturn(readableDevice(10L, Set.of(20L, 21L)));

        job.executeInternal(null);

        verify(driverReadService).read(10L, 20L);
        verify(driverReadService).read(10L, 21L);
    }

    @Test
    void disabledDeviceIsSkipped() {
        driverMetadata.setDeviceIds(Set.of(10L));
        DeviceBO device = readableDevice(10L, Set.of(20L));
        device.setEnableFlag(EnableFlagEnum.DISABLE);
        when(deviceMetadata.getCache(10L)).thenReturn(device);

        job.executeInternal(null);

        verify(driverReadService, never()).read(10L, 20L);
    }

    private DeviceBO readableDevice(Long id, Set<Long> pointIds) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        device.setEnableFlag(EnableFlagEnum.ENABLE);
        device.setProfileId(1L);
        device.setPointIds(pointIds);
        device.setDriverAttributeConfigIdMap(Collections.singletonMap(1L, null));
        device.setPointAttributeConfigIdMap(Collections.singletonMap(20L, Collections.singletonMap(1L, null)));
        return device;
    }
}
