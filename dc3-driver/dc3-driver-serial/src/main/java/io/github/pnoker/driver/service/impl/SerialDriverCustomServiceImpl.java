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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic serial port driver service implementation.
 * <p>
 * Supports RS232/RS485/RS422 with configurable protocol frame parsing
 * (frame header, footer, checksum), HEX/ASCII data formats, and byte ordering.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerialDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, SerialPortConnection> connectMap;

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // Serial drivers do not need custom scheduled tasks.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            SerialPortConnection conn = getConnector(device.getId(), driverConfig);
            return conn.isOpen() ? DeviceHealthState.online() : DeviceHealthState.offline();
        } catch (Exception e) {
            log.warn("Driver health check failed, protocol={}, deviceId={}", driverCode, device.getId(), e);
            return DeviceHealthState.offline();
        }
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                SerialPortConnection removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    removed.close();
                    log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode, metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        SerialPortConnection conn = getConnector(device.getId(), driverConfig);
        try {
            String sendCommandHex = getRequiredConfig(pointConfig, "sendCommand");
            byte[] commandBytes = SerialFrameParser.hexToBytes(sendCommandHex);
            int receiveLength = getConfigIntValue(pointConfig, "receiveLength", 0);

            byte[] rawResponse = conn.sendAndReceive(commandBytes, receiveLength);
            if (Objects.isNull(rawResponse) || rawResponse.length == 0) {
                throw new ReadPointException("Empty serial response, protocol={}", driverCode);
            }

            String value = parseResponse(rawResponse, pointConfig);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), conn);
            throw e;
        } catch (Exception e) {
            invalidateConnector(device.getId(), conn);
            throw new ReadPointException("Serial read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        SerialPortConnection conn = getConnector(device.getId(), driverConfig);
        try {
            String sendCommandHex = getRequiredConfig(pointConfig, "sendCommand");
            String resolved = sendCommandHex.replace("${value}", writePointValue.getValue(String.class));
            byte[] commandBytes = SerialFrameParser.hexToBytes(resolved);
            conn.send(commandBytes);
            return true;
        } catch (Exception e) {
            invalidateConnector(device.getId(), conn);
            throw new WritePointException("Serial write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    /**
     * Get or create a serial port connection for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration containing serial port parameters
     * @return cached or newly created SerialPortConnection
     * @throws ConnectorException if connection initialization fails
     */
    private SerialPortConnection getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String port = getConfigValue(driverConfig, "port", "/dev/ttyUSB0");
            int baudRate = getConfigIntValue(driverConfig, "baudRate", 9600);
            int dataBits = getConfigIntValue(driverConfig, "dataBits", 8);
            int stopBits = getConfigIntValue(driverConfig, "stopBits", 1);
            int parity = getConfigIntValue(driverConfig, "parity", 0);
            int timeout = getConfigIntValue(driverConfig, "timeout", 1000);

            log.debug("Driver connection creating, protocol={}, deviceId={}, port={}, baudRate={}",
                    driverCode, deviceId, port, baudRate);

            SerialPortConnection conn = new SerialPortConnection(port, baudRate, dataBits, stopBits, parity, timeout);
            conn.open();
            log.info("Driver connection established, protocol={}, deviceId={}, port={}", driverCode, deviceId, port);
            return conn;
        });
    }

    /**
     * Parse the raw serial response using the configured frame parser.
     *
     * @param rawResponse  raw response bytes
     * @param pointConfig  point configuration with frame parsing parameters
     * @return parsed value as string
     */
    private String parseResponse(byte[] rawResponse, Map<String, AttributeBO> pointConfig) {
        String frameHeader = getConfigValue(pointConfig, "frameHeader", "");
        String frameFooter = getConfigValue(pointConfig, "frameFooter", "");
        int dataOffset = getConfigIntValue(pointConfig, "dataOffset", 0);
        int dataLength = getConfigIntValue(pointConfig, "dataLength", 0);
        String checksumType = getConfigValue(pointConfig, "checksumType", "NONE");
        String dataFormat = getConfigValue(pointConfig, "dataFormat", "HEX");
        String byteOrder = getConfigValue(pointConfig, "byteOrder", "BIG");

        SerialFrameParser parser = new SerialFrameParser(frameHeader, frameFooter, dataOffset, dataLength, checksumType);
        byte[] data = parser.parse(rawResponse);

        return formatValue(data, dataFormat, byteOrder);
    }

    /**
     * Format the parsed data bytes according to the configured format.
     *
     * @param data       data bytes
     * @param dataFormat format: HEX, ASCII, or BINARY
     * @param byteOrder  byte order: BIG or LITTLE
     * @return formatted value string
     */
    private String formatValue(byte[] data, String dataFormat, String byteOrder) {
        return switch (dataFormat.toUpperCase()) {
            case "ASCII" -> new String(data);
            case "BINARY", "INT" -> {
                if (data.length == 1) {
                    yield String.valueOf(data[0] & 0xFF);
                } else if (data.length == 2) {
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    if ("LITTLE".equalsIgnoreCase(byteOrder)) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    yield String.valueOf(bb.getShort() & 0xFFFF);
                } else if (data.length == 4) {
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    if ("LITTLE".equalsIgnoreCase(byteOrder)) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    yield String.valueOf(bb.getInt());
                } else if (data.length == 8) {
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    if ("LITTLE".equalsIgnoreCase(byteOrder)) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    yield String.valueOf(bb.getLong());
                } else {
                    yield SerialFrameParser.bytesToHex(data);
                }
            }
            case "FLOAT" -> {
                if (data.length == 4) {
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    if ("LITTLE".equalsIgnoreCase(byteOrder)) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    yield String.valueOf(bb.getFloat());
                } else if (data.length == 8) {
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    if ("LITTLE".equalsIgnoreCase(byteOrder)) {
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                    }
                    yield String.valueOf(bb.getDouble());
                } else {
                    yield SerialFrameParser.bytesToHex(data);
                }
            }
            default -> SerialFrameParser.bytesToHex(data);
        };
    }

    private void invalidateConnector(Long deviceId, SerialPortConnection conn) {
        connectMap.remove(deviceId, conn);
        try {
            if (Objects.nonNull(conn)) {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Driver connection destroy failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        }
    }

    private String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            return defaultValue;
        }
        return attr.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Integer.class);
    }
}
