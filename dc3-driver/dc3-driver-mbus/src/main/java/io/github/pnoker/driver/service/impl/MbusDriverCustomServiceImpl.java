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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Custom driver service implementation for the M-Bus (Meter-Bus) driver.
 * <p>
 * Manages one serial connection per meter, sends REQ_UD2 requests, and decodes
 * DIF/VIF data records from the RSP_UD response. Writes use SND_UD frames and
 * expect a single-byte ACK.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MbusDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, MbusSerialPortConnection> connectMap;

    private static void checkRequired(
            Map<String, AttributeBO> config, String code, List<ValidationReport.AttributeIssue> issues) {
        AttributeBO attr = config.get(code);
        if (attr == null || attr.getValue() == null) {
            issues.add(ValidationReport.AttributeIssue.builder()
                    .attributeCode(code)
                    .level(ValidationReport.IssueLevel.ERROR)
                    .message("Missing required attribute: " + code)
                    .build());
        }
    }

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // M-Bus meters are polled on the SDK read schedule; no custom task is needed.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            MbusSerialPortConnection conn = getConnector(device.getId(), driverConfig);
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
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode,
                    metadataType,
                    operateType,
                    metadataEvent.getId());
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                MbusSerialPortConnection removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    removed.close();
                    log.info(
                            "Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                            driverCode,
                            metadataEvent.getId(),
                            operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode,
                    metadataType,
                    operateType,
                    metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point) {
        MbusSerialPortConnection conn = getConnector(device.getId(), driverConfig);
        try {
            int address = getConfigIntValue(driverConfig, "primaryAddress", 0);
            int recordIndex = getConfigIntValue(pointConfig, "recordIndex", 0);
            String dataFormat = getConfigValue(pointConfig, "dataFormat", "AUTO");

            byte[] request = MbusFrame.buildReqUd2(address);
            byte[] response = conn.sendAndReceive(request);
            if (Objects.isNull(response) || response.length == 0) {
                throw new ReadPointException("Empty M-Bus response, protocol={}", driverCode);
            }
            if (MbusFrame.control(response) != MbusFrame.CONTROL_RSP_UD) {
                throw new ReadPointException(
                        "Unexpected M-Bus response control code, protocol={}, control={}",
                        driverCode,
                        String.format("0x%02X", MbusFrame.control(response)));
            }
            byte[] data = MbusFrame.parse(response);
            List<MbusRecord> records = MbusFrame.parseRecords(data);
            if (records.isEmpty() || recordIndex >= records.size()) {
                throw new ReadPointException(
                        "M-Bus record not found, protocol={}, recordIndex={}, recordCount={}",
                        driverCode,
                        recordIndex,
                        records.size());
            }
            String value = MbusFrame.decodeValue(records.get(recordIndex), dataFormat);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), conn);
            throw e;
        } catch (Exception e) {
            invalidateConnector(device.getId(), conn);
            throw new ReadPointException("M-Bus read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        MbusSerialPortConnection conn = getConnector(device.getId(), driverConfig);
        try {
            int address = getConfigIntValue(driverConfig, "primaryAddress", 0);
            byte[] payload = writePointValue.getValue(String.class).getBytes(StandardCharsets.US_ASCII);
            byte[] request = MbusFrame.buildSndUd(address, payload);
            byte[] response = conn.sendAndReceive(request);
            return Objects.nonNull(response) && response.length == 1 && (response[0] & 0xFF) == (MbusFrame.ACK & 0xFF);
        } catch (Exception e) {
            invalidateConnector(device.getId(), conn);
            throw new WritePointException("M-Bus write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private MbusSerialPortConnection getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String port = getConfigValue(driverConfig, "port", "/dev/ttyUSB0");
            int baudRate = getConfigIntValue(driverConfig, "baudRate", 2400);
            int dataBits = getConfigIntValue(driverConfig, "dataBits", 8);
            int stopBits = getConfigIntValue(driverConfig, "stopBits", 1);
            int parity = getConfigIntValue(driverConfig, "parity", 2);
            int timeout = getConfigIntValue(driverConfig, "timeout", 1000);
            log.debug(
                    "Driver connection creating, protocol={}, deviceId={}, port={}, baudRate={}",
                    driverCode,
                    deviceId,
                    port,
                    baudRate);
            MbusSerialPortConnection conn =
                    new MbusSerialPortConnection(port, baudRate, dataBits, stopBits, parity, timeout);
            conn.open();
            log.info("Driver connection established, protocol={}, deviceId={}, port={}", driverCode, deviceId, port);
            return conn;
        });
    }

    private void invalidateConnector(Long deviceId, MbusSerialPortConnection conn) {
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
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr)
                || Objects.isNull(attr.getValue())
                || attr.getValue().isEmpty()) {
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
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "recordIndex", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }
}
