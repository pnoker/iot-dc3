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

package io.github.pnoker.driver.service.netty.tcp;

import io.github.pnoker.driver.service.netty.NettyServerHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NettyTcpServerHandlerTest {

    @Mock
    private NettyServerHandler nettyServerHandler;

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @AfterEach
    void tearDown() {
        NettyTcpServer.clearDeviceChannels();
    }

    @Test
    void channelReadReleasesInboundByteBuf() {
        NettyTcpServerHandler handler = new NettyTcpServerHandler(nettyServerHandler);
        ByteBuf buffer = Unpooled.buffer(1).writeByte(1);

        handler.channelRead(context, buffer);

        verify(nettyServerHandler).read(context, buffer);
        assertThat(buffer.refCnt()).isZero();
    }

    @Test
    void channelInactiveRemovesRegisteredDeviceChannel() {
        when(context.channel()).thenReturn(channel);
        NettyTcpServer.registerDeviceChannel(10L, channel);
        NettyTcpServerHandler handler = new NettyTcpServerHandler(nettyServerHandler);

        handler.channelInactive(context);

        assertThat(NettyTcpServer.getDeviceChannel(10L)).isNull();
    }
}
