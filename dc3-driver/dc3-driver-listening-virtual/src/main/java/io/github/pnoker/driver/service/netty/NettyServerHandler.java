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
    private static final int DEVICE_NAME_LENGTH = 22;
    private static final int HEX_KEY_OFFSET = 22;
    private static final int HEX_KEY_LENGTH = 1;
    private static final int MIN_MESSAGE_LENGTH = DEVICE_NAME_LENGTH + HEX_KEY_LENGTH;

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
        log.debug("Driver message received, protocol={}, remoteAddress={}, bytes={}", PROTOCOL,
                context.channel().remoteAddress(), readableBytes);
        if (log.isTraceEnabled()) {
            log.trace("Driver message payload received, protocol={}, remoteAddress={}, payload={}", PROTOCOL,
                    context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        }
        if (readableBytes < MIN_MESSAGE_LENGTH) {
            log.warn("Driver message skipped, protocol={}, remoteAddress={}, reason=payloadTooShort, bytes={}", PROTOCOL,
                    context.channel().remoteAddress(), readableBytes);
            return;
        }

        String deviceName = byteBuf.toString(0, DEVICE_NAME_LENGTH, StandardCharsets.UTF_8).trim();
        Long deviceId = parseDeviceId(deviceName, context);
        if (Objects.isNull(deviceId)) {
            return;
        }
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            log.warn("Driver message skipped, protocol={}, remoteAddress={}, deviceId={}, reason=deviceMissing", PROTOCOL,
                    context.channel().remoteAddress(), deviceId);
            return;
        }

        String hexKey = ByteBufUtil.hexDump(byteBuf, HEX_KEY_OFFSET, HEX_KEY_LENGTH);
        NettyTcpServer.registerDeviceChannel(deviceId, context.channel());

        Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointConfig(deviceId);
        if (Objects.isNull(pointConfigMap)) {
            log.warn("Driver message skipped, protocol={}, remoteAddress={}, deviceId={}, reason=pointConfigMissing", PROTOCOL,
                    context.channel().remoteAddress(), deviceId);
            return;
        }

        List<PointValue> pointValues = new ArrayList<>(16);
        for (Map.Entry<Long, Map<String, AttributeBO>> entry : pointConfigMap.entrySet()) {
            PointBO point = pointMetadata.getCache(entry.getKey());
            if (Objects.isNull(point)) {
                continue;
            }
            Map<String, AttributeBO> infoMap = entry.getValue();
            PointConfig pointConfig = readPointConfig(infoMap, point, context);
            if (Objects.isNull(pointConfig)) {
                continue;
            }

            if (Objects.equals(pointConfig.key(), hexKey)) {
                String value = readConfiguredValue(byteBuf, point, pointConfig.start(), pointConfig.end(), context);

                if (StringUtils.isNotEmpty(value)) {
                    pointValues.add(new PointValue(new ReadPointValue(device, point, value)));
                }
            }
        }

        if (!pointValues.isEmpty()) {
            driverSenderService.pointValueSender(pointValues);
        }
        log.debug("Driver point values forwarded, protocol={}, deviceId={}, key={}, count={}", PROTOCOL, deviceId, hexKey,
                pointValues.size());
    }

    private PointConfig readPointConfig(Map<String, AttributeBO> infoMap, PointBO point, ChannelHandlerContext context) {
        if (Objects.isNull(infoMap) || Objects.isNull(infoMap.get("key")) || Objects.isNull(infoMap.get("start"))
                || Objects.isNull(infoMap.get("end"))) {
            log.warn("Driver point config skipped, protocol={}, remoteAddress={}, pointId={}, reason=requiredConfigMissing", PROTOCOL,
                    context.channel().remoteAddress(), point.getId());
            return null;
        }
        try {
            return new PointConfig(infoMap.get("key").getValue(), infoMap.get("start").getValue(Integer.class),
                    infoMap.get("end").getValue(Integer.class));
        } catch (Exception e) {
            log.warn("Driver point config skipped, protocol={}, remoteAddress={}, pointId={}, reason=invalidConfig", PROTOCOL,
                    context.channel().remoteAddress(), point.getId(), e);
            return null;
        }
    }

    private Long parseDeviceId(String deviceName, ChannelHandlerContext context) {
        try {
            return Long.parseLong(deviceName);
        } catch (NumberFormatException ignored) {
            log.warn("Driver message skipped, protocol={}, remoteAddress={}, deviceName={}, reason=deviceIdInvalid", PROTOCOL,
                    context.channel().remoteAddress(), deviceName);
            return null;
        }
    }

    private String readConfiguredValue(ByteBuf byteBuf, PointBO point, int start, int end, ChannelHandlerContext context) {
        return switch (point.getPointName()) {
            case "altitude" -> readFloat(byteBuf, start, point, context);
            case "speed" -> readDouble(byteBuf, start, point, context);
            case "level" -> readLong(byteBuf, start, point, context);
            case "direction" -> readInt(byteBuf, start, point, context);
            case "locked" -> readBoolean(byteBuf, start, point, context);
            case "coordinate" -> readString(byteBuf, start, end, point, context);
            default -> StringUtils.EMPTY;
        };
    }

    private String readFloat(ByteBuf byteBuf, int start, PointBO point, ChannelHandlerContext context) {
        if (!hasBytes(byteBuf, start, Float.BYTES, point, context)) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(byteBuf.getFloat(start));
    }

    private String readDouble(ByteBuf byteBuf, int start, PointBO point, ChannelHandlerContext context) {
        if (!hasBytes(byteBuf, start, Double.BYTES, point, context)) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(byteBuf.getDouble(start));
    }

    private String readLong(ByteBuf byteBuf, int start, PointBO point, ChannelHandlerContext context) {
        if (!hasBytes(byteBuf, start, Long.BYTES, point, context)) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(byteBuf.getLong(start));
    }

    private String readInt(ByteBuf byteBuf, int start, PointBO point, ChannelHandlerContext context) {
        if (!hasBytes(byteBuf, start, Integer.BYTES, point, context)) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(byteBuf.getInt(start));
    }

    private String readBoolean(ByteBuf byteBuf, int start, PointBO point, ChannelHandlerContext context) {
        if (!hasBytes(byteBuf, start, 1, point, context)) {
            return StringUtils.EMPTY;
        }
        return String.valueOf(byteBuf.getBoolean(start));
    }

    private String readString(ByteBuf byteBuf, int start, int end, PointBO point, ChannelHandlerContext context) {
        int length = end > start ? end - start : end;
        if (!hasBytes(byteBuf, start, length, point, context)) {
            return StringUtils.EMPTY;
        }
        return byteBuf.toString(start, length, StandardCharsets.UTF_8).trim();
    }

    private boolean hasBytes(ByteBuf byteBuf, int start, int length, PointBO point, ChannelHandlerContext context) {
        if (start < 0 || length <= 0 || start + length > byteBuf.writerIndex()) {
            log.warn("Driver point value skipped, protocol={}, remoteAddress={}, pointId={}, pointName={}, reason=payloadOutOfBounds, start={}, length={}, bytes={}", PROTOCOL,
                    context.channel().remoteAddress(), point.getId(), point.getPointName(), start, length, byteBuf.readableBytes());
            return false;
        }
        return true;
    }

    private record PointConfig(String key, int start, int end) {
    }

}
