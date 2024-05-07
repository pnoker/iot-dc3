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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.CharsetUtil;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.PointMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
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
        long deviceId = Long.parseLong(deviceName);
        DeviceBO device = deviceMetadata.getDevice(deviceId);

        String hexKey = ByteBufUtil.hexDump(byteBuf, 22, 1);
        NettyTcpServer.deviceChannelMap.put(deviceId, context.channel());

        Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointAttributeConfig(deviceId);

        List<PointValue> pointValues = new ArrayList<>(16);
        for (Map.Entry<Long, Map<String, AttributeBO>> entry : pointConfigMap.entrySet()) {
            PointBO point = pointMetadata.getPoint(entry.getKey());
            Map<String, AttributeBO> infoMap = pointConfigMap.get(entry.getKey());
            int start = infoMap.get("start").getValue(Integer.class);
            int end = infoMap.get("end").getValue(Integer.class);

            if (infoMap.get("key").getValue().equals(hexKey)) {
                String value = switch (point.getPointName()) {
                    case "海拔" -> String.valueOf(byteBuf.getFloat(start));
                    case "速度" -> String.valueOf(byteBuf.getDouble(start));
                    case "液位" -> String.valueOf(byteBuf.getLong(start));
                    case "方向" -> String.valueOf(byteBuf.getInt(start));
                    case "锁定" -> String.valueOf(byteBuf.getBoolean(start));
                    case "经纬" -> byteBuf.toString(start, end, CharsetUtil.CHARSET_ISO_8859_1).trim();
                    default -> "";
                };

                if (CharSequenceUtil.isNotEmpty(value)) {
                    pointValues.add(new PointValue(new RValue(device, point, value)));
                }
            }
        }

        driverSenderService.pointValueSender(pointValues);
    }
}
