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

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.driver.coap.client.CoapClientManager;
import io.github.pnoker.driver.coap.entity.CoapResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoapDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private CoapClientManager coapClientManager;

    private CoapDriverCustomServiceImpl service;

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(Long id) {
        PointBO point = new PointBO();
        point.setId(id);
        return point;
    }

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(op);
        event.setId(id);
        return event;
    }

    @BeforeEach
    void setUp() {
        service = new CoapDriverCustomServiceImpl(driverMetadata, driverSenderService, coapClientManager);
    }

    @Test
    void initialIsNoOp() {
        assertThatNoException().isThrownBy(() -> service.initial());
        verifyNoInteractions(coapClientManager, driverSenderService, driverMetadata);
    }

    @Test
    void scheduleDoesNotReportDeviceStatus() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void deviceAndPointEventsAreLoggedOnly() {
        assertThatNoException().isThrownBy(
                () -> service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 1L)));
        assertThatNoException().isThrownBy(
                () -> service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 2L)));
    }

    @Test
    void readReturnsValueFromCoapGet() {
        Map<String, AttributeBO> driverConfig = new HashMap<>();
        driverConfig.put("deviceHost",
                AttributeBO.builder().value("192.168.1.10").type(AttributeTypeFlagEnum.STRING).build());
        driverConfig.put("devicePort",
                AttributeBO.builder().value("5683").type(AttributeTypeFlagEnum.INT).build());

        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("readPath",
                AttributeBO.builder().value("/sensors/temp").type(AttributeTypeFlagEnum.STRING).build());

        CoapResult mockResponse = CoapResult.builder()
                .statusCode(69)
                .payload("25.3")
                .success(true)
                .build();
        when(coapClientManager.get("coap://192.168.1.10:5683", "/sensors/temp")).thenReturn(mockResponse);

        ReadPointValue result = service.read(driverConfig, pointConfig, device(1L), point(1L));

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("25.3");
    }

    @Test
    void readReturnsNullWhenCoapGetFails() {
        Map<String, AttributeBO> driverConfig = new HashMap<>();
        driverConfig.put("deviceHost",
                AttributeBO.builder().value("192.168.1.10").type(AttributeTypeFlagEnum.STRING).build());
        driverConfig.put("devicePort",
                AttributeBO.builder().value("5683").type(AttributeTypeFlagEnum.INT).build());

        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("readPath",
                AttributeBO.builder().value("/sensors/temp").type(AttributeTypeFlagEnum.STRING).build());

        when(coapClientManager.get(anyString(), anyString())).thenReturn(null);

        ReadPointValue result = service.read(driverConfig, pointConfig, device(1L), point(1L));
        assertThat(result).isNull();
    }

    @Test
    void readUsesDefaultValuesWhenConfigMissing() {
        Map<String, AttributeBO> driverConfig = new HashMap<>();
        Map<String, AttributeBO> pointConfig = new HashMap<>();

        CoapResult mockResponse = CoapResult.builder()
                .statusCode(69)
                .payload("42.0")
                .success(true)
                .build();
        when(coapClientManager.get("coap://localhost:5683", "/sensors")).thenReturn(mockResponse);

        ReadPointValue result = service.read(driverConfig, pointConfig, device(1L), point(1L));

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("42.0");
    }

    @Test
    void writeSendsCoapPutAndReturnsTrueOnSuccess() {
        Map<String, AttributeBO> driverConfig = new HashMap<>();
        driverConfig.put("deviceHost",
                AttributeBO.builder().value("192.168.1.10").type(AttributeTypeFlagEnum.STRING).build());
        driverConfig.put("devicePort",
                AttributeBO.builder().value("5683").type(AttributeTypeFlagEnum.INT).build());

        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("writePath",
                AttributeBO.builder().value("/actuators/relay").type(AttributeTypeFlagEnum.STRING).build());

        CoapResult mockResponse = CoapResult.builder()
                .statusCode(68)
                .success(true)
                .build();
        when(coapClientManager.put("coap://192.168.1.10:5683", "/actuators/relay", "ON")).thenReturn(mockResponse);

        Boolean result = service.write(driverConfig, pointConfig, device(1L), point(1L),
                WritePointValue.builder().value("ON").type(PointTypeFlagEnum.STRING).build());

        assertThat(result).isTrue();
    }

    @Test
    void writeReturnsFalseWhenCoapPutFails() {
        Map<String, AttributeBO> driverConfig = new HashMap<>();
        driverConfig.put("deviceHost",
                AttributeBO.builder().value("192.168.1.10").type(AttributeTypeFlagEnum.STRING).build());
        driverConfig.put("devicePort",
                AttributeBO.builder().value("5683").type(AttributeTypeFlagEnum.INT).build());

        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("writePath",
                AttributeBO.builder().value("/actuators/relay").type(AttributeTypeFlagEnum.STRING).build());

        when(coapClientManager.put(anyString(), anyString(), anyString())).thenReturn(null);

        Boolean result = service.write(driverConfig, pointConfig, device(1L), point(1L),
                WritePointValue.builder().value("ON").type(PointTypeFlagEnum.STRING).build());

        assertThat(result).isFalse();
    }

}
