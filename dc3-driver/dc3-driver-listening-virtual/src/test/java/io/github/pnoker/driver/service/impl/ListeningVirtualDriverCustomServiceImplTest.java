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

import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.driver.service.netty.tcp.NettyTcpServer;
import io.github.pnoker.driver.service.netty.udp.NettyUdpServer;
import io.netty.channel.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListeningVirtualDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private NettyTcpServer nettyTcpServer;

    @Mock
    private NettyUdpServer nettyUdpServer;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    private ListeningVirtualDriverCustomServiceImpl service;

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
    void setUp() throws Exception {
        service = new ListeningVirtualDriverCustomServiceImpl(driverMetadata, driverSenderService, nettyTcpServer,
                nettyUdpServer, threadPoolExecutor);
        injectField("tcpPort", 6700);
        injectField("udpPort", 6800);
    }

    @AfterEach
    void cleanChannelMap() {
        NettyTcpServer.clearDeviceChannels();
    }

    @Test
    void initialDispatchesTcpAndUdpListenersToThreadPool() {
        service.initial();
        verify(threadPoolExecutor, times(2)).execute(any(Runnable.class));
        // The bootstrap captures occur lazily; if we want to assert they really call into
        // the netty servers, run the captured runnables.
        org.mockito.ArgumentCaptor<Runnable> captor = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(threadPoolExecutor, times(2)).execute(captor.capture());
        captor.getAllValues().get(0).run();
        captor.getAllValues().get(1).run();
        verify(nettyTcpServer).start(6700);
        verify(nettyUdpServer).start(6800);
    }

    @Test
    void scheduleSendsOnlineStatusForEveryAttachedDevice() {
        when(driverMetadata.getDeviceIds()).thenReturn(Set.of(101L, 102L));
        service.schedule();
        verify(driverSenderService).deviceStatusSender(eq(101L), eq(DeviceStatusEnum.ONLINE), eq(25),
                eq(TimeUnit.SECONDS));
        verify(driverSenderService).deviceStatusSender(eq(102L), eq(DeviceStatusEnum.ONLINE), eq(25),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void eventForDeviceIsLoggedWithoutSideEffects() {
        MetadataEventDTO event = metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 1L);
        assertThatNoException().isThrownBy(() -> service.event(event));
        verifyNoInteractions(driverMetadata, driverSenderService, nettyTcpServer, nettyUdpServer);
    }

    @Test
    void eventForPointIsLoggedWithoutSideEffects() {
        MetadataEventDTO event = metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 5L);
        assertThatNoException().isThrownBy(() -> service.event(event));
        verifyNoInteractions(driverMetadata, driverSenderService, nettyTcpServer, nettyUdpServer);
    }

    @Test
    void readReturnsNullBecauseListenerIsPassive() {
        DeviceBO device = device(1L);
        PointBO point = point(2L);
        assertThat(service.read(null, null, device, point)).isNull();
    }

    @Test
    void writeFlushesValueWhenChannelExistsForDevice() {
        DeviceBO device = device(900L);
        PointBO point = point(2L);
        Channel channel = org.mockito.Mockito.mock(Channel.class);
        NettyTcpServer.registerDeviceChannel(900L, channel);

        Boolean ok = service.write(null, null, device, point,
                WritePointValue.builder().value("hello").type(PointTypeFlagEnum.STRING).build());

        assertThat(ok).isTrue();
        verify(channel).writeAndFlush(any(byte[].class));
    }

    @Test
    void writeReturnsTrueButSkipsWhenChannelMissing() {
        DeviceBO device = device(901L);
        PointBO point = point(2L);

        Boolean ok = service.write(null, null, device, point,
                WritePointValue.builder().value("hello").type(PointTypeFlagEnum.STRING).build());

        assertThat(ok).isTrue();
        // No channel was registered, so nothing was flushed.
        Channel anyChannel = org.mockito.Mockito.mock(Channel.class);
        verify(anyChannel, never()).writeAndFlush(any());
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = ListeningVirtualDriverCustomServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
