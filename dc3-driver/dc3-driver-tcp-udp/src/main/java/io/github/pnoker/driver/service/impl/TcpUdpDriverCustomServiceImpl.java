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
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the TCP/UDP Raw Driver.
 * <p>
 * Provides generic TCP and UDP socket communication with connection caching
 * per device to avoid repeated TCP handshake overhead. Supports configurable
 * frame parsing with header, footer, offset, and length settings.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Service
public class TcpUdpDriverCustomServiceImpl implements DriverCustomService {

    private static final int FAILURE_BACKOFF_THRESHOLD = 3;
    private static final long FAILURE_BACKOFF_MS = 60_000;
    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;
    /**
     * Device TCP connection cache keyed by device ID.
     */
    private Map<Long, Socket> tcpConnectMap;
    /**
     * Failure tracking for connection backoff.
     */
    private Map<Long, ConsecutiveFailure> failureMap;

    /**
     * Explicit constructor for dependency injection.
     */
    public TcpUdpDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    private static void checkRequired(Map<String, AttributeBO> config, String code,
                                      List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code).level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code).build());
        }
    }

    @Override
    public void initial() {
        tcpConnectMap = new ConcurrentHashMap<>(16);
        failureMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        Long deviceId = device.getId();
        // Check cached connection
        Socket existing = tcpConnectMap.get(deviceId);
        if (existing != null) {
            return existing.isConnected() && !existing.isClosed()
                    ? DeviceHealthState.online()
                    : DeviceHealthState.offline();
        }
        // If in backoff, skip health check attempt
        ConsecutiveFailure failure = failureMap.get(deviceId);
        if (failure != null && failure.shouldBackoff()) {
            return DeviceHealthState.offline();
        }
        // Try a quick TCP connection test (UDP is connectionless, reported online by default)
        String protocol = getConfigValue(driverConfig, "protocol", "TCP");
        if ("UDP".equalsIgnoreCase(protocol)) {
            return DeviceHealthState.online();
        }
        try {
            String host = getConfigValue(driverConfig, "host", "localhost");
            int port = getConfigIntValue(driverConfig, "port", 502);
            int connectTimeout = getConfigIntValue(driverConfig, "connectTimeout", 5000);
            try (Socket test = new Socket()) {
                test.connect(new InetSocketAddress(host, port), connectTimeout);
                return DeviceHealthState.online();
            }
        } catch (Exception e) {
            log.warn("TCP/UDP health check failed, protocol={}, deviceId={}", driverCode, deviceId, e);
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
                Socket removed = tcpConnectMap.remove(metadataEvent.getId());
                if (removed != null) {
                    closeQuietly(removed);
                }
                failureMap.remove(metadataEvent.getId());
                log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                        driverCode, metadataEvent.getId(), operateType);
            }
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String protocol = getConfigValue(driverConfig, "protocol", "TCP");
        String sendCommand = getConfigValue(pointConfig, "sendCommand", "");
        String rawValue;

        try {
            if ("UDP".equalsIgnoreCase(protocol)) {
                rawValue = sendUdp(driverConfig, sendCommand);
            } else {
                rawValue = sendTcp(device.getId(), driverConfig, sendCommand);
            }
            String parsedValue = parseFrame(rawValue, pointConfig);
            return new ReadPointValue(device, point, parsedValue);
        } catch (IOException e) {
            invalidateConnector(device.getId());
            throw new ReadPointException("TCP/UDP read failed, protocol={}, deviceId={}, pointId={}, message={}",
                    driverCode, device.getId(), point.getId(), e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        String protocol = getConfigValue(driverConfig, "protocol", "TCP");
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
                sendUdp(driverConfig, sendCommand);
            } else {
                sendTcp(device.getId(), driverConfig, sendCommand);
            }
            return true;
        } catch (IOException e) {
            invalidateConnector(device.getId());
            throw new WritePointException("TCP/UDP write failed, protocol={}, deviceId={}, pointId={}, message={}",
                    driverCode, device.getId(), point.getId(), e.getMessage(), e);
        }
    }

    // ---- TCP with connection caching ----

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

    private String sendTcp(Long deviceId, Map<String, AttributeBO> driverConfig, String sendCommand) throws IOException {
        Socket socket = getTcpConnector(deviceId, driverConfig);
        byte[] sendBytes = hexToBytes(sendCommand);
        try {
            OutputStream out = socket.getOutputStream();
            out.write(sendBytes);
            out.flush();

            byte[] buffer = new byte[4096];
            InputStream in = socket.getInputStream();
            int len = in.read(buffer);
            if (len > 0) {
                byte[] response = new byte[len];
                System.arraycopy(buffer, 0, response, 0, len);
                // Successful communication clears failure tracking
                failureMap.remove(deviceId);
                return bytesToHex(response);
            }
        } catch (IOException e) {
            failureMap.compute(deviceId, (k, v) ->
                    v == null ? new ConsecutiveFailure() : v.increment());
            throw e;
        }
        return "";
    }

    // ---- UDP (stateless, connection-per-call) ----

    private Socket getTcpConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        // Check backoff
        ConsecutiveFailure failure = failureMap.get(deviceId);
        if (failure != null && failure.shouldBackoff()) {
            throw new ConnectorException(
                    "Driver connection in backoff after {} consecutive failures, protocol={}, deviceId={}",
                    failure.count, driverCode, deviceId);
        }

        Socket existing = tcpConnectMap.get(deviceId);
        if (existing != null && existing.isConnected() && !existing.isClosed()) {
            return existing;
        }

        // Remove stale socket
        if (existing != null) {
            closeQuietly(existing);
            tcpConnectMap.remove(deviceId);
        }

        // Create new connection
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", 502);
        int connectTimeout = getConfigIntValue(driverConfig, "connectTimeout", 5000);
        int readTimeout = getConfigIntValue(driverConfig, "readTimeout", 3000);

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);
            tcpConnectMap.put(deviceId, socket);
            failureMap.remove(deviceId);
            log.info("TCP connection established, protocol={}, deviceId={}, host={}, port={}",
                    driverCode, deviceId, host, port);
            return socket;
        } catch (IOException e) {
            failureMap.compute(deviceId, (k, v) ->
                    v == null ? new ConsecutiveFailure() : v.increment());
            throw new ConnectorException("TCP connection failed, protocol={}, deviceId={}, host={}, port={}, message={}",
                    driverCode, deviceId, host, port, e.getMessage(), e);
        }
    }

    // ---- Frame parsing ----

    private String sendUdp(Map<String, AttributeBO> driverConfig, String sendCommand) throws IOException {
        String host = getConfigValue(driverConfig, "host", "localhost");
        int port = getConfigIntValue(driverConfig, "port", 502);
        int readTimeout = getConfigIntValue(driverConfig, "readTimeout", 3000);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(readTimeout);
            byte[] sendBytes = hexToBytes(sendCommand);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length,
                    java.net.InetAddress.getByName(host), port);
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

    // ---- Helpers ----

    private String parseDataValue(byte[] dataBytes, String dataFormat, Map<String, AttributeBO> pointConfig) {
        String byteOrder = getConfigValue(pointConfig, "byteOrder", "BIG");
        ByteOrder order = "LITTLE".equalsIgnoreCase(byteOrder) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        return switch (dataFormat.toUpperCase()) {
            case "HEX" -> bytesToHex(dataBytes);
            case "ASCII" -> new String(dataBytes, StandardCharsets.US_ASCII).trim();
            case "INT16" -> dataBytes.length >= 2
                    ? String.valueOf(ByteBuffer.wrap(dataBytes).order(order).getShort())
                    : bytesToHex(dataBytes);
            case "UINT16" -> dataBytes.length >= 2
                    ? String.valueOf((int) ByteBuffer.wrap(dataBytes).order(order).getShort() & 0xFFFF)
                    : bytesToHex(dataBytes);
            case "INT32" -> dataBytes.length >= 4
                    ? String.valueOf(ByteBuffer.wrap(dataBytes).order(order).getInt())
                    : bytesToHex(dataBytes);
            case "FLOAT" -> dataBytes.length >= 4
                    ? String.valueOf(ByteBuffer.wrap(dataBytes).order(order).getFloat())
                    : bytesToHex(dataBytes);
            default -> bytesToHex(dataBytes);
        };
    }

    private void invalidateConnector(Long deviceId) {
        Socket removed = tcpConnectMap.remove(deviceId);
        if (removed != null) {
            closeQuietly(removed);
        }
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            log.warn("TCP socket close failed, protocol={}", driverCode, e);
        }
    }

    private byte[] hexToBytes(String hex) {
        if (StringUtils.isBlank(hex)) {
            return new byte[0];
        }
        hex = hex.replaceAll("\\s+", "");
        return HexFormat.of().parseHex(hex);
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        if (Objects.isNull(config) || Objects.isNull(config.get(code))) {
            return defaultValue;
        }
        String value = config.get(code).getValue();
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    // ---- Backoff tracking ----

    private int getConfigIntValue(Map<String, AttributeBO> config, String code, int defaultValue) {
        String value = getConfigValue(config, code, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            log.warn("Failed to parse int config value for code={}, using default={}", code, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "protocol", issues);
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "sendCommand", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    private static class ConsecutiveFailure {
        final int count;
        final long firstFailureTime;

        ConsecutiveFailure() {
            this.count = 1;
            this.firstFailureTime = System.currentTimeMillis();
        }

        ConsecutiveFailure(int count, long firstFailureTime) {
            this.count = count;
            this.firstFailureTime = firstFailureTime;
        }

        ConsecutiveFailure increment() {
            return new ConsecutiveFailure(count + 1, firstFailureTime);
        }

        boolean shouldBackoff() {
            return count >= FAILURE_BACKOFF_THRESHOLD
                    && (System.currentTimeMillis() - firstFailureTime) < FAILURE_BACKOFF_MS;
        }
    }

}