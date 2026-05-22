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
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.utils.JsonUtil;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverWriteServiceImplTest {

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private PointMetadata pointMetadata;

    @Mock
    private DriverCustomService driverCustomService;

    @Mock
    private DriverSenderService driverSenderService;

    @InjectMocks
    private DriverWriteServiceImpl service;

    private DeviceBO device;
    private PointBO point;

    @BeforeEach
    void setUp() {
        device = new DeviceBO();
        device.setId(10L);
        device.setPointIds(new HashSet<>(java.util.List.of(20L)));
        point = new PointBO();
        point.setId(20L);
    }

    @Test
    void writeSuccessfullyDispatchesToCustomService() {
        Map<String, AttributeBO> driverConfig = Map.of("host", AttributeBO.builder().value("h").build());
        Map<String, AttributeBO> pointConfig = Map.of("address", AttributeBO.builder().value("a").build());
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(pointConfig);
        when(pointMetadata.getCache(20L)).thenReturn(point);

        service.write(10L, 20L, "42");

        verify(driverCustomService).write(eq(driverConfig), eq(pointConfig), eq(device), eq(point),
                any(WritePointValue.class));
        verify(driverSenderService).pointValueSender(any(PointValue.class));
    }

    @Test
    void writeRejectsUnknownDevice() {
        when(deviceMetadata.getCache(10L)).thenReturn(null);
        assertThatThrownBy(() -> service.write(10L, 20L, "v")).isInstanceOf(ReadPointException.class);
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void writeRejectsPointNotBoundToDevice() {
        device.setPointIds(new HashSet<>(java.util.List.of(99L)));
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        assertThatThrownBy(() -> service.write(10L, 20L, "v")).isInstanceOf(ReadPointException.class);
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void writeRejectsUnknownPoint() {
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(Map.of("k", AttributeBO.builder().build()));
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(Map.of("k", AttributeBO.builder().build()));
        when(pointMetadata.getCache(20L)).thenReturn(null);
        assertThatThrownBy(() -> service.write(10L, 20L, "v")).isInstanceOf(ReadPointException.class);
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void commandWriteIgnoresNullPayload() {
        PointCommandDTO commandDTO = new PointCommandDTO();
        commandDTO.setContent(null);
        assertThatNoException().isThrownBy(() -> service.write(commandDTO));
        verify(driverCustomService, never()).write(any(), any(), any(), any(), any(WritePointValue.class));
    }

    @Test
    void commandWriteDispatchesParsedPointWrite() {
        Map<String, AttributeBO> driverConfig = Map.of("k", AttributeBO.builder().value("v").build());
        Map<String, AttributeBO> pointConfig = Map.of("k", AttributeBO.builder().value("v").build());
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getPointConfig(10L, 20L)).thenReturn(pointConfig);
        when(pointMetadata.getCache(20L)).thenReturn(point);

        PointCommandDTO commandDTO = new PointCommandDTO();
        commandDTO.setContent(JsonUtil.toJsonString(new PointCommandDTO.PointWrite(10L, 20L, "99")));
        service.write(commandDTO);

        verify(driverCustomService).write(eq(driverConfig), eq(pointConfig), eq(device), eq(point),
                any(WritePointValue.class));
        verify(driverSenderService).pointValueSender(any(PointValue.class));
    }
}
