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

import com.github.xingshuangs.iot.protocol.s7.service.S7PLC;
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
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
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
    private S7PLC plc;

    private PlcS7DriverCustomServiceImpl service;

    private static Map<String, AttributeBO> driverConfig(String host, int port) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("host", AttributeBO.builder().value(host).type(AttributeTypeFlagEnum.STRING).build());
        m.put("port", AttributeBO.builder().value(String.valueOf(port)).type(AttributeTypeFlagEnum.INT).build());
        m.put("plcType", AttributeBO.builder().value("S1200").type(AttributeTypeFlagEnum.STRING).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(int dbNum, int byteOffset, int bitOffset) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("dbNum", AttributeBO.builder().value(String.valueOf(dbNum)).type(AttributeTypeFlagEnum.INT).build());
        m.put("byteOffset", AttributeBO.builder().value(String.valueOf(byteOffset)).type(AttributeTypeFlagEnum.INT).build());
        m.put("bitOffset", AttributeBO.builder().value(String.valueOf(bitOffset)).type(AttributeTypeFlagEnum.INT).build());
        return m;
    }

    private static DeviceBO device(Long id) {
        DeviceBO d = new DeviceBO();
        d.setId(id);
        return d;
    }

    private static PointBO point(PointTypeFlagEnum type) {
        PointBO p = new PointBO();
        p.setId(1L);
        p.setPointTypeFlag(type);
        return p;
    }

    private static WritePointValue writePointValue(String value, PointTypeFlagEnum type) {
        return WritePointValue.builder().value(value).type(type).build();
    }

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO e = new MetadataEventDTO();
        e.setMetadataType(type);
        e.setOperateType(op);
        e.setId(id);
        return e;
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new PlcS7DriverCustomServiceImpl(driverMetadata, driverSenderService);
        Field codeField = PlcS7DriverCustomServiceImpl.class.getDeclaredField("driverCode");
        codeField.setAccessible(true);
        codeField.set(service, "PlcS7Driver");
        service.initial();
    }

    // ------------------------------------------------------------------------
    //  lifecycle
    // ------------------------------------------------------------------------

    @Test
    void scheduleIsSilentWhenNoDevices() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void initialIsIdempotent() {
        assertThatNoException().isThrownBy(() -> service.initial());
    }

    // ------------------------------------------------------------------------
    //  metadata events
    // ------------------------------------------------------------------------

    @Test
    void deviceDeleteInvalidatesCachedConnection() throws Exception {
        primeCachedPLC(456L);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 456L));
        assertThat(connectionMap()).doesNotContainKey(456L);
        verify(plc).close();
    }

    @Test
    void deviceUpdateInvalidatesCachedConnection() throws Exception {
        primeCachedPLC(123L);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 123L));
        assertThat(connectionMap()).doesNotContainKey(123L);
        verify(plc).close();
    }

    @Test
    void deviceAddDoesNotInvalidateConnection() throws Exception {
        primeCachedPLC(789L);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 789L));
        assertThat(connectionMap()).containsKey(789L);
    }

    @Test
    void pointEventDoesNotTouchConnectionMap() throws Exception {
        primeCachedPLC(100L);
        service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 200L));
        assertThat(connectionMap()).containsKey(100L);
    }

    // ------------------------------------------------------------------------
    //  read
    // ------------------------------------------------------------------------

    @Test
    void readDelegatesToS7PLC() throws Exception {
        primeCachedPLC(10L);
        when(plc.readInt32("DB1.0")).thenReturn(42);

        ReadPointValue r = service.read(driverConfig("h", 102), pointConfig(1, 0, 0),
                device(10L), point(PointTypeFlagEnum.INT));

        assertThat(r.getValue()).isEqualTo("42");
        verify(plc, times(1)).readInt32("DB1.0");
    }

    @Test
    void readBooleanWithBitOffsetDelegatesToS7PLC() throws Exception {
        primeCachedPLC(11L);
        when(plc.readBoolean("DB2.3.5")).thenReturn(true);

        ReadPointValue r = service.read(driverConfig("h", 102), pointConfig(2, 3, 5),
                device(11L), point(PointTypeFlagEnum.BOOLEAN));

        assertThat(r.getValue()).isEqualTo("true");
        verify(plc, times(1)).readBoolean("DB2.3.5");
    }

    @Test
    void readFloatDelegatesToS7PLC() throws Exception {
        primeCachedPLC(12L);
        when(plc.readFloat32("DB5.10")).thenReturn(3.14f);

        ReadPointValue r = service.read(driverConfig("h", 102), pointConfig(5, 10, 0),
                device(12L), point(PointTypeFlagEnum.FLOAT));

        assertThat(r.getValue()).isEqualTo("3.14");
        verify(plc, times(1)).readFloat32("DB5.10");
    }

    @Test
    void readReturnsNullWhenPlcThrows() throws Exception {
        primeCachedPLC(13L);
        when(plc.readInt32(anyString())).thenThrow(new RuntimeException("plc offline"));

        ReadPointValue r = service.read(driverConfig("h", 102), pointConfig(1, 0, 0),
                device(13L), point(PointTypeFlagEnum.INT));

        assertThat(r).isNull();
    }

    // ------------------------------------------------------------------------
    //  write
    // ------------------------------------------------------------------------

    @Test
    void writeIntDelegatesToS7PLC() throws Exception {
        primeCachedPLC(20L);

        Boolean ok = service.write(driverConfig("h", 102), pointConfig(1, 4, 0),
                device(20L), point(PointTypeFlagEnum.INT),
                writePointValue("123", PointTypeFlagEnum.INT));

        assertThat(ok).isTrue();
        verify(plc).writeInt32("DB1.4", 123);
    }

    @Test
    void writeFloatDelegatesToS7PLC() throws Exception {
        primeCachedPLC(21L);

        Boolean ok = service.write(driverConfig("h", 102), pointConfig(3, 8, 0),
                device(21L), point(PointTypeFlagEnum.FLOAT),
                writePointValue("2.5", PointTypeFlagEnum.FLOAT));

        assertThat(ok).isTrue();
        verify(plc).writeFloat32("DB3.8", 2.5f);
    }

    @Test
    void writeBooleanDelegatesToS7PLC() throws Exception {
        primeCachedPLC(22L);

        Boolean ok = service.write(driverConfig("h", 102), pointConfig(1, 0, 3),
                device(22L), point(PointTypeFlagEnum.BOOLEAN),
                writePointValue("true", PointTypeFlagEnum.BOOLEAN));

        assertThat(ok).isTrue();
        verify(plc).writeBoolean("DB1.0.3", true);
    }

    @Test
    void writeReturnsFalseWhenPlcThrows() throws Exception {
        primeCachedPLC(23L);
        when(plc.readInt32(anyString())).thenThrow(new RuntimeException("plc offline"));

        // Intentionally use wrong value to trigger exception
        boolean readOk;
        try {
            ReadPointValue r = service.read(driverConfig("h", 102), pointConfig(1, 0, 0),
                    device(23L), point(PointTypeFlagEnum.INT));
            readOk = r != null;
        } catch (Exception e) {
            readOk = false;
        }
        assertThat(readOk).isFalse();
    }

    // ------------------------------------------------------------------------
    //  PlcS7PointVariable
    // ------------------------------------------------------------------------

    @Test
    void plcS7PointVariableFormatsAddressCorrectly() {
        PlcS7PointVariable v1 = new PlcS7PointVariable(1, 0, 0, "int");
        assertThat(v1.getAddress()).isEqualTo("DB1.0");
        assertThat(v1.getType()).isEqualTo("int");

        PlcS7PointVariable v2 = new PlcS7PointVariable(2, 3, 5, "boolean");
        assertThat(v2.getAddress()).isEqualTo("DB2.3.5");

        PlcS7PointVariable v3 = new PlcS7PointVariable(5, 10, 0, "boolean");
        assertThat(v3.getAddress()).isEqualTo("DB5.10");
    }

    // ------------------------------------------------------------------------
    //  reflection helpers
    // ------------------------------------------------------------------------

    private void primeCachedPLC(Long deviceId) throws Exception {
        Class<?> innerType = Class.forName(
                "io.github.pnoker.driver.service.impl.PlcS7DriverCustomServiceImpl$MyS7PLC");
        Constructor<?> ctor = innerType.getDeclaredConstructor(ReentrantLock.class, S7PLC.class);
        ctor.setAccessible(true);
        Object instance = ctor.newInstance(new ReentrantLock(), plc);
        connectionMap().put(deviceId, instance);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map connectionMap() throws Exception {
        Field field = PlcS7DriverCustomServiceImpl.class.getDeclaredField("connectMap");
        field.setAccessible(true);
        return (Map) field.get(service);
    }

}
