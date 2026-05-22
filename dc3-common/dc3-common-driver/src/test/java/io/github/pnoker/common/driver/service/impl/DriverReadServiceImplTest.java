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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.exception.ReadPointException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverReadServiceImplTest {

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private PointMetadata pointMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private DriverCustomService driverCustomService;

    @InjectMocks
    private DriverReadServiceImpl service;

    private DeviceBO device;
    private PointBO point;
    private Map<String, AttributeBO> driverConfig;
    private Map<String, AttributeBO> pointConfig;

    @BeforeEach
    void setUp() {
        device = new DeviceBO();
        device.setId(10L);
        device.setPointIds(new HashSet<>(java.util.List.of(20L)));
        point = new PointBO();
        point.setId(20L);
        driverConfig = Map.of("host", AttributeBO.builder().value("127.0.0.1").build());
        pointConfig = Map.of("address", AttributeBO.builder().value("0001").build());
    }

    @Test
    void readSuccessfullyDispatchesValueToSender() {
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(pointConfig);
        when(pointMetadata.getCache(20L)).thenReturn(point);
        ReadPointValue readPointValue = new ReadPointValue(device, point, "42");
        when(driverCustomService.read(driverConfig, pointConfig, device, point)).thenReturn(readPointValue);

        service.read(10L, 20L);

        verify(driverSenderService).pointValueSender(any(PointValue.class));
    }

    @Test
    void readRejectsUnknownDevice() {
        when(deviceMetadata.getCache(10L)).thenReturn(null);
        assertThatThrownBy(() -> service.read(10L, 20L))
                .isInstanceOf(ReadPointException.class)
                .hasMessageContaining("device");
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void readRejectsPointNotBoundToDevice() {
        device.setPointIds(new HashSet<>(java.util.List.of(99L)));
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        assertThatThrownBy(() -> service.read(10L, 20L))
                .isInstanceOf(ReadPointException.class)
                .hasMessageContaining("not contained");
        verifyNoInteractions(driverCustomService);
    }

    @Test
    void readShortCircuitsForEmptyDriverConfig() {
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(Map.of());
        assertThatNoException().isThrownBy(() -> service.read(10L, 20L));
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void readShortCircuitsForEmptyPointConfig() {
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(Map.of());
        assertThatNoException().isThrownBy(() -> service.read(10L, 20L));
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void readRejectsUnknownPoint() {
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(pointConfig);
        when(pointMetadata.getCache(20L)).thenReturn(null);
        assertThatThrownBy(() -> service.read(10L, 20L))
                .isInstanceOf(ReadPointException.class)
                .hasMessageContaining("point");
    }

    @Test
    void readRejectsNullValueFromCustomService() {
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(pointConfig);
        when(pointMetadata.getCache(20L)).thenReturn(point);
        when(driverCustomService.read(driverConfig, pointConfig, device, point)).thenReturn(null);
        assertThatThrownBy(() -> service.read(10L, 20L))
                .isInstanceOf(ReadPointException.class)
                .hasMessageContaining("point value is null");
        verify(driverSenderService, never()).pointValueSender(any(PointValue.class));
    }
}
