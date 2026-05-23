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
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Common message handler for processing incoming TCP/UDP data.
 * <p>
 * This service parses incoming byte buffers according to a specific format, extracts
 * device and point information, and sends point values to the driver sender service for
 * further processing.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NettyServerHandler {

    private static final String PROTOCOL = "netty";

    private final DeviceMetadata deviceMetadata;

    private final PointMetadata pointMetadata;

    private final DriverSenderService driverSenderService;

    /**
     * Processes incoming messages from TCP/UDP clients.
     * <p>
     * Parses the byte buffer to extract device information and point values. The message
     * format is: <pre>
     * - Device name: 22 bytes (converted to device ID)
     * - Keyword: 1 byte (hex)
     * - Point values: variable length based on point configuration
     * </pre>
     * </p>
     *
     * @param context The channel handler context for the current connection
     * @param byteBuf The byte buffer containing the incoming message
     */
    public void read(ChannelHandlerContext context, ByteBuf byteBuf) {
        int readableBytes = byteBuf.readableBytes();
        log.debug("Driver message received, protocol=" + PROTOCOL + ", remoteAddress={}, bytes={}",
                context.channel().remoteAddress(), readableBytes);
        if (log.isTraceEnabled()) {
            log.trace("Driver message payload received, protocol=" + PROTOCOL + ", remoteAddress={}, payload={}",
                    context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        }
        if (readableBytes < 23) {
            log.warn("Driver message skipped, protocol=" + PROTOCOL + ", remoteAddress={}, reason=payloadTooShort, bytes={}",
                    context.channel().remoteAddress(), readableBytes);
            return;
        }

        String deviceName = byteBuf.toString(0, 22, StandardCharsets.UTF_8).trim();
        long deviceId = Long.parseLong(deviceName);
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            log.warn("Driver message skipped, protocol=" + PROTOCOL + ", remoteAddress={}, deviceId={}, reason=deviceMissing",
                    context.channel().remoteAddress(), deviceId);
            return;
        }

        String hexKey = ByteBufUtil.hexDump(byteBuf, 22, 1);
        NettyTcpServer.registerDeviceChannel(deviceId, context.channel());

        Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointConfig(deviceId);
        if (Objects.isNull(pointConfigMap)) {
            log.warn("Driver message skipped, protocol=" + PROTOCOL + ", remoteAddress={}, deviceId={}, reason=pointConfigMissing",
                    context.channel().remoteAddress(), deviceId);
            return;
        }

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
                    pointValues.add(new PointValue(new ReadPointValue(device, point, value)));
                }
            }
        }

        driverSenderService.pointValueSender(pointValues);
        log.debug("Driver point values forwarded, protocol=" + PROTOCOL + ", deviceId={}, key={}, count={}", deviceId, hexKey,
                pointValues.size());
    }

}
