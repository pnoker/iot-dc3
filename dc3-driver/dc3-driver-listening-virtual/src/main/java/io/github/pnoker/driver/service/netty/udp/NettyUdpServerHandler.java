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

import io.github.pnoker.driver.service.netty.NettyServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UDP server handler for processing incoming datagram packets.
 * <p>
 * This handler processes UDP messages according to a specific format: <pre>
 * - Device name: 22 bytes
 * - Keyword: 1 byte
 * - Altitude: 4 bytes (float)
 * - Speed: 8 bytes (double)
 * - Liquid level: 8 bytes (long)
 * - Direction: 4 bytes (int)
 * - Lock status: 1 byte (boolean)
 * - Latitude/longitude: 21 bytes (string)
 * </pre>
 * <p>
 * Example test message (hex): 4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69
 * 63 65 62 44 C3 E7 5C 40 46 D5 C2 8F 5C 28 F6 00 00 00 00 00 00 00 0C 00 00 00 2D 01 31
 * 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class NettyUdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    /**
     * Static self-reference for accessing Spring-injected beans from static context.
     */
    private static NettyUdpServerHandler nettyUdpServerHandler;

    private final NettyServerHandler nettyServerHandler;

    /**
     * Initializes the handler instance after Spring dependency injection.
     */
    @PostConstruct
    public void init() {
        nettyUdpServerHandler = this;
    }

    /**
     * Handles incoming UDP datagram packets.
     *
     * @param context The channel handler context
     * @param msg     The received datagram packet
     */
    @Override
    @SneakyThrows
    public void channelRead0(ChannelHandlerContext context, DatagramPacket msg) {
        nettyUdpServerHandler.nettyServerHandler.read(context, msg.content());
    }

    /**
     * Handles exceptions during UDP packet processing.
     *
     * @param context   The channel handler context
     * @param throwable The exception that occurred
     */
    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        log.warn("Driver channel error, protocol=udp, remoteAddress={}", context.channel().remoteAddress(), throwable);
        context.disconnect();
    }

}
