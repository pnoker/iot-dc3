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

import com.github.xingshuangs.iot.protocol.s7.enums.EPlcType;
import com.github.xingshuangs.iot.protocol.s7.service.S7PLC;
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
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.WritePointException;
import io.github.pnoker.driver.bean.PlcS7PointVariable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * S7 PLC driver service backed by the iot-communication library.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlcS7DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, MyS7PLC> connectMap;

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
                MyS7PLC removed = connectMap.remove(metadataEvent.getId());
                if (Objects.nonNull(removed)) {
                    closeConnection(metadataEvent.getId(), removed);
                }
                log.info("Driver connection invalidated, protocol={}, deviceId={}, operateType={}, removed={}",
                        driverCode, metadataEvent.getId(), operateType, Objects.nonNull(removed));
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        log.debug("Driver point read requested, protocol={}, deviceId={}, pointId={}", driverCode, device.getId(),
                point.getId());
        MyS7PLC myS7PLC = getS7PLC(device.getId(), driverConfig);
        PlcS7PointVariable variable = buildVariable(pointConfig, point.getPointTypeFlag().getCode());

        myS7PLC.lock.lock();
        try {
            Object value = readByType(myS7PLC.getPlc(), variable);
            return new ReadPointValue(device, point, String.valueOf(value));
        } catch (Exception e) {
            invalidateConnection(device.getId(), myS7PLC);
            log.error("Driver point read failed, protocol={}, deviceId={}, pointId={}", driverCode, device.getId(),
                    point.getId(), e);
            throw new ReadPointException("Driver point read failed, protocol={}, deviceId={}, pointId={}, message={}",
                    driverCode, device.getId(), point.getId(), e.getMessage(), e);
        } finally {
            myS7PLC.lock.unlock();
        }
    }

    // ------------------------------------------------------------------------
    //  private helpers
    // ------------------------------------------------------------------------

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        log.debug("Driver point write requested, protocol={}, deviceId={}, pointId={}, valueLength={}",
                driverCode, device.getId(), point.getId(), Objects.toString(writePointValue.getValue(), "").length());
        MyS7PLC myS7PLC = getS7PLC(device.getId(), driverConfig);
        PlcS7PointVariable variable = buildVariable(pointConfig, writePointValue.getType().getCode());

        myS7PLC.lock.lock();
        try {
            writeByType(myS7PLC.getPlc(), variable, writePointValue.getValue());
            return true;
        } catch (Exception e) {
            invalidateConnection(device.getId(), myS7PLC);
            log.error("Driver point write failed, protocol={}, deviceId={}, pointId={}", driverCode, device.getId(),
                    point.getId(), e);
            throw new WritePointException("Driver point write failed, protocol={}, deviceId={}, pointId={}, message={}",
                    driverCode, device.getId(), point.getId(), e.getMessage(), e);
        } finally {
            myS7PLC.lock.unlock();
        }
    }

    private MyS7PLC getS7PLC(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String host = driverConfig.get("host").getValue(String.class);
            int port = driverConfig.get("port").getValue(Integer.class);
            String plcType = driverConfig.containsKey("plcType")
                    ? driverConfig.get("plcType").getValue(String.class)
                    : "S1200";

            EPlcType ePlcType;
            try {
                ePlcType = EPlcType.valueOf(plcType);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown plcType '{}', fallback to S1200", plcType);
                ePlcType = EPlcType.S1200;
            }

            log.debug("Driver connection creating, protocol={}, deviceId={}, host={}, port={}, plcType={}",
                    driverCode, deviceId, host, port, plcType);
            try {
                S7PLC s7PLC = new S7PLC(ePlcType, host, port);
                s7PLC.setEnableReconnect(true);
                log.info("Driver connection established, protocol={}, deviceId={}, host={}, port={}, plcType={}",
                        driverCode, deviceId, host, port, plcType);
                return new MyS7PLC(new ReentrantLock(), s7PLC);
            } catch (Exception e) {
                log.error("Driver connection failed, protocol={}, deviceId={}, host={}, port={}", driverCode, deviceId,
                        host, port, e);
                throw new ServiceException("Driver connection failed, protocol={}, deviceId={}, host={}, port={}",
                        driverCode, deviceId, host, port, e);
            }
        });
    }

    private PlcS7PointVariable buildVariable(Map<String, AttributeBO> pointConfig, String type) {
        int dbNum = pointConfig.get("dbNum").getValue(Integer.class);
        int byteOffset = pointConfig.get("byteOffset").getValue(Integer.class);
        int bitOffset = pointConfig.get("bitOffset").getValue(Integer.class);
        return new PlcS7PointVariable(dbNum, byteOffset, bitOffset, type);
    }

    private Object readByType(S7PLC plc, PlcS7PointVariable variable) {
        String address = variable.getAddress();
        AttributeTypeEnum type = AttributeTypeEnum.ofCode(variable.getType());
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("Unknown type: " + variable.getType());
        }
        switch (type) {
            case BOOLEAN:
                return plc.readBoolean(address);
            case BYTE:
                return plc.readByte(address);
            case SHORT:
                return plc.readInt16(address);
            case INT:
                return plc.readInt32(address);
            case LONG:
                return plc.readInt64(address);
            case FLOAT:
                return plc.readFloat32(address);
            case DOUBLE:
                return plc.readFloat64(address);
            case STRING:
                return plc.readString(address);
            default:
                throw new IllegalArgumentException("Unsupported read type: " + type);
        }
    }

    private void writeByType(S7PLC plc, PlcS7PointVariable variable, String value) {
        String address = variable.getAddress();
        AttributeTypeEnum type = AttributeTypeEnum.ofCode(variable.getType());
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("Unknown type: " + variable.getType());
        }
        switch (type) {
            case BOOLEAN:
                plc.writeBoolean(address, Boolean.parseBoolean(value));
                break;
            case BYTE:
                plc.writeByte(address, Byte.parseByte(value));
                break;
            case SHORT:
                plc.writeInt16(address, Short.parseShort(value));
                break;
            case INT:
                plc.writeInt32(address, Integer.parseInt(value));
                break;
            case LONG:
                plc.writeInt64(address, Long.parseLong(value));
                break;
            case FLOAT:
                plc.writeFloat32(address, Float.parseFloat(value));
                break;
            case DOUBLE:
                plc.writeFloat64(address, Double.parseDouble(value));
                break;
            case STRING:
                plc.writeString(address, value);
                break;
            default:
                throw new IllegalArgumentException("Unsupported write type: " + type);
        }
    }

    private void invalidateConnection(Long deviceId, MyS7PLC myS7PLC) {
        connectMap.remove(deviceId, myS7PLC);
        closeConnection(deviceId, myS7PLC);
    }

    private void closeConnection(Long deviceId, MyS7PLC myS7PLC) {
        myS7PLC.lock.lock();
        try {
            myS7PLC.getPlc().close();
        } catch (Exception e) {
            log.warn("Driver connection close failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        } finally {
            myS7PLC.lock.unlock();
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "plcType", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "dbNum", issues);
        checkRequired(pointConfig, "byteOffset", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Getter
    @RequiredArgsConstructor
    private static class MyS7PLC {

        private final ReentrantLock lock;

        private final S7PLC plc;

    }

}