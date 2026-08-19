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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the DL/T 645-2007 smart meter driver.
 * <p>
 * Manages one serial connection per meter and reads/writes points using standard
 * DL/T 645-2007 data identifiers. Meter responses carry their data field offset by
 * {@code +0x33}, which {@link Dlt645Frame} reverses during parse.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Dlt645DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Dlt645SerialPortConnection> connectMap;

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
        // DL/T 645 meters are polled on the SDK read schedule; no custom task is needed.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            Dlt645SerialPortConnection conn = getConnector(device.getId(), driverConfig);
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
                Dlt645SerialPortConnection removed = connectMap.remove(metadataEvent.getId());
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
        Dlt645SerialPortConnection conn = getConnector(device.getId(), driverConfig);
        try {
            int[] di = parseDi(getRequiredConfig(pointConfig, "di"));
            byte[] address = Dlt645Frame.encodeAddress(getConfigValue(driverConfig, "meterAddress", "000000000000"));

            byte[] request = Dlt645Frame.buildReadRequest(address, di);
            byte[] response = conn.sendAndReceive(request);
            if (Objects.isNull(response) || response.length == 0) {
                throw new ReadPointException("Empty DL/T 645 response, protocol={}", driverCode);
            }
            byte control = Dlt645Frame.control(response);
            if (control != Dlt645Frame.CONTROL_READ_RESPONSE
                    && control != Dlt645Frame.CONTROL_READ_RESPONSE_MORE) {
                throw new ReadPointException("Unexpected DL/T 645 response control code, protocol={}, control={}",
                        driverCode, String.format("0x%02X", control));
            }
            byte[] data = Dlt645Frame.parse(response);
            String value = formatReadValue(data, getConfigValue(pointConfig, "dataFormat", "HEX"));
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), conn);
            throw e;
        } catch (Exception e) {
            invalidateConnector(device.getId(), conn);
            throw new ReadPointException("DL/T 645 read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        Dlt645SerialPortConnection conn = getConnector(device.getId(), driverConfig);
        try {
            int[] di = parseDi(getRequiredConfig(pointConfig, "di"));
            byte[] address = Dlt645Frame.encodeAddress(getConfigValue(driverConfig, "meterAddress", "000000000000"));
            byte[] password = parseHex(getConfigValue(driverConfig, "password", "00000000"), 4);
            byte[] operatorCode = parseHex(getConfigValue(driverConfig, "operatorCode", "00000000"), 4);
            String dataFormat = getConfigValue(pointConfig, "dataFormat", "HEX");
            byte[] payload = encodeWriteValue(writePointValue.getValue(String.class), dataFormat);

            byte[] request = Dlt645Frame.buildWriteRequest(address, password, operatorCode, di, payload);
            byte[] response = conn.sendAndReceive(request);
            if (Objects.isNull(response) || response.length == 0) {
                return false;
            }
            return Dlt645Frame.control(response) == Dlt645Frame.CONTROL_WRITE_RESPONSE;
        } catch (Exception e) {
            invalidateConnector(device.getId(), conn);
            throw new WritePointException("DL/T 645 write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private Dlt645SerialPortConnection getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String port = getConfigValue(driverConfig, "port", "/dev/ttyUSB0");
            int baudRate = getConfigIntValue(driverConfig, "baudRate", 2400);
            int dataBits = getConfigIntValue(driverConfig, "dataBits", 8);
            int stopBits = getConfigIntValue(driverConfig, "stopBits", 1);
            int parity = getConfigIntValue(driverConfig, "parity", 2);
            int timeout = getConfigIntValue(driverConfig, "timeout", 1000);

            log.debug("Driver connection creating, protocol={}, deviceId={}, port={}, baudRate={}",
                    driverCode, deviceId, port, baudRate);
            Dlt645SerialPortConnection conn = new Dlt645SerialPortConnection(port, baudRate, dataBits, stopBits, parity,
                    timeout);
            conn.open();
            log.info("Driver connection established, protocol={}, deviceId={}, port={}", driverCode, deviceId, port);
            return conn;
        });
    }

    private int[] parseDi(String di) {
        byte[] bytes = parseHex(di, 4);
        return new int[]{ bytes[0] & 0xFF, bytes[1] & 0xFF, bytes[2] & 0xFF, bytes[3] & 0xFF };
    }

    private byte[] parseHex(String hex, int expectedLength) {
        if (Objects.isNull(hex) || hex.length() != expectedLength * 2 || !hex.matches("[0-9a-fA-F]+")) {
            throw new ConnectorException("Invalid hexadecimal value: {}", hex);
        }
        byte[] result = new byte[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private String formatReadValue(byte[] data, String dataFormat) {
        return switch (dataFormat.toUpperCase()) {
            case "BCD" -> toBcdString(data);
            case "INT" -> String.valueOf(toInt(data));
            case "FLOAT" -> String.valueOf(toFloat(data));
            case "ASCII" -> new String(data, StandardCharsets.US_ASCII);
            default -> toHexString(data);
        };
    }

    private byte[] encodeWriteValue(String value, String dataFormat) {
        return switch (dataFormat.toUpperCase()) {
            case "BCD" -> fromBcdString(value);
            case "INT" -> fromInt(new BigInteger(value).longValue());
            case "FLOAT" -> fromFloat(Float.parseFloat(value));
            case "ASCII" -> value.getBytes(StandardCharsets.US_ASCII);
            default -> parseHex(value, value.replace(" ", "").length() / 2);
        };
    }

    private String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private String toBcdString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append((char) ('0' + ((b >> 4) & 0x0F)));
            sb.append((char) ('0' + (b & 0x0F)));
        }
        return sb.toString();
    }

    private long toInt(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        return switch (data.length) {
            case 1 -> bb.get() & 0xFF;
            case 2 -> bb.getShort() & 0xFFFF;
            case 4 -> bb.getInt();
            case 8 -> bb.getLong();
            default -> new BigInteger(data).longValue();
        };
    }

    private double toFloat(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        return data.length == 4 ? bb.getFloat() : bb.getDouble();
    }

    private byte[] fromBcdString(String value) {
        if (value.length() % 2 != 0) {
            value = "0" + value;
        }
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = value.charAt(i * 2) - '0';
            int low = value.charAt(i * 2 + 1) - '0';
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private byte[] fromInt(long value) {
        ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        bb.putLong(value);
        return bb.array();
    }

    private byte[] fromFloat(float value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        bb.putFloat(value);
        return bb.array();
    }

    private void invalidateConnector(Long deviceId, Dlt645SerialPortConnection conn) {
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

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "meterAddress", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "di", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}
