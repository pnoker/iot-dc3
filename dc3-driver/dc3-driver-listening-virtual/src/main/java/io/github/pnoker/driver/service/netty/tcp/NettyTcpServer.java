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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty-based TCP server for handling device connections and data exchange.
 * <p>
 * Maintains a mapping of device IDs to Netty channels for bi-directional communication
 * with connected devices.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
public class NettyTcpServer {

    /**
     * Mapping of device IDs to Netty channels.
     * <p>
     * Used to store and retrieve the communication channel for each device. This enables
     * sending data back to specific devices when needed.
     * </p>
     */
    private static final Map<Long, Channel> DEVICE_CHANNEL_MAP = new ConcurrentHashMap<>(16);

    public static void registerDeviceChannel(Long deviceId, Channel channel) {
        DEVICE_CHANNEL_MAP.put(deviceId, channel);
    }

    public static Channel getDeviceChannel(Long deviceId) {
        return DEVICE_CHANNEL_MAP.get(deviceId);
    }

    public static void clearDeviceChannels() {
        DEVICE_CHANNEL_MAP.clear();
    }

    /**
     * Starts the TCP server on the specified port.
     *
     * @param port The port number to listen on
     */
    @SneakyThrows
    public void start(int port) {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new StringEncoder())
                                    .addLast(new ByteArrayEncoder())
                                    .addLast(new WriteTimeoutHandler(30), new NettyTcpServerHandler(null));
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            log.info("Driver listener started, protocol=tcp, port={}", port);
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
            log.info("Driver listener stopped, protocol=tcp, port={}", port);
        }
    }

}
