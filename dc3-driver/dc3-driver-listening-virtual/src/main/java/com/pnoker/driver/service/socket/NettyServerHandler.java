package com.pnoker.driver.service.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
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
        //连接激活后，可做处理
    }

    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext context, Object data) {
        ByteBuf byteBuf = (ByteBuf) data;
        log.info("received:" + byteBuf.toString(CharsetUtil.UTF_8));
        //根据消息类型回复报文数据
        context.writeAndFlush(data);
    }

    @Override
    @SneakyThrows
    public void channelReadComplete(ChannelHandlerContext context) {
    }

    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        throwable.printStackTrace();
        context.close();
    }

}