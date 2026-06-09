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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Sl651DriverCustomServiceImplTest {

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private PointMetadata pointMetadata;

    private DriverMetadata driverMetadata;
    private Sl651DriverCustomServiceImpl service;

    @BeforeEach
    void setUp() {
        driverMetadata = new DriverMetadata();
        service = new Sl651DriverCustomServiceImpl(driverMetadata, driverSenderService, deviceMetadata, pointMetadata);
    }

    @Test
    void forwardTelemetryMapsStationElementsToConfiguredPoints() {
        driverMetadata.getDeviceIds().add(10L);
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setDeviceCode("01020304");
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getPointConfig(10L)).thenReturn(Map.of(
                20L, Map.of("index", AttributeBO.builder().value("1").type(AttributeTypeEnum.INT).build())
        ));
        PointBO point = new PointBO();
        point.setId(20L);
        when(pointMetadata.getCache(20L)).thenReturn(point);

        service.forwardTelemetry("01020304", List.of("water", "12.3"));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<PointValue>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(driverSenderService).pointValueSender(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getDeviceId()).isEqualTo(10L);
        assertThat(captor.getValue().get(0).getPointId()).isEqualTo(20L);
        assertThat(captor.getValue().get(0).getRawValue()).isEqualTo("12.3");
    }
}
