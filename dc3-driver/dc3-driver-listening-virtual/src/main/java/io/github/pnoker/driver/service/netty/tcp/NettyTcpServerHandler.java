/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.driver.service.netty.tcp;

import io.github.pnoker.driver.service.netty.NettyServerHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 报文处理，需要视具体情况开发
 * 本驱动中使用报文（设备名称[22]+关键字[1]+海拔[4]+速度[8]+液位[8]+方向[4]+锁定[1]+经纬[21]）进行测试使用
 * 4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69 63 65
 * 62
 * ‭44 C3 E7 5C‬
 * ‭40 46 D5 C2 8F 5C 28 F6‬
 * 00 00 00 00 00 00 00 0C
 * 00 00 00 2D
 * 01
 * 31 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31
 * <p>
 * 使用 sokit 发送以下报文
 * lg:[4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69 63 65 62 44 C3 E7 5C 40 46 D5 C2 8F 5C 28 F6 00 00 00 00 00 00 00 0C 00 00 00 2D 01 31 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31]
 *
 * @author pnoker
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyTcpServerHandler extends ChannelInboundHandlerAdapter {
    private static NettyTcpServerHandler nettyTcpServerHandler;

    @PostConstruct
    public void init() {
        nettyTcpServerHandler = this;
    }

    @Resource
    private NettyServerHandler nettyServerHandler;

    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext context, Object msg) {
        nettyTcpServerHandler.nettyServerHandler.read(context, (ByteBuf) msg);
    }

    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        log.debug(throwable.getMessage());
        context.disconnect();
    }

}