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
import io.github.pnoker.driver.api.S7Connector;
import io.github.pnoker.driver.api.S7Serializer;
import io.github.pnoker.driver.api.factory.S7SerializerFactory;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlcS7DriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private S7Serializer serializer;

    @Mock
    private S7Connector connector;

    private PlcS7DriverCustomServiceImpl service;

    private static Map<String, AttributeBO> driverConfig(String host, int port) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("host", AttributeBO.builder().value(host).type(AttributeTypeFlagEnum.STRING).build());
        m.put("port", AttributeBO.builder().value(String.valueOf(port)).type(AttributeTypeFlagEnum.INT).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(int dbNum, int byteOffset, int bitOffset, int blockSize) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("dbNum", AttributeBO.builder().value(String.valueOf(dbNum)).type(AttributeTypeFlagEnum.INT).build());
        m.put("byteOffset",
                AttributeBO.builder().value(String.valueOf(byteOffset)).type(AttributeTypeFlagEnum.INT).build());
        m.put("bitOffset",
                AttributeBO.builder().value(String.valueOf(bitOffset)).type(AttributeTypeFlagEnum.INT).build());
        m.put("blockSize",
                AttributeBO.builder().value(String.valueOf(blockSize)).type(AttributeTypeFlagEnum.INT).build());
        return m;
    }

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(PointTypeFlagEnum type, String pointName) {
        PointBO point = new PointBO();
        point.setId(1L);
        point.setPointTypeFlag(type);
        point.setPointName(pointName);
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
    void setUp() {
        service = new PlcS7DriverCustomServiceImpl(driverMetadata, driverSenderService);
        service.initial();
    }

    @Test
    void scheduleDoesNotReportDeviceStatus() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void scheduleIsSilentWhenNoDevicesRegistered() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void deviceUpdateInvalidatesCachedConnector() throws Exception {
        Object myConnector = primeCachedConnector(123L);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 123L));
        assertThat(connectionMap()).doesNotContainKey(123L);
        assertThat(myConnector).isNotNull();
    }

    @Test
    void deviceDeleteInvalidatesCachedConnector() throws Exception {
        primeCachedConnector(456L);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 456L));
        assertThat(connectionMap()).doesNotContainKey(456L);
    }

    @Test
    void deviceAddDoesNotInvalidateConnector() throws Exception {
        primeCachedConnector(789L);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 789L));
        assertThat(connectionMap()).containsKey(789L);
    }

    @Test
    void pointEventDoesNotTouchConnectionMap() throws Exception {
        primeCachedConnector(100L);
        service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 200L));
        assertThat(connectionMap()).containsKey(100L);
    }

    @Test
    void readDispenseDelegatesToSerializerWithBuiltVariable() throws Exception {
        primeCachedConnector(10L);
        try (MockedStatic<S7SerializerFactory> staticMock = Mockito.mockStatic(S7SerializerFactory.class)) {
            staticMock.when(() -> S7SerializerFactory.buildSerializer(connector)).thenReturn(serializer);
            when(serializer.dispense(any(PlcS7PointVariable.class))).thenReturn((short) 42);

            ReadPointValue readPointValue = service.read(driverConfig("h", 102), pointConfig(1, 0, 0, 2), device(10L),
                    point(PointTypeFlagEnum.INT, "int"));

            assertThat(readPointValue.getValue()).isEqualTo("42");
            verify(serializer, times(1)).dispense(any(PlcS7PointVariable.class));
        }
    }

    @Test
    void readReturnsNullWhenSerializerFails() throws Exception {
        primeCachedConnector(11L);
        try (MockedStatic<S7SerializerFactory> staticMock = Mockito.mockStatic(S7SerializerFactory.class)) {
            staticMock.when(() -> S7SerializerFactory.buildSerializer(connector)).thenReturn(serializer);
            when(serializer.dispense(any(PlcS7PointVariable.class))).thenThrow(new RuntimeException("plc offline"));

            ReadPointValue readPointValue = service.read(driverConfig("h", 102), pointConfig(1, 0, 0, 2), device(11L),
                    point(PointTypeFlagEnum.INT, "int"));

            assertThat(readPointValue).isNull();
        }
    }

    @Test
    void writeStoresIntegerThroughSerializer() throws Exception {
        primeCachedConnector(20L);
        try (MockedStatic<S7SerializerFactory> staticMock = Mockito.mockStatic(S7SerializerFactory.class)) {
            staticMock.when(() -> S7SerializerFactory.buildSerializer(connector)).thenReturn(serializer);

            Boolean ok = service.write(driverConfig("h", 102), pointConfig(1, 4, 0, 2), device(20L),
                    point(PointTypeFlagEnum.INT, "int"), writePointValue("123", PointTypeFlagEnum.INT));

            assertThat(ok).isTrue();
            verify(serializer).store(eq(123), eq(1), eq(4));
        }
    }

    @Test
    void writeReturnsFalseWhenSerializerStoreThrows() throws Exception {
        primeCachedConnector(21L);
        try (MockedStatic<S7SerializerFactory> staticMock = Mockito.mockStatic(S7SerializerFactory.class)) {
            staticMock.when(() -> S7SerializerFactory.buildSerializer(connector)).thenReturn(serializer);
            Mockito.doThrow(new RuntimeException("plc offline")).when(serializer).store(any(), any(int.class),
                    any(int.class));

            Boolean ok = service.write(driverConfig("h", 102), pointConfig(1, 4, 0, 2), device(21L),
                    point(PointTypeFlagEnum.INT, "int"), writePointValue("1", PointTypeFlagEnum.INT));

            assertThat(ok).isFalse();
        }
    }

    @Test
    void plcS7PointVariableMapsAllSupportedTypeCodes() {
        assertThat(new PlcS7PointVariable(1, 0, 0, 1, "bool").getType()).hasToString("BOOL");
        assertThat(new PlcS7PointVariable(1, 0, 0, 1, "byte").getType()).hasToString("BYTE");
        assertThat(new PlcS7PointVariable(1, 0, 0, 2, "int").getType()).hasToString("INT");
        assertThat(new PlcS7PointVariable(1, 0, 0, 4, "dint").getType()).hasToString("DINT");
        assertThat(new PlcS7PointVariable(1, 0, 0, 2, "word").getType()).hasToString("WORD");
        assertThat(new PlcS7PointVariable(1, 0, 0, 4, "dword").getType()).hasToString("DWORD");
        assertThat(new PlcS7PointVariable(1, 0, 0, 4, "real").getType()).hasToString("REAL");
        assertThat(new PlcS7PointVariable(1, 0, 0, 4, "date").getType()).hasToString("DATE");
        assertThat(new PlcS7PointVariable(1, 0, 0, 4, "time").getType()).hasToString("TIME");
        assertThat(new PlcS7PointVariable(1, 0, 0, 8, "datetime").getType()).hasToString("DATE_AND_TIME");
        // Default branch: any unknown code falls back to STRING.
        assertThat(new PlcS7PointVariable(1, 0, 0, 16, "unknown").getType()).hasToString("STRING");
    }

    @Test
    void initialIsIdempotent() {
        assertThatNoException().isThrownBy(() -> service.initial());
    }

    /**
     * Inject a fully-formed MyS7Connector record into the private connectMap so that
     * read / write paths can run without invoking the (untestable) S7ConnectorFactory.
     */
    private Object primeCachedConnector(Long deviceId) throws Exception {
        Class<?> myType = Class.forName("io.github.pnoker.driver.service.impl.PlcS7DriverCustomServiceImpl$MyS7Connector");
        java.lang.reflect.Constructor<?> ctor = myType.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        myType.getMethod("setLock", ReentrantReadWriteLock.class).invoke(instance, new ReentrantReadWriteLock());
        myType.getMethod("setConnector", S7Connector.class).invoke(instance, connector);
        connectionMap().put(deviceId, instance);
        return instance;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map connectionMap() throws Exception {
        Field field = PlcS7DriverCustomServiceImpl.class.getDeclaredField("connectMap");
        field.setAccessible(true);
        return (Map) field.get(service);
    }

}
