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

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModbusTcpDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private ModbusFactory modbusFactory;

    @Mock
    private ModbusMaster modbusMaster;

    private ModbusTcpDriverCustomServiceImpl service;

    private ModbusFactory previousFactory;

    private static ModbusFactory swapStaticFactory(ModbusFactory replacement) throws Exception {
        Field field = ModbusTcpDriverCustomServiceImpl.class.getDeclaredField("modbusFactory");
        field.setAccessible(true);
        ModbusFactory previous = (ModbusFactory) field.get(null);
        field.set(null, replacement);
        return previous;
    }

    private static Map<String, AttributeBO> driverConfig(String host, int port) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("host", AttributeBO.builder().value(host).type(AttributeTypeFlagEnum.STRING).build());
        m.put("port", AttributeBO.builder().value(String.valueOf(port)).type(AttributeTypeFlagEnum.INT).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(int slaveId, int functionCode, int offset) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("slaveId",
                AttributeBO.builder().value(String.valueOf(slaveId)).type(AttributeTypeFlagEnum.INT).build());
        m.put("functionCode",
                AttributeBO.builder().value(String.valueOf(functionCode)).type(AttributeTypeFlagEnum.INT).build());
        m.put("offset", AttributeBO.builder().value(String.valueOf(offset)).type(AttributeTypeFlagEnum.INT).build());
        return m;
    }

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(PointTypeFlagEnum type) {
        PointBO point = new PointBO();
        point.setId(1L);
        point.setPointTypeFlag(type);
        return point;
    }

    private static WritePointValue writePointValue(String value, PointTypeFlagEnum type) {
        return WritePointValue.builder().value(value).type(type).build();
    }

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(op);
        event.setId(id);
        return event;
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new ModbusTcpDriverCustomServiceImpl(driverMetadata, driverSenderService);
        previousFactory = swapStaticFactory(modbusFactory);
        service.initial();
    }

    @AfterEach
    void restoreFactory() throws Exception {
        swapStaticFactory(previousFactory);
    }

    @Test
    void scheduleDoesNotReportDeviceStatus() {
        service.schedule();
        verify(driverSenderService, never()).deviceStatusSender(any(), any(), anyInt(), any(TimeUnit.class));
    }

    @Test
    void healthReturnsOnlineWhenConnectorIsInitialized() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        when(modbusMaster.isInitialized()).thenReturn(true);

        DeviceHealthState health = service.health(driverConfig("h", 502), device(11L));

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
        verify(modbusMaster).init();
    }

    @Test
    void healthReturnsOfflineWhenConnectionCannotInitialize() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        doThrow(new ModbusInitException("offline")).when(modbusMaster).init();

        DeviceHealthState health = service.health(driverConfig("h", 502), device(11L));

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void deviceUpdateInvalidatesCachedConnection() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        service.read(driverConfig("h", 502), pointConfig(1, 1, 0), device(123L), point(PointTypeFlagEnum.BOOLEAN));
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 123L));
        service.read(driverConfig("h", 502), pointConfig(1, 1, 0), device(123L), point(PointTypeFlagEnum.BOOLEAN));
        verify(modbusFactory, times(2)).createTcpMaster(any(IpParameters.class), eq(true));
    }

    @Test
    void deviceDeleteInvalidatesCachedConnection() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        service.read(driverConfig("h", 502), pointConfig(1, 1, 0), device(456L), point(PointTypeFlagEnum.BOOLEAN));
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 456L));
        service.read(driverConfig("h", 502), pointConfig(1, 1, 0), device(456L), point(PointTypeFlagEnum.BOOLEAN));
        verify(modbusFactory, times(2)).createTcpMaster(any(IpParameters.class), eq(true));
    }

    @Test
    void pointEventDoesNotTouchConnectionMap() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        service.read(driverConfig("h", 502), pointConfig(1, 1, 0), device(789L), point(PointTypeFlagEnum.BOOLEAN));
        service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 999L));
        service.read(driverConfig("h", 502), pointConfig(1, 1, 0), device(789L), point(PointTypeFlagEnum.BOOLEAN));
        verify(modbusFactory, times(1)).createTcpMaster(any(IpParameters.class), eq(true));
    }

    @Test
    void firstReadCachesNewConnection() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        when(modbusMaster.getValue(any(BaseLocator.class))).thenReturn(true);

        service.read(driverConfig("host", 1502), pointConfig(1, 1, 0), device(1L), point(PointTypeFlagEnum.BOOLEAN));
        service.read(driverConfig("host", 1502), pointConfig(1, 1, 0), device(1L), point(PointTypeFlagEnum.BOOLEAN));

        verify(modbusFactory, times(1)).createTcpMaster(any(IpParameters.class), eq(true));
        verify(modbusMaster, times(1)).init();
    }

    @Test
    void connectionFailureThrowsConnectorException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        doThrow(new ModbusInitException("offline")).when(modbusMaster).init();

        assertThatThrownBy(() -> service.read(driverConfig("host", 1502), pointConfig(1, 1, 0), device(7L),
                point(PointTypeFlagEnum.INT))).isInstanceOf(ConnectorException.class)
                .hasMessageContaining("offline");
        verify(modbusMaster).destroy();
    }

    @Test
    void readDispatchesByFunctionCode() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        when(modbusMaster.getValue(any(BaseLocator.class))).thenReturn(true, false, 42, 7);

        assertThat(
                service.read(driverConfig("h", 1), pointConfig(1, 1, 0), device(2L), point(PointTypeFlagEnum.BOOLEAN))
                        .getValue())
                .isEqualTo("true");
        assertThat(
                service.read(driverConfig("h", 1), pointConfig(1, 2, 0), device(2L), point(PointTypeFlagEnum.BOOLEAN))
                        .getValue())
                .isEqualTo("false");
        assertThat(service.read(driverConfig("h", 1), pointConfig(1, 3, 0), device(2L), point(PointTypeFlagEnum.INT))
                .getValue()).isEqualTo("42");
        assertThat(service.read(driverConfig("h", 1), pointConfig(1, 4, 0), device(2L), point(PointTypeFlagEnum.INT))
                .getValue()).isEqualTo("7");
    }

    @Test
    void readUnsupportedFunctionCodeThrows() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        // Function code 99 is not handled.
        assertThatThrownBy(() -> service.read(driverConfig("h", 1), pointConfig(1, 99, 0),
                device(2L), point(PointTypeFlagEnum.INT))).isInstanceOf(UnSupportException.class)
                .hasMessageContaining("function code");
        verify(modbusMaster, never()).getValue(any(BaseLocator.class));
    }

    @Test
    void readTransportFailureBubblesAsReadPointException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        when(modbusMaster.getValue(any(BaseLocator.class))).thenThrow(new ModbusTransportException("rs485 down"));

        assertThatThrownBy(() -> service.read(driverConfig("h", 1), pointConfig(1, 3, 0), device(3L),
                point(PointTypeFlagEnum.INT))).isInstanceOf(ReadPointException.class)
                .hasMessageContaining("rs485 down");
        verify(modbusMaster).destroy();
    }

    @Test
    void readErrorResponseBubblesAsReadPointException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        com.serotonin.modbus4j.msg.ModbusResponse errorResponse =
                org.mockito.Mockito.mock(com.serotonin.modbus4j.msg.ModbusResponse.class);
        when(errorResponse.getExceptionMessage()).thenReturn("illegal data address");
        when(modbusMaster.getValue(any(BaseLocator.class))).thenThrow(new ErrorResponseException(null, errorResponse));

        assertThatThrownBy(() -> service.read(driverConfig("h", 1), pointConfig(1, 4, 0), device(3L),
                point(PointTypeFlagEnum.INT))).isInstanceOf(ReadPointException.class)
                .hasMessageContaining("illegal data address");
    }

    @Test
    void writeCoilSucceedsWhenResponseIsNotException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        WriteCoilResponse response = org.mockito.Mockito.mock(WriteCoilResponse.class);
        when(response.isException()).thenReturn(false);
        when(modbusMaster.send(any(com.serotonin.modbus4j.msg.WriteCoilRequest.class))).thenReturn(response);

        Boolean ok = service.write(driverConfig("h", 1), pointConfig(1, 1, 0), device(7L),
                point(PointTypeFlagEnum.BOOLEAN), writePointValue("true", PointTypeFlagEnum.BOOLEAN));
        assertThat(ok).isTrue();
    }

    @Test
    void writeCoilReturnsFalseWhenResponseIsException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        WriteCoilResponse response = org.mockito.Mockito.mock(WriteCoilResponse.class);
        when(response.isException()).thenReturn(true);
        when(modbusMaster.send(any(com.serotonin.modbus4j.msg.WriteCoilRequest.class))).thenReturn(response);

        Boolean ok = service.write(driverConfig("h", 1), pointConfig(1, 1, 0), device(7L),
                point(PointTypeFlagEnum.BOOLEAN), writePointValue("false", PointTypeFlagEnum.BOOLEAN));
        assertThat(ok).isFalse();
    }

    @Test
    void writeCoilTransportFailureThrowsWritePointException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        when(modbusMaster.send(any(com.serotonin.modbus4j.msg.WriteCoilRequest.class)))
                .thenThrow(new ModbusTransportException("transport reset"));

        assertThatThrownBy(() -> service.write(driverConfig("h", 1), pointConfig(1, 1, 0), device(7L),
                point(PointTypeFlagEnum.BOOLEAN), writePointValue("true", PointTypeFlagEnum.BOOLEAN)))
                .isInstanceOf(WritePointException.class)
                .hasMessageContaining("transport reset");
        verify(modbusMaster).destroy();
    }

    @Test
    void writeHoldingRegisterReturnsTrueOnSuccess() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);

        Boolean ok = service.write(driverConfig("h", 1), pointConfig(1, 3, 0), device(8L),
                point(PointTypeFlagEnum.FLOAT), writePointValue("3.14", PointTypeFlagEnum.FLOAT));
        assertThat(ok).isTrue();
        verify(modbusMaster).setValue(any(BaseLocator.class), any());
    }

    @Test
    void writeHoldingRegisterTransportFailureThrowsWritePointException() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        doThrow(new ModbusTransportException("offline")).when(modbusMaster).setValue(any(BaseLocator.class), any());

        assertThatThrownBy(() -> service.write(driverConfig("h", 1), pointConfig(1, 3, 0), device(8L),
                point(PointTypeFlagEnum.FLOAT), writePointValue("1.0", PointTypeFlagEnum.FLOAT)))
                .isInstanceOf(WritePointException.class)
                .hasMessageContaining("offline");
    }

    // ── DriverValidator ─────────────────────────────────────────────────

    @Test
    void validateReportsMissingHostAndPortAsErrors() {
        Map<String, AttributeBO> empty = Map.of();
        ValidationReport report = service.validate(empty);
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).hasSize(2);
        assertThat(report.getIssues()).extracting("attributeCode").contains("host", "port");
        report.getIssues().forEach(i -> assertThat(i.getLevel())
                .isEqualTo(ValidationReport.IssueLevel.ERROR));
    }

    @Test
    void validateReportsPortOutOfRange() {
        Map<String, AttributeBO> config = driverConfig("localhost", 99999);
        ValidationReport report = service.validate(config);
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).extracting("attributeCode").contains("port");
    }

    @Test
    void validatePassesWithCompleteConfig() {
        Map<String, AttributeBO> config = driverConfig("192.168.1.10", 502);
        ValidationReport report = service.validate(config);
        assertThat(report.isPassed()).isTrue();
    }

    @Test
    void validatePointReportsMissingAttributes() {
        Map<String, AttributeBO> empty = Map.of();
        ValidationReport report = service.validatePoint(empty,
                point(PointTypeFlagEnum.INT));
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).hasSize(3);
        assertThat(report.getIssues()).extracting("attributeCode")
                .contains("slaveId", "functionCode", "offset");
    }

    @Test
    void validatePointReportsInvalidFunctionCode() {
        Map<String, AttributeBO> config = pointConfig(1, 99, 0);
        ValidationReport report = service.validatePoint(config,
                point(PointTypeFlagEnum.INT));
        assertThat(report.isPassed()).isFalse();
        assertThat(report.getIssues()).extracting("attributeCode")
                .contains("functionCode");
    }

    @Test
    void validatePointPassesWithValidConfig() {
        Map<String, AttributeBO> config = pointConfig(1, 3, 100);
        ValidationReport report = service.validatePoint(config,
                point(PointTypeFlagEnum.INT));
        assertThat(report.isPassed()).isTrue();
    }

    @Test
    void writeUnsupportedFunctionCodeReturnsFalse() throws Exception {
        when(modbusFactory.createTcpMaster(any(IpParameters.class), eq(true))).thenReturn(modbusMaster);
        Boolean ok = service.write(driverConfig("h", 1), pointConfig(1, 4, 0), device(8L),
                point(PointTypeFlagEnum.INT), writePointValue("1", PointTypeFlagEnum.INT));
        assertThat(ok).isFalse();
        verify(modbusMaster, never()).setValue(any(BaseLocator.class), any());
        verify(modbusMaster, never()).send(any(com.serotonin.modbus4j.msg.WriteCoilRequest.class));
    }

}
