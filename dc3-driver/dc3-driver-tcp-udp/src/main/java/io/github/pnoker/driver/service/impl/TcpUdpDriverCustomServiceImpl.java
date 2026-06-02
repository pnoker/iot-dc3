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

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the TCP/UDP Raw Driver.
 * <p>
 * This service provides generic TCP and UDP socket communication for reading
 * and writing data to devices that support raw socket protocols. It supports
 * configurable frame parsing with header, footer, offset, and length settings.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
public class TcpUdpDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    /**
     * Device connection cache keyed by device ID.
     */
    private final ConcurrentHashMap<Long, Socket> connectMap = new ConcurrentHashMap<>();

    /**
     * Explicit constructor for dependency injection.
     *
     * @param driverMetadata       driver metadata service
     * @param driverSenderService  driver sender service
     */
    public TcpUdpDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    @Override
    public void initial() {
        /*
         * TCP/UDP Driver initialization logic
         */
    }

    @Override
    public void schedule() {
        /*
         * TCP/UDP Driver custom schedule logic
         */
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * TCP/UDP Driver metadata event handling
         */
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", 502);
        String protocol = getConfigValue(driverConfig, "protocol", "TCP");
        int connectTimeout = getConfigIntValue(driverConfig, "connectTimeout", 5000);
        int readTimeout = getConfigIntValue(driverConfig, "readTimeout", 3000);

        String sendCommand = getConfigValue(pointConfig, "sendCommand", "");
        String rawValue;

        try {
            if ("UDP".equalsIgnoreCase(protocol)) {
                rawValue = sendUdp(host, port, sendCommand, readTimeout);
            } else {
                rawValue = sendTcp(host, port, sendCommand, connectTimeout, readTimeout);
            }

            String parsedValue = parseFrame(rawValue, pointConfig);
            return new ReadPointValue(device, point, parsedValue);
        } catch (Exception e) {
            log.error("TCP/UDP read failed, deviceId={}, pointId={}, host={}, port={}",
                    device.getId(), point.getId(), host, port, e);
            throw new RuntimeException("TCP/UDP read failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", 502);
        String protocol = getConfigValue(driverConfig, "protocol", "TCP");
        int connectTimeout = getConfigIntValue(driverConfig, "connectTimeout", 5000);
        int readTimeout = getConfigIntValue(driverConfig, "readTimeout", 3000);

        String sendCommand = getConfigValue(pointConfig, "sendCommand", "");
        if (StringUtils.isBlank(sendCommand)) {
            log.warn("TCP/UDP write failed, sendCommand is empty, deviceId={}, pointId={}",
                    device.getId(), point.getId());
            return false;
        }

        String value = writePointValue.getValue();
        sendCommand = sendCommand.replace("${value}", value);

        try {
            if ("UDP".equalsIgnoreCase(protocol)) {
                sendUdp(host, port, sendCommand, readTimeout);
            } else {
                sendTcp(host, port, sendCommand, connectTimeout, readTimeout);
            }
            return true;
        } catch (Exception e) {
            log.error("TCP/UDP write failed, deviceId={}, pointId={}, host={}, port={}",
                    device.getId(), point.getId(), host, port, e);
            return false;
        }
    }

    @Override
    public Map<String, String> execute(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> commandConfig,
                                       DeviceBO device, FacadeCommandBO command, Map<String, String> paramValues) {
        Map<String, String> result = new LinkedHashMap<>();
        String sendCommand = getConfigValue(commandConfig, "sendCommand", "${value}");
        if (Objects.nonNull(paramValues)) {
            for (Map.Entry<String, String> entry : paramValues.entrySet()) {
                sendCommand = sendCommand.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        result.put("sendCommand", sendCommand);
        return result;
    }

    /**
     * Send command via TCP and return response as hex string.
     */
    private String sendTcp(String host, int port, String sendCommand, int connectTimeout, int readTimeout) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);

            byte[] sendBytes = hexToBytes(sendCommand);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(sendBytes);
                out.flush();
            }

            byte[] buffer = new byte[4096];
            try (InputStream in = socket.getInputStream()) {
                int len = in.read(buffer);
                if (len > 0) {
                    byte[] response = new byte[len];
                    System.arraycopy(buffer, 0, response, 0, len);
                    return bytesToHex(response);
                }
            }
        }
        return "";
    }

    /**
     * Send command via UDP and return response as hex string.
     */
    private String sendUdp(String host, int port, String sendCommand, int readTimeout) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(readTimeout);

            byte[] sendBytes = hexToBytes(sendCommand);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, java.net.InetAddress.getByName(host), port);
            socket.send(sendPacket);

            byte[] buffer = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            int len = receivePacket.getLength();
            byte[] response = new byte[len];
            System.arraycopy(buffer, 0, response, 0, len);
            return bytesToHex(response);
        }
    }

    /**
     * Parse the raw response using frame configuration.
     */
    private String parseFrame(String rawHex, Map<String, AttributeBO> pointConfig) {
        if (StringUtils.isBlank(rawHex)) {
            return "";
        }

        int offset = getConfigIntValue(pointConfig, "dataOffset", 0);
        int length = getConfigIntValue(pointConfig, "dataLength", 0);
        String dataFormat = getConfigValue(pointConfig, "dataFormat", "HEX");

        try {
            byte[] rawBytes = hexToBytes(rawHex);

            if (length > 0 && offset >= 0 && (offset + length) <= rawBytes.length) {
                byte[] dataBytes = new byte[length];
                System.arraycopy(rawBytes, offset, dataBytes, 0, length);
                return parseDataValue(dataBytes, dataFormat, pointConfig);
            }

            return rawHex;
        } catch (Exception e) {
            log.warn("Frame parsing failed, rawHex={}, falling back to raw value", rawHex, e);
            return rawHex;
        }
    }

    /**
     * Parse data bytes based on the specified format.
     */
    private String parseDataValue(byte[] dataBytes, String dataFormat, Map<String, AttributeBO> pointConfig) {
        String byteOrder = getConfigValue(pointConfig, "byteOrder", "BIG");
        ByteOrder order = "LITTLE".equalsIgnoreCase(byteOrder) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;

        return switch (dataFormat.toUpperCase()) {
            case "HEX" -> bytesToHex(dataBytes);
            case "ASCII" -> new String(dataBytes, StandardCharsets.US_ASCII).trim();
            case "INT16" -> {
                if (dataBytes.length >= 2) {
                    yield String.valueOf(ByteBuffer.wrap(dataBytes).order(order).getShort());
                }
                yield bytesToHex(dataBytes);
            }
            case "UINT16" -> {
                if (dataBytes.length >= 2) {
                    byte[] unsigned = new byte[]{0, dataBytes[0], dataBytes[1]};
                    yield String.valueOf((int) ByteBuffer.wrap(dataBytes).order(order).getShort() & 0xFFFF);
                }
                yield bytesToHex(dataBytes);
            }
            case "INT32" -> {
                if (dataBytes.length >= 4) {
                    yield String.valueOf(ByteBuffer.wrap(dataBytes).order(order).getInt());
                }
                yield bytesToHex(dataBytes);
            }
            case "FLOAT" -> {
                if (dataBytes.length >= 4) {
                    yield String.valueOf(ByteBuffer.wrap(dataBytes).order(order).getFloat());
                }
                yield bytesToHex(dataBytes);
            }
            default -> bytesToHex(dataBytes);
        };
    }

    /**
     * Convert hex string to byte array.
     */
    private byte[] hexToBytes(String hex) {
        if (StringUtils.isBlank(hex)) {
            return new byte[0];
        }
        hex = hex.replaceAll("\\s+", "");
        return HexFormat.of().parseHex(hex);
    }

    /**
     * Convert byte array to uppercase hex string.
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    /**
     * Get string config value with default.
     */
    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        if (Objects.isNull(config) || Objects.isNull(config.get(code))) {
            return defaultValue;
        }
        String value = config.get(code).getValue();
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    /**
     * Get int config value with default.
     */
    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        String value = getConfigValue(config, code, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse int config value for code={}, using default={}", code, defaultValue);
            return defaultValue;
        }
    }
}
