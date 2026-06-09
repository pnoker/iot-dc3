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

package io.github.pnoker.driver.service.netty;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NettyServerHandlerTest {

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private PointMetadata pointMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    private NettyServerHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NettyServerHandler(deviceMetadata, pointMetadata, driverSenderService);
        when(context.channel()).thenReturn(channel);
    }

    @Test
    void coordinateUsesEndOffsetAsExclusiveBound() {
        DeviceBO device = new DeviceBO();
        device.setId(123L);
        when(deviceMetadata.getCache(123L)).thenReturn(device);
        when(deviceMetadata.getPointConfig(123L)).thenReturn(Map.of(1L, Map.of(
                "key", AttributeBO.builder().value("62").type(AttributeTypeEnum.STRING).build(),
                "start", AttributeBO.builder().value("23").type(AttributeTypeEnum.INT).build(),
                "end", AttributeBO.builder().value("28").type(AttributeTypeEnum.INT).build()
        )));
        PointBO point = new PointBO();
        point.setId(1L);
        point.setPointName("coordinate");
        when(pointMetadata.getCache(1L)).thenReturn(point);

        handler.read(context, Unpooled.wrappedBuffer(message("123", (byte) 0x62, "abcde")));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<PointValue>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(driverSenderService).pointValueSender(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getRawValue()).isEqualTo("abcde");
    }

    @Test
    void invalidDeviceIdIsSkipped() {
        handler.read(context, Unpooled.wrappedBuffer(message("not-a-number", (byte) 0x62, "abcde")));

        verify(driverSenderService, never()).pointValueSender(org.mockito.ArgumentMatchers.<List<PointValue>>any());
    }

    @Test
    void malformedPointConfigIsSkipped() {
        DeviceBO device = new DeviceBO();
        device.setId(123L);
        when(deviceMetadata.getCache(123L)).thenReturn(device);
        when(deviceMetadata.getPointConfig(123L)).thenReturn(Map.of(1L, Map.of(
                "key", AttributeBO.builder().value("62").type(AttributeTypeEnum.STRING).build(),
                "start", AttributeBO.builder().value("23").type(AttributeTypeEnum.INT).build()
        )));
        PointBO point = new PointBO();
        point.setId(1L);
        point.setPointName("coordinate");
        when(pointMetadata.getCache(1L)).thenReturn(point);

        handler.read(context, Unpooled.wrappedBuffer(message("123", (byte) 0x62, "abcde")));

        verify(driverSenderService, never()).pointValueSender(org.mockito.ArgumentMatchers.<List<PointValue>>any());
    }

    private byte[] message(String deviceName, byte key, String payload) {
        byte[] deviceBytes = String.format("%-22s", deviceName).getBytes(StandardCharsets.UTF_8);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[deviceBytes.length + 1 + payloadBytes.length];
        System.arraycopy(deviceBytes, 0, bytes, 0, deviceBytes.length);
        bytes[22] = key;
        System.arraycopy(payloadBytes, 0, bytes, 23, payloadBytes.length);
        return bytes;
    }
}
