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
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.driver.api.S7Connector;
import io.github.pnoker.driver.api.S7Serializer;
import io.github.pnoker.driver.api.factory.S7ConnectorFactory;
import io.github.pnoker.driver.api.factory.S7SerializerFactory;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Custom driver service implementation for the Siemens S7 PLC driver.
 * <p>
 * This service provides S7 PLC-specific device communication capabilities. It manages TCP
 * connections to PLC devices, handles read/write operations to data blocks (DB), and
 * ensures thread-safe access using read-write locks. The driver supports various data
 * types including INT, LONG, FLOAT, DOUBLE, BOOLEAN, and STRING.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlcS7DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;

    private final DriverSenderService driverSenderService;

    /**
     * Cache of device ID to S7 connector instances.
     */
    private Map<Long, MyS7Connector> connectMap;

    @Override
    public void initial() {
        connectMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();

        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=plcS7, metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());

            // Remove stale connection when device is updated or deleted
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                MyS7Connector removed = connectMap.remove(metadataEvent.getId());
                log.info("Driver connection invalidated, protocol=plcS7, deviceId={}, operateType={}, removed={}",
                        metadataEvent.getId(), operateType, Objects.nonNull(removed));
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol=plcS7, metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        log.debug("Driver point read requested, protocol=plcS7, deviceId={}, pointId={}, pointType={}", device.getId(),
                point.getId(), point.getPointTypeFlag());
        MyS7Connector myS7Connector = getS7Connector(device.getId(), driverConfig);

        try {
            myS7Connector.lock.writeLock().lock();
            S7Serializer serializer = S7SerializerFactory.buildSerializer(myS7Connector.getConnector());
            PlcS7PointVariable plcs7PointVariable = getPointVariable(pointConfig, point.getPointTypeFlag().getCode());
            return new ReadPointValue(device, point, String.valueOf(serializer.dispense(plcs7PointVariable)));
        } catch (Exception e) {
            log.error("Driver point read failed, protocol=plcS7, deviceId={}, pointId={}", device.getId(),
                    point.getId(), e);
            return null;
        } finally {
            myS7Connector.lock.writeLock().unlock();
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        log.debug("Driver point write requested, protocol=plcS7, deviceId={}, pointId={}, pointType={}, valueLength={}",
                device.getId(), point.getId(), writePointValue.getType(), Objects.toString(writePointValue.getValue(), "").length());
        MyS7Connector myS7Connector = getS7Connector(device.getId(), driverConfig);
        myS7Connector.lock.writeLock().lock();
        S7Serializer serializer = S7SerializerFactory.buildSerializer(myS7Connector.getConnector());
        PlcS7PointVariable plcs7PointVariable = getPointVariable(pointConfig, writePointValue.getType().getCode());

        try {
            store(serializer, plcs7PointVariable, writePointValue.getType().getCode(), writePointValue.getValue());
            return true;
        } catch (Exception e) {
            log.error("Driver point write failed, protocol=plcS7, deviceId={}, pointId={}", device.getId(),
                    point.getId(), e);
            return false;
        } finally {
            myS7Connector.lock.writeLock().unlock();
        }
    }

    /**
     * Get or create an S7 TCP connector for the given device.
     *
     * @param deviceId     unique device identifier
     * @param driverConfig driver configuration (host, port)
     * @return wrapper containing the connector and its read-write lock
     * @throws ServiceException if connector creation fails
     */
    private MyS7Connector getS7Connector(Long deviceId, Map<String, AttributeBO> driverConfig) {
        MyS7Connector myS7Connector = connectMap.get(deviceId);
        if (Objects.isNull(myS7Connector)) {
            myS7Connector = new MyS7Connector();

            String host = driverConfig.get("host").getValue(String.class);
            int port = driverConfig.get("port").getValue(Integer.class);
            log.debug("Driver connection creating, protocol=plcS7, deviceId={}, host={}, port={}", deviceId, host,
                    port);
            try {
                S7Connector s7Connector = S7ConnectorFactory.buildTCPConnector()
                        .withHost(host)
                        .withPort(port)
                        .build();
                myS7Connector.setLock(new ReentrantReadWriteLock());
                myS7Connector.setConnector(s7Connector);
            } catch (Exception e) {
                log.error("Driver connection failed, protocol=plcS7, deviceId={}, host={}, port={}", deviceId, host,
                        port, e);
                throw new ServiceException("Driver connection failed, protocol=plcS7, deviceId={}, host={}, port={}",
                        deviceId, host, port, e);
            }
            connectMap.put(deviceId, myS7Connector);
            log.info("Driver connection established, protocol=plcS7, deviceId={}, host={}, port={}", deviceId, host,
                    port);
        }
        return myS7Connector;
    }

    /**
     * Build a PlcS7PointVariable from point configuration attributes.
     *
     * @param pointConfig point attributes (dbNum, byteOffset, bitOffset, blockSize)
     * @param type        S7 data type code
     * @return the point variable definition
     */
    private PlcS7PointVariable getPointVariable(Map<String, AttributeBO> pointConfig, String type) {
        int dbNum = pointConfig.get("dbNum").getValue(Integer.class);
        int byteOffset = pointConfig.get("byteOffset").getValue(Integer.class);
        int bitOffset = pointConfig.get("bitOffset").getValue(Integer.class);
        int blockSize = pointConfig.get("blockSize").getValue(Integer.class);
        log.debug("Driver point config resolved, protocol=plcS7, dbNum={}, byteOffset={}, bitOffset={}, blockSize={}",
                dbNum, byteOffset, bitOffset, blockSize);
        return new PlcS7PointVariable(dbNum, byteOffset, bitOffset, blockSize, type);
    }

    /**
     * Write a typed value to an S7 data block.
     * <p>
     * Supports INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING.
     *
     * @param serializer         active S7 serializer
     * @param plcS7PointVariable target point variable (DB number, offsets)
     * @param type               data type code
     * @param value              string representation of the value to write
     * @throws UnSupportException if the type is unsupported
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
     * Wrapper holding an S7 connector and its thread-safety lock.
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
