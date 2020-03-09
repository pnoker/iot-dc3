package com.pnoker.driver.service.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pnoker
 */
@Slf4j
@ChannelHandler.Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    @SneakyThrows
    public void channelActive(ChannelHandlerContext context) {
        log.info("channelActive:{}", context.channel().remoteAddress());
    }

    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext context, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        String hexDump = ByteBufUtil.hexDump(byteBuf);
        log.info("{}->{}", context.channel().remoteAddress(), hexDump);
    }

    @Override
    @SneakyThrows
    public void channelReadComplete(ChannelHandlerContext context) {
    }

    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        log.error("exception:{}", context.channel().remoteAddress());
        context.close();
    }

}