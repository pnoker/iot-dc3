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

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.driver.api.S7Connector;
import io.github.pnoker.driver.api.S7Serializer;
import io.github.pnoker.driver.api.factory.S7ConnectorFactory;
import io.github.pnoker.driver.api.factory.S7SerializerFactory;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


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

    @Resource
    DriverMetadata driverMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    /**
     * Plc Connector Map
     * 仅供参考
     */
    private Map<Long, MyS7Connector> connectMap;

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
         * Finally, submit the device status to the SDK management through the {@link DriverSenderService#deviceStatusSender} interface.
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
         * PLC S7 data reading logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * This method is used to read data of the specified point from the PLC S7 device.
         * 1. Obtain the S7 connector of the device.
         * 2. Lock to ensure thread safety.
         * 3. Use the S7 serializer to read the point data.
         * 4. Package the read data as an RValue object and return it.
         * 5. Catch and log exceptions, ensuring the lock is released in the finally block.
         */
        log.debug("Plc S7 Read, device: {}, point: {}", JsonUtil.toJsonString(device), JsonUtil.toJsonString(point));
        MyS7Connector myS7Connector = getS7Connector(device.getId(), driverConfig);

        try {
            myS7Connector.lock.writeLock().lock();
            S7Serializer serializer = S7SerializerFactory.buildSerializer(myS7Connector.getConnector());
            PlcS7PointVariable plcs7PointVariable = getPointVariable(pointConfig, point.getPointTypeFlag().getCode());
            return new RValue(device, point, String.valueOf(serializer.dispense(plcs7PointVariable)));
        } catch (Exception e) {
            log.error("Plc S7 Read Error: {}", e.getMessage());
            return null;
        } finally {
            myS7Connector.lock.writeLock().unlock();
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue) {
        /*
         * PLC S7 data write logic
         *
         * Hint: The logic here is for reference only; please modify it according to the actual application scenario.
         * This method is used to write data of the specified point to the PLC S7 device.
         * 1. Obtain the S7 connector of the device.
         * 2. Lock to ensure thread safety.
         * 3. Use the S7 serializer to write the point data.
         * 4. Catch and log exceptions, ensuring the lock is released in the finally block.
         * 5. Return the result of the write operation (success or failure).
         */
        log.debug("Plc S7 Write, device: {}, value: {}", JsonUtil.toJsonString(device), JsonUtil.toJsonString(wValue));
        MyS7Connector myS7Connector = getS7Connector(device.getId(), driverConfig);
        myS7Connector.lock.writeLock().lock();
        S7Serializer serializer = S7SerializerFactory.buildSerializer(myS7Connector.getConnector());
        PlcS7PointVariable plcs7PointVariable = getPointVariable(pointConfig, wValue.getType().getCode());

        try {
            store(serializer, plcs7PointVariable, wValue.getType().getCode(), wValue.getValue());
            return true;
        } catch (Exception e) {
            log.error("Plc S7 Write Error: {}", e.getMessage());
            return false;
        } finally {
            myS7Connector.lock.writeLock().unlock();
        }
    }

    /**
     * Get PLC S7 Connector
     * <p>
     * This method retrieves the S7 connector for the specified device from the cache. If the connector
     * is not present in the cache, a new one is created based on the driver configuration and then
     * cached for subsequent use.
     * <p>
     * During connector creation, the host address and port number are obtained from the driver
     * configuration, and a read-write lock is initialized to ensure thread safety. If connector
     * creation fails, a {@link ServiceException} is thrown.
     *
     * @param deviceId     Device ID used to identify the unique device connector
     * @param driverConfig Driver configuration containing parameters such as host address and port
     *                     required to connect to the PLC
     * @return The {@link MyS7Connector} object corresponding to the device ID, containing the S7
     * connector and read-write lock
     * @throws ServiceException Thrown if connector creation fails
     */
    private MyS7Connector getS7Connector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        MyS7Connector myS7Connector = connectMap.get(deviceId);
        if (Objects.isNull(myS7Connector)) {
            myS7Connector = new MyS7Connector();

            log.debug("Plc S7 Connection Info {}", JsonUtil.toJsonString(driverConfig));
            try {
                S7Connector s7Connector = S7ConnectorFactory.buildTCPConnector()
                        .withHost(driverConfig.get("host").getValue(String.class))
                        .withPort(driverConfig.get("port").getValue(Integer.class))
                        .build();
                myS7Connector.setLock(new ReentrantReadWriteLock());
                myS7Connector.setConnector(s7Connector);
            } catch (Exception e) {
                throw new ServiceException("new s7connector fail" + e.getMessage());
            }
            connectMap.put(deviceId, myS7Connector);
        }
        return myS7Connector;
    }

    /**
     * Get PLC S7 point variable information
     * <p>
     * This method extracts PLC S7 point variable information from the point configuration
     * and encapsulates it into a {@link PlcS7PointVariable} object.
     * The point configuration should contain the following key attributes:
     * - dbNum: data block number
     * - byteOffset: byte offset
     * - bitOffset: bit offset
     * - blockSize: data block size
     * - type: point data type
     * <p>
     * If any of the above attributes is missing in the point configuration,
     * a {@link NullPointerException} will be thrown.
     *
     * @param pointConfig point configuration information, containing related attributes of the point variable
     * @param type        point data type, used to identify the type of point data
     * @return the encapsulated {@link PlcS7PointVariable} object, containing detailed information of the point variable
     * @throws NullPointerException if any necessary attribute is missing in the point configuration
     */
    private PlcS7PointVariable getPointVariable(Map<String, AttributeBO> pointConfig, String type) {
        log.debug("Plc S7 Point Attribute Config {}", JsonUtil.toJsonString(pointConfig));
        return new PlcS7PointVariable(
                pointConfig.get("dbNum").getValue(Integer.class),
                pointConfig.get("byteOffset").getValue(Integer.class),
                pointConfig.get("bitOffset").getValue(Integer.class),
                pointConfig.get("blockSize").getValue(Integer.class),
                type);
    }

    /**
     * Write data to PLC S7
     * <p>
     * This method is used to write data of a specified type to a designated point on the PLC S7.
     * 1. Obtain the corresponding {@link AttributeTypeFlagEnum} enum value based on the type string.
     * 2. If the type is not supported, throw an {@link UnSupportException}.
     * 3. Convert the string value to the corresponding Java type based on the type.
     * 4. Use {@link S7Serializer} to write the data to the specified data block and byte offset on the PLC S7.
     * <p>
     * Supported data types include:
     * - INT: integer
     * - LONG: long integer
     * - FLOAT: single-precision floating point
     * - DOUBLE: double-precision floating point
     * - BOOLEAN: boolean
     * - STRING: string
     *
     * @param serializer         S7 serializer, used for data interaction with PLC S7
     * @param plcS7PointVariable PLC S7 point variable information, including data block number, byte offset, etc.
     * @param type               Data type string, used to identify the type of data to be written
     * @param value              The data value in string format to be written
     * @throws UnSupportException Thrown if the data type is not supported
     */
    private void store(S7Serializer serializer, PlcS7PointVariable plcS7PointVariable, String type, String value) {
        AttributeTypeFlagEnum valueType = AttributeTypeFlagEnum.ofCode(type);
        if (Objects.isNull(valueType)) {
            throw new UnSupportException("Unsupported type of " + type);
        }
        AttributeBO attributeBOConfig = new AttributeBO(value, valueType);

        switch (valueType) {
            case INT:
                int intValue = attributeBOConfig.getValue(Integer.class);
                serializer.store(intValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case LONG:
                long longValue = attributeBOConfig.getValue(Long.class);
                serializer.store(longValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case FLOAT:
                float floatValue = attributeBOConfig.getValue(Float.class);
                serializer.store(floatValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case DOUBLE:
                double doubleValue = attributeBOConfig.getValue(Double.class);
                serializer.store(doubleValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case BOOLEAN:
                boolean booleanValue = attributeBOConfig.getValue(Boolean.class);
                serializer.store(booleanValue, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            case STRING:
                serializer.store(value, plcS7PointVariable.getDbNum(), plcS7PointVariable.getByteOffset());
                break;
            default:
                break;
        }
    }

    /**
     * MyS7Connector inner class
     * <p>
     * This class is used to encapsulate information related to the PLC S7 connection, including the read-write lock and the S7 connector.
     * The read-write lock {@link ReentrantReadWriteLock} is used to ensure thread-safe operations on the S7 connector in a multi-threaded environment.
     * The S7 connector {@link S7Connector} is used to communicate with the PLC S7 device.
     * <p>
     * This class provides a no-argument constructor and an all-argument constructor, and uses Lombok annotations to automatically generate getter and setter methods.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MyS7Connector {
        private ReentrantReadWriteLock lock;
        private S7Connector connector;
    }

}
