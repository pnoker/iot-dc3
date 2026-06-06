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

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.WriteCoilRequest;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
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
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the Modbus RTU driver.
 * <p>
 * Manages Modbus RTU serial connections, reads point values from Modbus devices
 * via function codes 1-4, and writes values to coils and holding registers.
 * Each RTU device is connected via a dedicated serial port.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModbusRtuDriverCustomServiceImpl implements DriverCustomService {

    /**
     * Max consecutive failures before entering backoff.
     */
    private static final int FAILURE_BACKOFF_THRESHOLD = 3;
    /**
     * Backoff duration in milliseconds after exceeding the failure threshold.
     */
    private static final long FAILURE_BACKOFF_MS = 60_000;
    /**
     * Modbus factory for creating ModbusMaster instances.
     */
    private static final ModbusFactory modbusFactory = new ModbusFactory();
    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;
    /**
     * Cache of device ID to ModbusMaster connections.
     */
    private Map<Long, ModbusMaster> connectMap;
    /**
     * Failure tracking for connection backoff to prevent repeated connection
     * attempts to unreachable devices on every schedule cycle.
     */
    private Map<Long, ConsecutiveFailure> failureMap;

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
        failureMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        Long deviceId = device.getId();
        // If connection exists and is initialized, it's online
        ModbusMaster existing = connectMap.get(deviceId);
        if (existing != null) {
            return existing.isInitialized()
                    ? DeviceHealthState.online()
                    : DeviceHealthState.offline();
        }
        // If in backoff, report offline without attempting a new connection
        ConsecutiveFailure failure = failureMap.get(deviceId);
        if (failure != null && failure.shouldBackoff()) {
            return DeviceHealthState.offline();
        }
        // Last resort: try to create connection
        try {
            return getConnector(deviceId, driverConfig).isInitialized()
                    ? DeviceHealthState.online()
                    : DeviceHealthState.offline();
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
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}", driverCode,
                    metadataType, operateType, metadataEvent.getId());

            // Remove stale connection when device is updated or deleted
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                ModbusMaster removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    removed.destroy();
                    log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}", driverCode,
                            metadataEvent.getId(), operateType);
                }
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}", driverCode,
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        ModbusMaster modbusMaster = getConnector(device.getId(), driverConfig);
        try {
            return new ReadPointValue(device, point, readValue(modbusMaster, pointConfig, point.getPointTypeFlag().getCode()));
        } catch (ReadPointException e) {
            invalidateConnector(device.getId(), modbusMaster);
            throw e;
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        ModbusMaster modbusMaster = getConnector(device.getId(), driverConfig);
        try {
            return writeValue(modbusMaster, pointConfig, writePointValue);
        } catch (WritePointException e) {
            invalidateConnector(device.getId(), modbusMaster);
            throw e;
        }
    }

    /**
     * Get or create a Modbus RTU connection for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration containing serial port parameters
     * @return cached or newly created ModbusMaster
     * @throws ConnectorException if connection initialization fails
     */
    private ModbusMaster getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        // Check backoff before attempting connection
        ConsecutiveFailure failure = failureMap.get(deviceId);
        if (failure != null && failure.shouldBackoff()) {
            throw new ConnectorException(
                    "Driver connection in backoff after {} consecutive failures, protocol={}, deviceId={}",
                    failure.count, driverCode, deviceId);
        }

        return connectMap.computeIfAbsent(deviceId, id -> {
            String port = driverConfig.get("port").getValue(String.class);
            int baudRate = driverConfig.get("baudRate").getValue(Integer.class);
            int dataBits = driverConfig.get("dataBits").getValue(Integer.class);
            int stopBits = driverConfig.get("stopBits").getValue(Integer.class);
            int parity = driverConfig.get("parity").getValue(Integer.class);
            log.debug("Driver connection creating, protocol={}, deviceId={}, port={}, baudRate={}", driverCode, deviceId,
                    port, baudRate);
            JSerialCommWrapper wrapper = new JSerialCommWrapper(port, baudRate, dataBits, stopBits, parity);
            ModbusMaster modbusMaster = modbusFactory.createRtuMaster(wrapper);
            try {
                modbusMaster.init();
                log.info("Driver connection established, protocol={}, deviceId={}, port={}, baudRate={}", driverCode,
                        deviceId, port, baudRate);
                // Successful connection clears failure tracking
                failureMap.remove(deviceId);
            } catch (ModbusInitException e) {
                try {
                    modbusMaster.destroy();
                } catch (Exception destroyException) {
                    log.warn("Driver connection destroy failed after init error, protocol={}, deviceId={}, port={}", driverCode,
                            deviceId, port, destroyException);
                }
                // Record failure for backoff
                failureMap.compute(deviceId, (k, v) ->
                        v == null ? new ConsecutiveFailure() : v.increment());
                log.error("Driver connection failed, protocol={}, deviceId={}, port={}, baudRate={}", driverCode, deviceId,
                        port, baudRate, e);
                throw new ConnectorException("Driver connection failed, protocol=" + driverCode + ", deviceId={}, port={}, message={}",
                        deviceId, port, e.getMessage(), e);
            }
            return modbusMaster;
        });
    }

    /**
     * Read a point value from the Modbus device by function code.
     * <p>
     * Function codes: 1=coil, 2=input status, 3=holding register, 4=input register.
     *
     * @param modbusMaster active Modbus connection
     * @param pointConfig  point configuration (slaveId, functionCode, offset)
     * @param type         point value type for register data interpretation
     * @return read value as string, or "0" for unsupported function codes
     */
    private String readValue(ModbusMaster modbusMaster, Map<String, AttributeBO> pointConfig, String type) {
        int slaveId = pointConfig.get("slaveId").getValue(Integer.class);
        int functionCode = pointConfig.get("functionCode").getValue(Integer.class);
        int offset = pointConfig.get("offset").getValue(Integer.class);
        switch (functionCode) {
            case 1:
                BaseLocator<Boolean> coilLocator = BaseLocator.coilStatus(slaveId, offset);
                Boolean coilValue = getMasterValue(modbusMaster, coilLocator);
                return String.valueOf(coilValue);
            case 2:
                BaseLocator<Boolean> inputLocator = BaseLocator.inputStatus(slaveId, offset);
                Boolean inputStatusValue = getMasterValue(modbusMaster, inputLocator);
                return String.valueOf(inputStatusValue);
            case 3:
                BaseLocator<Number> holdingLocator = BaseLocator.holdingRegister(slaveId, offset, getValueType(type));
                Number holdingValue = getMasterValue(modbusMaster, holdingLocator);
                return String.valueOf(holdingValue);
            case 4:
                BaseLocator<Number> inputRegister = BaseLocator.inputRegister(slaveId, offset, getValueType(type));
                Number inputRegisterValue = getMasterValue(modbusMaster, inputRegister);
                return String.valueOf(inputRegisterValue);
            default:
                log.warn("Unsupported Modbus function code, slaveId={}, functionCode={}, offset={}", slaveId,
                        functionCode, offset);
                throw new UnSupportException("Unsupported Modbus read function code: " + functionCode);
        }
    }

    /**
     * Read a value from the Modbus device using the given locator.
     *
     * @param modbusMaster active Modbus connection
     * @param locator      identifies the target point (slave, function, offset)
     * @param <T>          value type determined by the locator
     * @return the read value
     * @throws ReadPointException if a transport or error response occurs
     */
    private <T> T getMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator) {
        try {
            return modbusMaster.getValue(locator);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Driver point read failed, protocol={}", driverCode, e);
            throw new ReadPointException("Driver point read failed, protocol=" + driverCode + ", message={}", e.getMessage(),
                    e);
        }
    }

    /**
     * Write a point value to the Modbus device by function code.
     * <p>
     * Function codes: 1=write coil, 3=write holding register. Others return false.
     *
     * @param modbusMaster    active Modbus connection
     * @param pointConfig     point configuration (slaveId, functionCode, offset)
     * @param writePointValue value to write
     * @return true if write succeeded, false if failed or unsupported function code
     */
    private boolean writeValue(ModbusMaster modbusMaster, Map<String, AttributeBO> pointConfig, WritePointValue writePointValue) {
        int slaveId = pointConfig.get("slaveId").getValue(Integer.class);
        int functionCode = pointConfig.get("functionCode").getValue(Integer.class);
        int offset = pointConfig.get("offset").getValue(Integer.class);
        switch (functionCode) {
            case 1:
                WriteCoilResponse coilResponse = setMasterValue(modbusMaster, slaveId, offset, writePointValue);
                return !coilResponse.isException();
            case 3:
                BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset,
                        getValueType(writePointValue.getType().getCode()));
                setMasterValue(modbusMaster, locator, writePointValue);
                return true;
            default:
                log.warn("Unsupported Modbus write function code, slaveId={}, functionCode={}, offset={}", slaveId,
                        functionCode, offset);
                return false;
        }
    }

    /**
     * Map a point type flag to a Modbus DataType constant.
     * <p>
     * LONG->4-byte int, FLOAT->4-byte float, DOUBLE->8-byte float, else 2-byte int.
     *
     * @param type point type code
     * @return Modbus DataType constant
     * @throws UnSupportException if the type is unknown
     */
    private int getValueType(String type) {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(type);
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + type);
        }

        return switch (valueType) {
            case LONG -> DataType.FOUR_BYTE_INT_SIGNED;
            case FLOAT -> DataType.FOUR_BYTE_FLOAT;
            case DOUBLE -> DataType.EIGHT_BYTE_FLOAT;
            default -> DataType.TWO_BYTE_INT_SIGNED;
        };
    }

    /**
     * Write a boolean value to a Modbus coil.
     *
     * @param modbusMaster    active Modbus connection
     * @param slaveId         target slave address
     * @param offset          coil offset
     * @param writePointValue value containing the boolean to write
     * @return the coil write response
     * @throws WritePointException if a transport error occurs
     */
    private WriteCoilResponse setMasterValue(ModbusMaster modbusMaster, int slaveId, int offset, WritePointValue writePointValue) {
        try {
            WriteCoilRequest coilRequest = new WriteCoilRequest(slaveId, offset, writePointValue.getValue(Boolean.class));
            return (WriteCoilResponse) modbusMaster.send(coilRequest);
        } catch (ModbusTransportException e) {
            log.error("Driver point write failed, protocol={}, slaveId={}, offset={}", driverCode, slaveId, offset, e);
            throw new WritePointException("Driver point write failed, protocol=" + driverCode + ", slaveId={}, offset={}, message={}",
                    slaveId, offset, e.getMessage(), e);
        }
    }

    /**
     * Write a numeric value to a Modbus holding register via the given locator.
     *
     * @param modbusMaster    active Modbus connection
     * @param locator         identifies the target register
     * @param writePointValue value to write
     * @param <T>             value type determined by the locator
     * @throws WritePointException if a transport or error response occurs
     */
    private <T> void setMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator, WritePointValue writePointValue) {
        try {
            PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(writePointValue.getType().getCode());
            if (Objects.isNull(valueType)) {
                throw new UnSupportException("Unsupported type of " + writePointValue.getType());
            }
            Number value = switch (valueType) {
                case INT -> writePointValue.getValue(Integer.class);
                case LONG -> writePointValue.getValue(Long.class);
                case FLOAT -> writePointValue.getValue(Float.class);
                case DOUBLE -> writePointValue.getValue(Double.class);
                default -> writePointValue.getValue(Float.class);
            };
            modbusMaster.setValue(locator, value);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Driver point write failed, protocol={}", driverCode, e);
            throw new WritePointException("Driver point write failed, protocol=" + driverCode + ", message={}", e.getMessage(),
                    e);
        }
    }

    private void invalidateConnector(Long deviceId, ModbusMaster modbusMaster) {
        connectMap.remove(deviceId, modbusMaster);
        try {
            modbusMaster.destroy();
        } catch (Exception e) {
            log.warn("Driver connection destroy failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "baudRate", issues);
        checkRequired(driverConfig, "dataBits", issues);
        checkRequired(driverConfig, "stopBits", issues);
        checkRequired(driverConfig, "parity", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "slaveId", issues);
        checkRequired(pointConfig, "functionCode", issues);
        checkRequired(pointConfig, "offset", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    /**
     * Tracks consecutive connection failures for backoff.
     */
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