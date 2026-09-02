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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;

@ExtendWith(MockitoExtension.class)
class BleDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private BleManagerFactory managerFactory;

    @Mock
    private BluetoothManager bluetoothManager;

    @Mock
    private DeviceGovernor deviceGovernor;

    @Mock
    private CharacteristicGovernor characteristicGovernor;

    private BleDriverCustomServiceImpl service;

    private static AttributeBO attribute(String value, AttributeTypeEnum type) {
        return AttributeBO.builder().value(value).type(type).build();
    }

    private static Map<String, AttributeBO> driverConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("adapterName", attribute("hci0", AttributeTypeEnum.STRING));
        config.put("deviceAddress", attribute("AA:BB:CC:DD:EE:FF", AttributeTypeEnum.STRING));
        return config;
    }

    private static Map<String, AttributeBO> pointConfig(String format) {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("serviceUuid", attribute("service-uuid", AttributeTypeEnum.STRING));
        config.put("characteristicUuid", attribute("characteristic-uuid", AttributeTypeEnum.STRING));
        config.put("readFormat", attribute(format, AttributeTypeEnum.STRING));
        config.put("byteOrder", attribute("BIG", AttributeTypeEnum.STRING));
        return config;
    }

    private static DeviceBO device() {
        DeviceBO device = new DeviceBO();
        device.setId(7L);
        return device;
    }

    private static PointBO point() {
        PointBO point = new PointBO();
        point.setId(9L);
        point.setPointTypeFlag(PointTypeEnum.STRING);
        return point;
    }

    private static Stream<Arguments> encodedValues() {
        return Stream.of(
                Arguments.of("UTF8", "hello".getBytes(StandardCharsets.UTF_8), "hello"),
                Arguments.of("HEX", new byte[] {0x01, (byte) 0xAF}, "01AF"),
                Arguments.of("INT16", new byte[] {0x00, 0x2A}, "42"),
                Arguments.of("UINT16", new byte[] {(byte) 0xFF, (byte) 0xFF}, "65535"),
                Arguments.of("FLOAT", new byte[] {0x3F, (byte) 0xA0, 0x00, 0x00}, "1.25"));
    }

    @BeforeEach
    void setUp() {
        service = new BleDriverCustomServiceImpl(driverMetadata, driverSenderService, managerFactory);
    }

    @Test
    void reportsOfflineUntilNativeManagerIsCreated() {
        assertThat(service.health().getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void initialCreatesNativeManagerAndReportsOnline() {
        when(managerFactory.create()).thenReturn(bluetoothManager);

        service.initial();

        assertThat(service.health().getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
        verify(managerFactory).create();
    }

    @Test
    void validatesDriverAndPointAttributesAtTheirDeclaredBoundaries() {
        ValidationReport validDriver = service.validate(driverConfig());
        ValidationReport validPoint = service.validatePoint(pointConfig("UTF8"), point());
        ValidationReport missingDriver = service.validate(Map.of());
        ValidationReport missingPoint = service.validatePoint(Map.of(), point());

        assertThat(validDriver.isPassed()).isTrue();
        assertThat(validPoint.isPassed()).isTrue();
        assertThat(missingDriver.getIssues())
                .extracting(ValidationReport.AttributeIssue::getAttributeCode)
                .containsExactlyInAnyOrder("adapterName", "deviceAddress");
        assertThat(missingPoint.getIssues())
                .extracting(ValidationReport.AttributeIssue::getAttributeCode)
                .containsExactlyInAnyOrder("serviceUuid", "characteristicUuid");
    }

    @Test
    void deviceHealthReflectsGovernorConnectivity() {
        initializeConnectedDevice();
        when(deviceGovernor.isOnline()).thenReturn(true);
        when(deviceGovernor.isConnected()).thenReturn(true);
        when(characteristicGovernor.read()).thenReturn("value".getBytes(StandardCharsets.UTF_8));
        DeviceBO device = device();

        service.read(driverConfig(), pointConfig("UTF8"), device, point());

        assertThat(service.health(driverConfig(), device).getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
    }

    @ParameterizedTest(name = "decodes {0} payload")
    @MethodSource("encodedValues")
    void readDecodesConfiguredWireFormat(String format, byte[] bytes, String expected) {
        initializeConnectedDevice();
        when(characteristicGovernor.read()).thenReturn(bytes);
        DeviceBO device = device();

        ReadPointValue value = service.read(driverConfig(), pointConfig(format), device, point());

        assertThat(value.getValue()).isEqualTo(expected);
        assertThat(value.getDevice()).isSameAs(device);
    }

    @Test
    void updateEventReleasesAndEvictsCachedDeviceGovernor() {
        initializeConnectedDevice();
        when(characteristicGovernor.read()).thenReturn("value".getBytes(StandardCharsets.UTF_8));
        DeviceBO device = device();
        service.read(driverConfig(), pointConfig("UTF8"), device, point());

        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(MetadataTypeEnum.DEVICE);
        event.setOperateType(MetadataOperateTypeEnum.UPDATE);
        event.setId(device.getId());
        service.event(event);

        verify(deviceGovernor).setConnectionControl(false);
        assertThat(service.health(driverConfig(), device).getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void writeDelegatesUtf8BytesToCharacteristic() {
        initializeConnectedDevice();
        when(characteristicGovernor.write(any(byte[].class))).thenReturn(true);

        boolean written = service.write(
                driverConfig(),
                pointConfig("UTF8"),
                device(),
                point(),
                WritePointValue.builder().type(PointTypeEnum.STRING).value("开启").build());

        assertThat(written).isTrue();
        verify(characteristicGovernor).write("开启".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readRejectsMissingCharacteristicConfiguration() {
        when(managerFactory.create()).thenReturn(bluetoothManager);
        service.initial();

        assertThatThrownBy(() -> service.read(driverConfig(), Map.of(), device(), point()))
                .isInstanceOf(ReadPointException.class)
                .hasMessageContaining("serviceUuid");
    }

    private void initializeConnectedDevice() {
        when(managerFactory.create()).thenReturn(bluetoothManager);
        when(bluetoothManager.getDeviceGovernor(any(URL.class), eq(true))).thenReturn(deviceGovernor);
        when(bluetoothManager.getCharacteristicGovernor(any(URL.class), eq(true)))
                .thenReturn(characteristicGovernor);
        service.initial();
    }
}
