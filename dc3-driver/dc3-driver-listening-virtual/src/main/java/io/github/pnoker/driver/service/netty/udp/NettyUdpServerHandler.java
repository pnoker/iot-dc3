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
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 报文处理, 需要视具体情况开发
 * 本驱动中使用报文(设备名称[22]+关键字[1]+海拔[4]+速度[8]+液位[8]+方向[4]+锁定[1]+经纬[21])进行测试使用
 * 4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69 63 65
 * 62
 * 44 C3 E7 5C
 * 40 46 D5 C2 8F 5C 28 F6
 * 00 00 00 00 00 00 00 0C
 * 00 00 00 2D
 * 01
 * 31 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31
 * <p>
 * 使用 sokit 发送以下报文
 * lg:[4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69 63 65 62 44 C3 E7 5C 40 46 D5 C2 8F 5C 28 F6 00 00 00 00 00 00 00 0C 00 00 00 2D 01 31 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31]
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyUdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static NettyUdpServerHandler nettyUdpServerHandler;
    @Resource
    private NettyServerHandler nettyServerHandler;

    @PostConstruct
    public void init() {
        nettyUdpServerHandler = this;
    }

    @Override
    @SneakyThrows
    public void channelRead0(ChannelHandlerContext context, DatagramPacket msg) {
        nettyUdpServerHandler.nettyServerHandler.read(context, msg.content());
    }

    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        log.debug(throwable.getMessage());
        context.disconnect();
    }

}