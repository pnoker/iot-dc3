/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.driver.service.netty;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.dto.PointDTO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.bo.AttributeBO;
import io.github.pnoker.common.utils.ValueUtil;
import io.github.pnoker.driver.service.netty.tcp.NettyTcpServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class NettyServerHandler {

    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private PointMetadata pointMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    /**
     * 例子, 仅供参考, 请结合自己的实际数据格式进行解析
     */
    public void read(ChannelHandlerContext context, ByteBuf byteBuf) {
        log.info("{}->{}", context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        String deviceName = byteBuf.toString(0, 22, CharsetUtil.CHARSET_ISO_8859_1);
        Long deviceId = Long.valueOf(deviceName);
        String hexKey = ByteBufUtil.hexDump(byteBuf, 22, 1);

        NettyTcpServer.deviceChannelMap.put(deviceId, context.channel());

        List<PointValue> pointValues = new ArrayList<>(16);
        Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointAttributeConfig(deviceId);
        for (Long pointId : pointConfigMap.keySet()) {
            PointDTO point = pointMetadata.getPoint(pointId);
            Map<String, AttributeBO> infoMap = pointConfigMap.get(pointId);
            AttributeBO startAttribute = infoMap.get("start");
            AttributeBO endAttribute = infoMap.get("end");
            int start = startAttribute.getAttributeValue(Integer.class);
            int end = endAttribute.getAttributeValue(Integer.class);

            if (infoMap.get("key").getValue().equals(hexKey)) {
                PointValue pointValue = null;
                switch (point.getPointName()) {
                    case "海拔":
                        float altitude = byteBuf.getFloat(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(altitude),
                                ValueUtil.getValue(point, String.valueOf(altitude)));
                        break;
                    case "速度":
                        double speed = byteBuf.getDouble(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(speed),
                                ValueUtil.getValue(point, String.valueOf(speed)));
                        break;
                    case "液位":
                        long level = byteBuf.getLong(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(level),
                                ValueUtil.getValue(point, String.valueOf(level)));
                        break;
                    case "方向":
                        int direction = byteBuf.getInt(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(direction),
                                ValueUtil.getValue(point, String.valueOf(direction)));
                        break;
                    case "锁定":
                        boolean lock = byteBuf.getBoolean(start);
                        pointValue = new PointValue(deviceId, pointId, String.valueOf(lock),
                                ValueUtil.getValue(point, String.valueOf(lock)));
                        break;
                    case "经纬":
                        String lalo = byteBuf.toString(start, end, CharsetUtil.CHARSET_ISO_8859_1).trim();
                        pointValue = new PointValue(deviceId, pointId, lalo,
                                ValueUtil.getValue(point, lalo));
                        break;
                    default:
                        break;
                }
                if (ObjectUtil.isNotNull(pointValue)) {
                    pointValues.add(pointValue);
                }
            }
        }
        driverSenderService.pointValueSender(pointValues);
    }
}
