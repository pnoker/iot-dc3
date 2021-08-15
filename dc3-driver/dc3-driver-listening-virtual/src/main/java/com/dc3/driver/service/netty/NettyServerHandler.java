/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.service.netty;

import cn.hutool.core.util.CharsetUtil;
import com.dc3.common.bean.driver.AttributeInfo;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.model.PointValue;
import com.dc3.common.sdk.bean.driver.DriverContext;
import com.dc3.common.sdk.service.DriverService;
import com.dc3.common.sdk.util.DriverUtils;
import com.dc3.driver.service.netty.tcp.NettyTcpServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class NettyServerHandler {

    @Resource
    private DriverService driverService;
    @Resource
    private DriverContext driverContext;

    public Long getDeviceIdByName(String name) {
        List<Device> values = new ArrayList<>(driverContext.getDriverMetadata().getDeviceMap().values());
        for (int i = 0; i < values.size(); i++) {
            Device device = values.get(i);
            if (device.getName().equals(name)) {
                return device.getId();
            }
        }
        return null;
    }

    public void read(ChannelHandlerContext context, ByteBuf byteBuf) {
        log.info("{}->{}", context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        String deviceName = byteBuf.toString(0, 22, CharsetUtil.CHARSET_ISO_8859_1);
        Long deviceId = getDeviceIdByName(deviceName);
        String hexKey = ByteBufUtil.hexDump(byteBuf, 22, 1);

        //TODO 简单的例子，用于存储channel，然后配合write接口实现向下发送数据
        NettyTcpServer.deviceChannelMap.put(deviceId, context.channel());

        List<PointValue> pointValues = new ArrayList<>(16);
        Map<Long, Map<String, AttributeInfo>> pointInfoMap = driverContext.getDriverMetadata().getPointInfoMap().get(deviceId);
        for (Long pointId : pointInfoMap.keySet()) {
            Point point = driverContext.getPointByDeviceIdAndPointId(deviceId, pointId);
            Map<String, AttributeInfo> infoMap = pointInfoMap.get(pointId);
            int start = DriverUtils.value(infoMap.get("start").getType(), infoMap.get("start").getValue());
            int end = DriverUtils.value(infoMap.get("end").getType(), infoMap.get("end").getValue());

            if (infoMap.get("key").getValue().equals(hexKey)) {
                PointValue pointValue = null;
                switch (point.getName()) {
                    case "海拔":
                        float altitude = byteBuf.getFloat(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(altitude),
                                driverService.convertValue(deviceId, pointId, String.valueOf(altitude)));
                        break;
                    case "速度":
                        double speed = byteBuf.getDouble(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(speed),
                                driverService.convertValue(deviceId, pointId, String.valueOf(speed)));
                        break;
                    case "液位":
                        long level = byteBuf.getLong(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(level),
                                driverService.convertValue(deviceId, pointId, String.valueOf(level)));
                        break;
                    case "方向":
                        int direction = byteBuf.getInt(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(direction),
                                driverService.convertValue(deviceId, pointId, String.valueOf(direction)));
                        break;
                    case "锁定":
                        boolean lock = byteBuf.getBoolean(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(lock),
                                driverService.convertValue(deviceId, pointId, String.valueOf(lock)));
                        break;
                    case "经纬":
                        String lalo = byteBuf.toString(start, end, CharsetUtil.CHARSET_ISO_8859_1).trim();
                        pointValue = new PointValue(deviceId, pointId, lalo,
                                driverService.convertValue(deviceId, pointId, lalo));
                        break;
                    default:
                        break;
                }
                if (null != pointValue) {
                    pointValues.add(pointValue);
                }
            }
        }
        driverService.pointValueSender(pointValues);
    }
}
