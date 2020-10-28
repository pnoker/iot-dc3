/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.service.netty.udp;

import cn.hutool.core.util.CharsetUtil;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.model.Point;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.sdk.util.DriverUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class NettyUdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static NettyUdpServerHandler nettyUdpServerHandler;

    @PostConstruct
    public void init() {
        nettyUdpServerHandler = this;
    }

    @Resource
    private DriverService driverService;
    @Resource
    private DriverContext driverContext;

    @Override
    @SneakyThrows
    public void channelRead0(ChannelHandlerContext context, DatagramPacket msg) {
        ByteBuf byteBuf = msg.content();
        log.info("{}->{}", context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        String deviceName = byteBuf.toString(0, 22, CharsetUtil.CHARSET_ISO_8859_1);
        Long deviceId = nettyUdpServerHandler.driverContext.getDeviceIdByName(deviceName);
        String hexKey = ByteBufUtil.hexDump(byteBuf, 22, 1);

        List<PointValue> pointValues = new ArrayList<>();
        Map<Long, Map<String, AttributeInfo>> pointInfoMap = nettyUdpServerHandler.driverContext.getDevicePointInfoMap().get(deviceId);
        for (Long pointId : pointInfoMap.keySet()) {
            Point point = nettyUdpServerHandler.driverContext.getDevicePoint(deviceId, pointId);
            Map<String, AttributeInfo> infoMap = pointInfoMap.get(pointId);
            int start = DriverUtils.value(infoMap.get("start").getType(), infoMap.get("start").getValue());
            int end = DriverUtils.value(infoMap.get("end").getType(), infoMap.get("end").getValue());

            if (infoMap.get("key").getValue().equals(hexKey)) {
                PointValue pointValue = null;
                switch (point.getName()) {
                    case "海拔":
                        float altitude = byteBuf.getFloat(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(altitude),
                                nettyUdpServerHandler.driverService.convertValue(deviceId, pointId, String.valueOf(altitude)));
                        break;
                    case "速度":
                        double speed = byteBuf.getDouble(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(speed),
                                nettyUdpServerHandler.driverService.convertValue(deviceId, pointId, String.valueOf(speed)));
                        break;
                    case "液位":
                        long level = byteBuf.getLong(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(level),
                                nettyUdpServerHandler.driverService.convertValue(deviceId, pointId, String.valueOf(level)));
                        break;
                    case "方向":
                        int direction = byteBuf.getInt(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(direction),
                                nettyUdpServerHandler.driverService.convertValue(deviceId, pointId, String.valueOf(direction)));
                        break;
                    case "锁定":
                        boolean lock = byteBuf.getBoolean(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(lock),
                                nettyUdpServerHandler.driverService.convertValue(deviceId, pointId, String.valueOf(lock)));
                        break;
                    case "经纬":
                        String lalo = byteBuf.toString(start, end, CharsetUtil.CHARSET_ISO_8859_1).trim();
                        pointValue = new PointValue(deviceId, pointId, lalo,
                                nettyUdpServerHandler.driverService.convertValue(deviceId, pointId, lalo));
                        break;
                    default:
                        break;
                }
                if (null != pointValue) {
                    pointValues.add(pointValue);
                }
            }
        }
        nettyUdpServerHandler.driverService.pointValueSender(pointValues);
    }

    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        log.debug(throwable.getMessage());
        context.close();
    }

}