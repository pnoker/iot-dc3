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

import com.github.xingshuangs.iot.protocol.melsec.service.McPLC;
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
import io.github.pnoker.driver.bean.MelsecPointVariable;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MelsecDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private McPLC plc;

    private MelsecDriverCustomServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new MelsecDriverCustomServiceImpl(driverMetadata, driverSenderService);
        Field codeField = MelsecDriverCustomServiceImpl.class.getDeclaredField("driverCode");
        codeField.setAccessible(true);
        codeField.set(service, "MelsecDriver");
        service.initial();
    }

    @Test
    void scheduleIsSilent() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void deviceUpdateInvalidatesCachedConnection() throws Exception {
        primeCachedPLC(10L);

        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 10L));

        assertThat(connectionMap()).doesNotContainKey(10L);
        verify(plc).close();
    }

    @Test
    void readDelegatesToMcPlc() throws Exception {
        primeCachedPLC(11L);
        when(plc.readInt32("D100")).thenReturn(42);

        ReadPointValue value = service.read(driverConfig(), pointConfig("D100", 0),
                device(11L), point(PointTypeFlagEnum.INT));

        assertThat(value.getValue()).isEqualTo("42");
    }

    @Test
    void readFailureInvalidatesCachedConnection() throws Exception {
        primeCachedPLC(12L);
        when(plc.readInt32(anyString())).thenThrow(new RuntimeException("plc offline"));

        ReadPointValue value = service.read(driverConfig(), pointConfig("D100", 0),
                device(12L), point(PointTypeFlagEnum.INT));

        assertThat(value).isNull();
        assertThat(connectionMap()).doesNotContainKey(12L);
        verify(plc).close();
    }

    @Test
    void writeDelegatesToMcPlc() throws Exception {
        primeCachedPLC(13L);

        Boolean ok = service.write(driverConfig(), pointConfig("D100", 0),
                device(13L), point(PointTypeFlagEnum.INT),
                WritePointValue.builder().value("7").type(PointTypeFlagEnum.INT).build());

        assertThat(ok).isTrue();
        verify(plc).writeInt32("D100", 7);
    }

    @Test
    void writeFailureInvalidatesCachedConnection() throws Exception {
        primeCachedPLC(14L);
        doThrow(new RuntimeException("plc offline")).when(plc).writeInt32(anyString(), org.mockito.ArgumentMatchers.anyInt());

        Boolean ok = service.write(driverConfig(), pointConfig("D100", 0),
                device(14L), point(PointTypeFlagEnum.INT),
                WritePointValue.builder().value("7").type(PointTypeFlagEnum.INT).build());

        assertThat(ok).isFalse();
        assertThat(connectionMap()).doesNotContainKey(14L);
        verify(plc).close();
    }

    @Test
    void pointVariableKeepsAddressTypeAndLength() {
        MelsecPointVariable variable = new MelsecPointVariable("D100", "string", 16);

        assertThat(variable.getAddress()).isEqualTo("D100");
        assertThat(variable.getType()).isEqualTo("string");
        assertThat(variable.getLength()).isEqualTo(16);
    }

    private static Map<String, AttributeBO> driverConfig() {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("host", AttributeBO.builder().value("127.0.0.1").type(AttributeTypeFlagEnum.STRING).build());
        m.put("port", AttributeBO.builder().value("5000").type(AttributeTypeFlagEnum.INT).build());
        m.put("series", AttributeBO.builder().value("QnA").type(AttributeTypeFlagEnum.STRING).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(String address, int length) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("address", AttributeBO.builder().value(address).type(AttributeTypeFlagEnum.STRING).build());
        m.put("length", AttributeBO.builder().value(String.valueOf(length)).type(AttributeTypeFlagEnum.INT).build());
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

    private void primeCachedPLC(Long deviceId) throws Exception {
        Class<?> innerType = Class.forName(
                "io.github.pnoker.driver.service.impl.MelsecDriverCustomServiceImpl$MyMcPLC");
        Constructor<?> ctor = innerType.getDeclaredConstructor(ReentrantLock.class, McPLC.class);
        ctor.setAccessible(true);
        Object instance = ctor.newInstance(new ReentrantLock(), plc);
        connectionMap().put(deviceId, instance);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map connectionMap() throws Exception {
        Field field = MelsecDriverCustomServiceImpl.class.getDeclaredField("connectMap");
        field.setAccessible(true);
        return (Map) field.get(service);
    }
}
