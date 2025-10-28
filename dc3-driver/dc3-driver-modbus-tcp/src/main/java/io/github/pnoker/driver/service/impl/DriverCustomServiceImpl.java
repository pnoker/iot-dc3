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
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.WriteCoilRequest;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * Drive custom service implementation classes
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverCustomServiceImpl implements DriverCustomService {

    static ModbusFactory modbusFactory;

    static {
        modbusFactory = new ModbusFactory();
    }

    @Resource
    DriverMetadata driverMetadata;
    @Resource
    private DriverSenderService driverSenderService;
    private Map<Long, ModbusMaster> connectMap;

    @Override
    public void initial() {
        /*
         * Driver initialization logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * This method is automatically executed when the driver starts, and you can perform specific initialization operations here.
         *
         */
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        /*
         * Device status upload logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * Device status upload can be flexibly implemented based on specific requirements. Here are some common approaches:
         * - Determine device status based on read data in the `read` method;
         * - Periodically check device status in a custom scheduled task;
         * - Trigger device status judgment based on specific business logic or events.
         *
         * Finally, submit the device status to the SDK management through the {@link DriverSenderService#deviceStatusSender(Long, DeviceStatusEnum)} interface.
         * The device status enumeration {@link DeviceStatusEnum} includes the following states:
         * - ONLINE: Device online
         * - OFFLINE: Device offline
         * - MAINTAIN: Device under maintenance
         * - FAULT: Device fault
         *
         * In the following example, all devices are set to {@link DeviceStatusEnum#ONLINE}, with a status validity period of 25 {@link TimeUnit#SECONDS}.
         */
        driverMetadata.getDeviceIds().forEach(id -> driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * Receive metadata events for driver, device, and point creation, update, and deletion.
         *
         * Metadata type: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT)
         * Metadata operation type: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Device metadata event: deviceId: {}, operate: {}", metadataEvent.getId(), operateType);

            // When the device is updated or deleted, remove the corresponding connection handle
            if (MetadataOperateTypeEnum.DELETE.equals(operateType) || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                connectMap.remove(metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Point metadata event: pointId: {}, operate: {}", metadataEvent.getId(), operateType);
        }
    }

    @Override
    public RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point) {
        /*
         * Read device point data
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * Read the specified device point data through the Modbus connector and return an RValue object.
         * The RValue object contains device information, point information, and the read value.
         */
        return new RValue(device, point, readValue(getConnector(device.getId(), driverConfig), pointConfig, point.getPointTypeFlag().getCode()));
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * Write device point data
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * Write the specified value to the device point through the Modbus connector and return the write result.
         */
        ModbusMaster modbusMaster = getConnector(device.getId(), driverConfig);
        return writeValue(modbusMaster, pointConfig, wValue);
    }

    /**
     * Get Modbus Master connector
     * <p>
     * This method is used to obtain or create a Modbus Master connector based on the device ID and driver configuration.
     * If the connector already exists, it will be returned directly; otherwise, a new connector will be created and initialized according to the configuration.
     * If initialization fails, the connector will be removed and an exception will be thrown.
     *
     * @param deviceId     Device ID, used to identify a unique device connection
     * @param driverConfig Driver configuration, including the host address and port number required to connect to the Modbus device
     * @return ModbusMaster Returns the Modbus Master connector associated with the device
     * @throws ConnectorException If the connector initialization fails, this exception will be thrown
     */
    private ModbusMaster getConnector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        log.debug("Modbus Tcp Connection Info: {}", JsonUtil.toJsonString(driverConfig));
        ModbusMaster modbusMaster = connectMap.get(deviceId);
        if (Objects.isNull(modbusMaster)) {
            IpParameters params = new IpParameters();
            params.setHost(driverConfig.get("host").getValue(String.class));
            params.setPort(driverConfig.get("port").getValue(Integer.class));
            modbusMaster = modbusFactory.createTcpMaster(params, true);
            try {
                modbusMaster.init();
                connectMap.put(deviceId, modbusMaster);
            } catch (ModbusInitException e) {
                connectMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                log.error("Connect modbus master error: {}", e.getMessage(), e);
                throw new ConnectorException(e.getMessage());
            }
        }
        return modbusMaster;
    }

    /**
     * Read Modbus device point value
     * <p>
     * According to the function code(functionCode) and offset in the point configuration, read the corresponding type of value from the Modbus device.
     * Supported function codes include:
     * - 1: Read Coil Status
     * - 2: Read Input Status
     * - 3: Read Holding Register
     * - 4: Read Input Register
     *
     * @param modbusMaster ModbusMaster connector for communication with the device
     * @param pointConfig  Point configuration, including slaveId, functionCode, offset, etc.
     * @param type         Point value type, used to determine how to parse data in the register
     * @return String Returns the read point value as a string. If the function code is not supported, returns "0".
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
                return "0";
        }
    }

    /**
     * Read data of a specified point from the ModbusMaster connector
     * <p>
     * This method reads data from the ModbusMaster connector via the given {@link BaseLocator}.
     * If a {@link ModbusTransportException} or {@link ErrorResponseException} occurs during reading,
     * an error log is recorded and a {@link ReadPointException} is thrown.
     *
     * @param modbusMaster ModbusMaster connector for communication with the device
     * @param locator      Point locator, containing slave ID, function code, offset, etc.
     * @param <T>          Return value type, determined by the point data type
     * @return T           The read point data
     * @throws ReadPointException Thrown if an exception occurs during reading
     */
    private <T> T getMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator) {
        try {
            return modbusMaster.getValue(locator);
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Read modbus master value error: {}", e.getMessage(), e);
            throw new ReadPointException(e.getMessage());
        }
    }

    /**
     * Write point value to Modbus device
     * <p>
     * According to the function code and offset in the point configuration, write the specified value to the corresponding point of the Modbus device.
     * Supported function codes include:
     * - 1: Write Coil Status
     * - 3: Write Holding Register
     * <p>
     * For function code 1, write a boolean value to the coil status and return the write result.
     * For function code 3, write a numeric value to the holding register and return the write success status.
     * Other function codes are not supported and return false.
     *
     * @param modbusMaster ModbusMaster connector for communication with the device
     * @param pointConfig  Point configuration, including slave ID, function code, offset, etc.
     * @param wValue       Value to be written, including value type and specific value
     * @return boolean Write result, true indicates successful write, false indicates failed write or unsupported function code
     */
    private boolean writeValue(ModbusMaster modbusMaster, Map<String, AttributeBO> pointConfig, WValue wValue) {
        int slaveId = pointConfig.get("slaveId").getValue(Integer.class);
        int functionCode = pointConfig.get("functionCode").getValue(Integer.class);
        int offset = pointConfig.get("offset").getValue(Integer.class);
        switch (functionCode) {
            case 1:
                WriteCoilResponse coilResponse = setMasterValue(modbusMaster, slaveId, offset, wValue);
                return !coilResponse.isException();
            case 3:
                BaseLocator<Number> locator = BaseLocator.holdingRegister(slaveId, offset, getValueType(wValue.getType().getCode()));
                setMasterValue(modbusMaster, locator, wValue);
                return true;
            default:
                return false;
        }
    }

    /**
     * Get Modbus data type
     * <p>
     * Return the corresponding Modbus data type based on the point value type.
     * Supported point value types include:
     * - {@link PointTypeFlagEnum#LONG}: returns 4-byte signed integer ({@link DataType#FOUR_BYTE_INT_SIGNED})
     * - {@link PointTypeFlagEnum#FLOAT}: returns 4-byte float ({@link DataType#FOUR_BYTE_FLOAT})
     * - {@link PointTypeFlagEnum#DOUBLE}: returns 8-byte float ({@link DataType#EIGHT_BYTE_FLOAT})
     * - Other types: return 2-byte signed integer ({@link DataType#TWO_BYTE_INT_SIGNED})
     * <p>
     * Hint: This method can be extended according to actual project requirements, such as supporting byte swapping, big/little-endian mode, etc.
     *
     * @param type Point value type, used to determine the Modbus data type
     * @return int Returns the corresponding Modbus data type
     * @throws UnSupportException If the point value type is not supported, this exception is thrown
     */
    private int getValueType(String type) {
        PointTypeFlagEnum valueType = PointTypeFlagEnum.ofCode(type);
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + type);
        }

        switch (valueType) {
            case LONG:
                return DataType.FOUR_BYTE_INT_SIGNED;
            case FLOAT:
                return DataType.FOUR_BYTE_FLOAT;
            case DOUBLE:
                return DataType.EIGHT_BYTE_FLOAT;
            default:
                return DataType.TWO_BYTE_INT_SIGNED;
        }
    }

    /**
     * Write coil status value to Modbus device
     * <p>
     * This method writes a boolean value to the coil (offset) of the specified slave (slaveId) via the ModbusMaster connector.
     * If a {@link ModbusTransportException} occurs during the write process, an error log is recorded and a {@link WritePointException} is thrown.
     *
     * @param modbusMaster ModbusMaster connector for communication with the device
     * @param slaveId      Slave ID, identifying the target device
     * @param offset       Coil offset, identifying the target coil
     * @param wValue       Value to be written, containing the boolean value
     * @return WriteCoilResponse Returns the response result of the write operation
     * @throws WritePointException Thrown if an exception occurs during the write process
     */
    private WriteCoilResponse setMasterValue(ModbusMaster modbusMaster, int slaveId, int offset, WValue wValue) {
        try {
            WriteCoilRequest coilRequest = new WriteCoilRequest(slaveId, offset, wValue.getValue(Boolean.class));
            return (WriteCoilResponse) modbusMaster.send(coilRequest);
        } catch (ModbusTransportException e) {
            log.error("Write modbus master value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * Write a value of the specified type to the Modbus device
     * <p>
     * This method writes a value to a designated point via the ModbusMaster connector. The point information
     * is defined by {@link BaseLocator}, and the value type to be written is specified by {@link WValue}.
     * It supports writing floating-point data.
     * <p>
     * If a {@link ModbusTransportException} or {@link ErrorResponseException} occurs during the write process,
     * an error log is recorded and a {@link WritePointException} is thrown.
     *
     * @param modbusMaster ModbusMaster connector used to communicate with the device
     * @param locator      Point locator containing slave ID, function code, offset, etc.
     * @param wValue       Value to be written, including value type and specific numeric value
     * @param <T>          Return value type determined by the point's data type
     * @throws WritePointException Thrown if an exception occurs during the write process
     */
    private <T> void setMasterValue(ModbusMaster modbusMaster, BaseLocator<T> locator, WValue wValue) {
        try {
            modbusMaster.setValue(locator, wValue.getValue(Float.class));
        } catch (ModbusTransportException | ErrorResponseException e) {
            log.error("Write modbus master value error: {}", e.getMessage(), e);
            throw new WritePointException(e.getMessage());
        }
    }

}
