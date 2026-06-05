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

package io.github.pnoker.driver.service.netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * Netty-based UDP server for listening to incoming UDP datagrams.
 * <p>
 * This server starts a UDP listener on the specified port and handles incoming datagram
 * packets with a write timeout of 30 seconds.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyUdpServer {

    private static final String PROTOCOL = "udp";
    private final NettyUdpServerHandler nettyUdpServerHandler;

    /**
     * Starts the UDP server on the specified port.
     *
     * @param port The port number to listen on
     */
    @SneakyThrows
    public void start(int port) {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new WriteTimeoutHandler(30), nettyUdpServerHandler);
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            log.info("Driver listener started, protocol={}, port={}", PROTOCOL,  port);
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
            log.info("Driver listener stopped, protocol={}, port={}", PROTOCOL,  port);
        }
    }

}
