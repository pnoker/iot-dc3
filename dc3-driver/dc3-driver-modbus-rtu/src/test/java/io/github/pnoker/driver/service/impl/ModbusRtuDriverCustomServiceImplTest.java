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
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.locator.BaseLocator;
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
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModbusRtuDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private ModbusFactory modbusFactory;

    @Mock
    private ModbusMaster modbusMaster;

    private ModbusRtuDriverCustomServiceImpl service;
    private ModbusFactory previousFactory;

    private static ModbusFactory swapStaticFactory(ModbusFactory replacement) throws Exception {
        Field field = ModbusRtuDriverCustomServiceImpl.class.getDeclaredField("modbusFactory");
        field.setAccessible(true);
        ModbusFactory previous = (ModbusFactory) field.get(null);
        field.set(null, replacement);
        return previous;
    }

    private static Map<String, AttributeBO> driverConfig() {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("port", AttributeBO.builder().value("/dev/ttyS0").type(AttributeTypeFlagEnum.STRING).build());
        m.put("baudRate", AttributeBO.builder().value("9600").type(AttributeTypeFlagEnum.INT).build());
        m.put("dataBits", AttributeBO.builder().value("8").type(AttributeTypeFlagEnum.INT).build());
        m.put("stopBits", AttributeBO.builder().value("1").type(AttributeTypeFlagEnum.INT).build());
        m.put("parity", AttributeBO.builder().value("0").type(AttributeTypeFlagEnum.INT).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(int slaveId, int functionCode, int offset) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("slaveId", AttributeBO.builder().value(String.valueOf(slaveId)).type(AttributeTypeFlagEnum.INT).build());
        m.put("functionCode", AttributeBO.builder().value(String.valueOf(functionCode)).type(AttributeTypeFlagEnum.INT).build());
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

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(op);
        event.setId(id);
        return event;
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new ModbusRtuDriverCustomServiceImpl(driverMetadata, driverSenderService);
        previousFactory = swapStaticFactory(modbusFactory);
        service.initial();
    }

    @AfterEach
    void tearDown() throws Exception {
        swapStaticFactory(previousFactory);
    }

    @Test
    void readDispatchesByFunctionCode() throws Exception {
        when(modbusFactory.createRtuMaster(any(JSerialCommWrapper.class))).thenReturn(modbusMaster);
        when(modbusMaster.getValue(any(BaseLocator.class))).thenReturn(true, 42);

        assertThat(service.read(driverConfig(), pointConfig(1, 1, 0), device(10L),
                point(PointTypeFlagEnum.BOOLEAN)).getValue()).isEqualTo("true");
        assertThat(service.read(driverConfig(), pointConfig(1, 3, 0), device(10L),
                point(PointTypeFlagEnum.INT)).getValue()).isEqualTo("42");

        verify(modbusFactory, times(1)).createRtuMaster(any(JSerialCommWrapper.class));
    }

    @Test
    void readUnsupportedFunctionCodeThrows() throws Exception {
        when(modbusFactory.createRtuMaster(any(JSerialCommWrapper.class))).thenReturn(modbusMaster);

        assertThatThrownBy(() -> service.read(driverConfig(), pointConfig(1, 99, 0), device(10L),
                point(PointTypeFlagEnum.INT))).isInstanceOf(UnSupportException.class);
        verify(modbusMaster, never()).getValue(any(BaseLocator.class));
    }

    @Test
    void readTransportFailureInvalidatesConnection() throws Exception {
        when(modbusFactory.createRtuMaster(any(JSerialCommWrapper.class))).thenReturn(modbusMaster);
        when(modbusMaster.getValue(any(BaseLocator.class))).thenThrow(new ModbusTransportException("serial down"));

        assertThatThrownBy(() -> service.read(driverConfig(), pointConfig(1, 3, 0), device(20L),
                point(PointTypeFlagEnum.INT))).isInstanceOf(ReadPointException.class)
                .hasMessageContaining("serial down");
        verify(modbusMaster).destroy();
    }

    @Test
    void connectionFailureDestroysMaster() throws Exception {
        when(modbusFactory.createRtuMaster(any(JSerialCommWrapper.class))).thenReturn(modbusMaster);
        doThrow(new ModbusInitException("offline")).when(modbusMaster).init();

        assertThatThrownBy(() -> service.read(driverConfig(), pointConfig(1, 1, 0), device(10L),
                point(PointTypeFlagEnum.INT))).isInstanceOf(ConnectorException.class)
                .hasMessageContaining("offline");
        verify(modbusMaster).destroy();
    }

    @Test
    void deviceUpdateInvalidatesCachedConnection() throws Exception {
        when(modbusFactory.createRtuMaster(any(JSerialCommWrapper.class))).thenReturn(modbusMaster);
        when(modbusMaster.getValue(any(BaseLocator.class))).thenReturn(true);

        service.read(driverConfig(), pointConfig(1, 1, 0), device(30L), point(PointTypeFlagEnum.BOOLEAN));
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 30L));
        service.read(driverConfig(), pointConfig(1, 1, 0), device(30L), point(PointTypeFlagEnum.BOOLEAN));

        verify(modbusFactory, times(2)).createRtuMaster(any(JSerialCommWrapper.class));
        verify(modbusMaster).destroy();
    }
}
