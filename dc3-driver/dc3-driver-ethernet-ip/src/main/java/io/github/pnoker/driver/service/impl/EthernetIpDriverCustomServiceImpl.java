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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EtherNet/IP CIP driver service implementation.
 * <p>
 * Communicates with Rockwell Allen-Bradley PLCs over EtherNet/IP protocol
 * using raw TCP socket and CIP (Common Industrial Protocol) message framing.
 * Supports tag read/write via CIP Data Table Read/Write services.
 * </p>
 *
 *
 * <p>
 * <b>WARNING:</b> This driver is a work-in-progress skeleton. Protocol-level
 * I/O is not yet fully implemented — see TODO markers in method bodies.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Slf4j
@Service
public class EthernetIpDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Socket> connectMap;

    public EthernetIpDriverCustomServiceImpl(DriverMetadata driverMetadata, DriverSenderService driverSenderService) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
    }

    private static void readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read < 0) throw new IOException("Connection closed");
            offset += read;
        }
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b));
        return sb.toString();
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
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // No custom schedule needed.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        Socket socket = connectMap.get(device.getId());
        return Objects.nonNull(socket) && socket.isConnected() && !socket.isClosed()
                ? DeviceHealthState.online()
                : DeviceHealthState.offline();
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
                Socket removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    try {
                        removed.close();
                    } catch (IOException ignored) {
                    }
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
        Socket socket = getConnector(device.getId(), driverConfig);
        try {
            String tagName = getRequiredConfig(pointConfig, "tagName");
            String tagType = getConfigValue(pointConfig, "tagType", "DINT");

            // CIP Data Table Read: service=0x4C (Read Tag Service)
            byte[] response = sendCipCommand(socket, buildReadTagRequest(tagName));
            String value = parseTagValue(response, tagType);

            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), socket);
            throw e;
        } catch (Exception e) {
            invalidateConnector(device.getId(), socket);
            throw new ReadPointException("EtherNet/IP read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        Socket socket = getConnector(device.getId(), driverConfig);
        try {
            String tagName = getRequiredConfig(pointConfig, "tagName");
            String tagType = getConfigValue(pointConfig, "tagType", "DINT");
            String value = writePointValue.getValue(String.class);

            // CIP Data Table Write: service=0x4D (Write Tag Service)
            byte[] response = sendCipCommand(socket, buildWriteTagRequest(tagName, tagType, value));
            return response != null && response.length > 0;
        } catch (Exception e) {
            invalidateConnector(device.getId(), socket);
            throw new WritePointException("EtherNet/IP write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private Socket getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String host = getRequiredConfig(driverConfig, "host");
            int port = getConfigIntValue(driverConfig, "port", 44818);
            int timeout = getConfigIntValue(driverConfig, "timeout", 5000);

            try {
                Socket socket = new Socket(host, port);
                socket.setSoTimeout(timeout);
                // TODO: Send RegisterSession and ForwardOpen CIP commands to establish I/O connection
                log.info("EtherNet/IP connection established, protocol={}, deviceId={}, host={}:{}",
                        driverCode, deviceId, host, port);
                return socket;
            } catch (IOException e) {
                throw new ConnectorException("EtherNet/IP connection failed, protocol={}, deviceId={}, message={}",
                        driverCode, deviceId, e.getMessage(), e);
            }
        });
    }

    private void invalidateConnector(Long deviceId, Socket socket) {
        connectMap.remove(deviceId, socket);
        try {
            if (Objects.nonNull(socket) && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Build a CIP Read Tag request (service 0x4C).
     * TODO: Implement full CIP protocol framing (RegisterSession, ForwardOpen, Read Tag Service).
     */
    private byte[] buildReadTagRequest(String tagName) {
        byte[] tag = tagName.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buf = ByteBuffer.allocate(8 + tag.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x4C); // Read Tag Service
        buf.put((byte) tag.length);
        buf.put(tag);
        buf.putInt(1); // element count
        return buf.array();
    }

    /**
     * Build a CIP Write Tag request (service 0x4D).
     * TODO: Implement full CIP write framing.
     */
    private byte[] buildWriteTagRequest(String tagName, String tagType, String value) {
        byte[] tag = tagName.getBytes(StandardCharsets.US_ASCII);
        byte[] val = encodeTagValue(tagType, value);
        ByteBuffer buf = ByteBuffer.allocate(10 + tag.length + val.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x4D); // Write Tag Service
        buf.put((byte) tag.length);
        buf.put(tag);
        buf.put((byte) mapTypeCode(tagType));
        buf.putInt(val.length);
        buf.put(val);
        return buf.array();
    }

    /**
     * Send a CIP command over TCP and return the response.
     */
    private byte[] sendCipCommand(Socket socket, byte[] command) throws IOException {
        OutputStream out = socket.getOutputStream();
        // CIP encapsulation header (24 bytes) + command data
        byte[] header = buildEncapsulationHeader(command.length);
        out.write(header);
        out.write(command);
        out.flush();

        InputStream in = socket.getInputStream();
        byte[] respHeader = new byte[24];
        readFully(in, respHeader);
        int dataLen = ByteBuffer.wrap(respHeader, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        if (dataLen > 0) {
            byte[] data = new byte[dataLen];
            readFully(in, data);
            return data;
        }
        return new byte[0];
    }

    private byte[] buildEncapsulationHeader(int commandLen) {
        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) commandLen);
        // Simple 24-byte header; detailed CIP encapsulation framing is TODO
        return buf.array();
    }

    private String parseTagValue(byte[] data, String tagType) {
        if (data == null || data.length < 2) return null;
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return switch (tagType.toUpperCase()) {
            case "BOOL" -> String.valueOf(buf.get() != 0);
            case "SINT" -> String.valueOf(buf.get());
            case "INT" -> String.valueOf(buf.getShort());
            case "DINT" -> String.valueOf(buf.getInt());
            case "REAL" -> String.valueOf(buf.getFloat());
            case "STRING" -> new String(data, StandardCharsets.US_ASCII).trim();
            default -> bytesToHex(data);
        };
    }

    private byte[] encodeTagValue(String tagType, String value) {
        ByteBuffer buf;
        switch (tagType.toUpperCase()) {
            case "BOOL" -> {
                buf = ByteBuffer.allocate(1);
                buf.put((byte) (Boolean.parseBoolean(value) ? 1 : 0));
            }
            case "SINT" -> {
                buf = ByteBuffer.allocate(1);
                buf.put(Byte.parseByte(value));
            }
            case "INT" -> {
                buf = ByteBuffer.allocate(2);
                buf.order(ByteOrder.LITTLE_ENDIAN).putShort(Short.parseShort(value));
            }
            case "DINT" -> {
                buf = ByteBuffer.allocate(4);
                try {

                    buf.order(ByteOrder.LITTLE_ENDIAN).putInt(Integer.parseInt(value));

                } catch (NumberFormatException e) {

                    buf.order(ByteOrder.LITTLE_ENDIAN).putInt(0);

                }
            }
            case "REAL" -> {
                buf = ByteBuffer.allocate(4);
                buf.order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.parseFloat(value));
            }
            default -> buf = ByteBuffer.wrap(value.getBytes(StandardCharsets.US_ASCII));
        }
        return buf.array();
    }

    private byte mapTypeCode(String tagType) {
        return switch (tagType.toUpperCase()) {
            case "BOOL" -> (byte) 0xC1;
            case "SINT" -> (byte) 0xC2;
            case "INT" -> (byte) 0xC3;
            case "DINT" -> (byte) 0xC4;
            case "REAL" -> (byte) 0xCA;
            default -> (byte) 0xC4;
        };
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

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "slot", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "tagName", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}