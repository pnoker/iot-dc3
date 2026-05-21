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

import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import org.jinterop.dcom.core.JIVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.UnknownGroupException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpcDaDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private OpcDaDriverCustomServiceImpl service;

    private static Map<String, AttributeBO> pointConfig(String group, String tag) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("group", AttributeBO.builder().value(group).type(AttributeTypeFlagEnum.STRING).build());
        m.put("tag", AttributeBO.builder().value(tag).type(AttributeTypeFlagEnum.STRING).build());
        return m;
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
        service = new OpcDaDriverCustomServiceImpl(driverMetadata, driverSenderService);
        service.initial();
    }

    @Test
    void initialAllocatesEmptyConnectionMap() {
        assertThatNoException().isThrownBy(() -> service.initial());
    }

    @Test
    void scheduleSendsOnlineForEveryAttachedDevice() {
        when(driverMetadata.getDeviceIds()).thenReturn(Set.of(1L, 2L));
        service.schedule();
        verify(driverSenderService).deviceStatusSender(eq(1L), eq(DeviceStatusEnum.ONLINE), eq(25),
                eq(TimeUnit.SECONDS));
        verify(driverSenderService).deviceStatusSender(eq(2L), eq(DeviceStatusEnum.ONLINE), eq(25),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void scheduleIsSilentWhenNoDevicesRegistered() {
        when(driverMetadata.getDeviceIds()).thenReturn(Set.of());
        service.schedule();
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void deviceUpdateInvalidatesCachedServer() throws Exception {
        Server cached = Mockito.mock(Server.class);
        connectionMap().put(123L, cached);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, 123L));
        assertThat(connectionMap()).doesNotContainKey(123L);
    }

    @Test
    void deviceDeleteInvalidatesCachedServer() throws Exception {
        Server cached = Mockito.mock(Server.class);
        connectionMap().put(456L, cached);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, 456L));
        assertThat(connectionMap()).doesNotContainKey(456L);
    }

    @Test
    void deviceAddDoesNotTouchCachedServer() throws Exception {
        Server cached = Mockito.mock(Server.class);
        connectionMap().put(789L, cached);
        service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 789L));
        assertThat(connectionMap()).containsKey(789L);
    }

    @Test
    void pointEventDoesNotTouchConnectionMap() throws Exception {
        Server cached = Mockito.mock(Server.class);
        connectionMap().put(100L, cached);
        service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 200L));
        assertThat(connectionMap()).containsKey(100L);
    }

    @Test
    void getItemReusesExistingGroupWhenFound() throws Exception {
        Server server = Mockito.mock(Server.class);
        Group group = Mockito.mock(Group.class);
        Item item = Mockito.mock(Item.class);
        when(server.findGroup("g1")).thenReturn(group);
        when(group.addItem("tag.x")).thenReturn(item);

        Item resolved = service.getItem(server, pointConfig("g1", "tag.x"));

        assertThat(resolved).isSameAs(item);
        verify(server).findGroup("g1");
        verify(server, Mockito.never()).addGroup(any(String.class));
    }

    @Test
    void getItemAddsNewGroupWhenNotFound() throws Exception {
        Server server = Mockito.mock(Server.class);
        Group group = Mockito.mock(Group.class);
        Item item = Mockito.mock(Item.class);
        when(server.findGroup("g2")).thenThrow(new UnknownGroupException("g2"));
        when(server.addGroup("g2")).thenReturn(group);
        when(group.addItem("tag.y")).thenReturn(item);

        Item resolved = service.getItem(server, pointConfig("g2", "tag.y"));

        assertThat(resolved).isSameAs(item);
        verify(server).addGroup("g2");
    }

    @Test
    void readItemConvertsShortVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_I2);
        when(variant.getObjectAsShort()).thenReturn((short) 7);

        assertThat(service.readItem(item)).isEqualTo("7");
    }

    @Test
    void readItemConvertsIntVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_I4);
        when(variant.getObjectAsInt()).thenReturn(42);

        assertThat(service.readItem(item)).isEqualTo("42");
    }

    @Test
    void readItemConvertsLongVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_I8);
        when(variant.getObjectAsLong()).thenReturn(99999999999L);

        assertThat(service.readItem(item)).isEqualTo("99999999999");
    }

    @Test
    void readItemConvertsFloatVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_R4);
        when(variant.getObjectAsFloat()).thenReturn(1.25f);

        assertThat(service.readItem(item)).isEqualTo("1.25");
    }

    @Test
    void readItemConvertsDoubleVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_R8);
        when(variant.getObjectAsDouble()).thenReturn(3.14);

        assertThat(service.readItem(item)).isEqualTo("3.14");
    }

    @Test
    void readItemConvertsBooleanVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_BOOL);
        when(variant.getObjectAsBoolean()).thenReturn(true);

        assertThat(service.readItem(item)).isEqualTo("true");
    }

    @Test
    void readItemConvertsBStringVariant() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_BSTR);
        when(variant.getObjectAsString2()).thenReturn("hello");

        assertThat(service.readItem(item)).isEqualTo("hello");
    }

    @Test
    void readItemFallsBackToToStringForUnknownType() throws Exception {
        Item item = Mockito.mock(Item.class);
        ItemState state = Mockito.mock(ItemState.class);
        JIVariant variant = Mockito.mock(JIVariant.class);
        when(item.read(false)).thenReturn(state);
        when(state.getValue()).thenReturn(variant);
        when(variant.getType()).thenReturn(JIVariant.VT_UI1);
        when(variant.getObject()).thenReturn(123);

        assertThat(service.readItem(item)).isEqualTo("123");
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Server> connectionMap() throws Exception {
        Field field = OpcDaDriverCustomServiceImpl.class.getDeclaredField("connectMap");
        field.setAccessible(true);
        return (Map<Long, Server>) field.get(service);
    }

}
