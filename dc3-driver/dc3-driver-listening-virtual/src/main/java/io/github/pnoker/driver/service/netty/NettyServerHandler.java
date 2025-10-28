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

package io.github.pnoker.driver.service.netty;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.9.0
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
     * Example, for reference only. Please parse the actual data format according to your own needs.
     * <p>
     * This method is used to handle incoming messages from clients.
     * It logs the received message, extracts the device name and ID, and then processes the point values based on the device configuration.
     * Finally, it sends the processed point values to the driver sender service.
     *
     * @param context The channel handler context for the current connection.
     * @param byteBuf The byte buffer containing the incoming message.
     */
    public void read(ChannelHandlerContext context, ByteBuf byteBuf) {
        log.info("{}->{}", context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        String deviceName = byteBuf.toString(0, 22, StandardCharsets.UTF_8);
        long deviceId = Long.parseLong(deviceName);
        DeviceBO device = deviceMetadata.getCache(deviceId);

        String hexKey = ByteBufUtil.hexDump(byteBuf, 22, 1);
        NettyTcpServer.deviceChannelMap.put(deviceId, context.channel());

        Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointConfig(deviceId);

        List<PointValue> pointValues = new ArrayList<>(16);
        for (Map.Entry<Long, Map<String, AttributeBO>> entry : pointConfigMap.entrySet()) {
            PointBO point = pointMetadata.getCache(entry.getKey());
            Map<String, AttributeBO> infoMap = pointConfigMap.get(entry.getKey());
            int start = infoMap.get("start").getValue(Integer.class);
            int end = infoMap.get("end").getValue(Integer.class);

            if (infoMap.get("key").getValue().equals(hexKey) && Objects.nonNull(point)) {
                String value = switch (point.getPointName()) {
                    case "altitude" -> String.valueOf(byteBuf.getFloat(start));
                    case "speed" -> String.valueOf(byteBuf.getDouble(start));
                    case "level" -> String.valueOf(byteBuf.getLong(start));
                    case "direction" -> String.valueOf(byteBuf.getInt(start));
                    case "locked" -> String.valueOf(byteBuf.getBoolean(start));
                    case "coordinate" -> byteBuf.toString(start, end, StandardCharsets.UTF_8).trim();
                    default -> StringUtils.EMPTY;
                };

                if (StringUtils.isNotEmpty(value)) {
                    pointValues.add(new PointValue(new RValue(device, point, value)));
                }
            }
        }

        driverSenderService.pointValueSender(pointValues);
    }
}
