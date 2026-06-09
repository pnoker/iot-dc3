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

import com.github.xingshuangs.iot.protocol.melsec.enums.EMcSeries;
import com.github.xingshuangs.iot.protocol.melsec.service.McPLC;
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
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.driver.bean.MelsecPointVariable;
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
 * Melsec MC driver service backed by the iot-communication library.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MelsecDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, MyMcPLC> connectMap;

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
                MyMcPLC removed = connectMap.remove(metadataEvent.getId());
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
        MyMcPLC myMcPLC = getMcPLC(device.getId(), driverConfig);
        MelsecPointVariable variable = buildVariable(pointConfig, point.getPointTypeFlag().getCode());

        myMcPLC.lock.lock();
        try {
            Object value = readByType(myMcPLC.getPlc(), variable);
            return new ReadPointValue(device, point, String.valueOf(value));
        } catch (Exception e) {
            invalidateConnection(device.getId(), myMcPLC);
            log.error("Driver point read failed, protocol={}, deviceId={}, pointId={}", driverCode, device.getId(),
                    point.getId(), e);
            return null;
        } finally {
            myMcPLC.lock.unlock();
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
        MyMcPLC myMcPLC = getMcPLC(device.getId(), driverConfig);
        MelsecPointVariable variable = buildVariable(pointConfig, writePointValue.getType().getCode());

        myMcPLC.lock.lock();
        try {
            writeByType(myMcPLC.getPlc(), variable, writePointValue.getValue());
            return true;
        } catch (Exception e) {
            invalidateConnection(device.getId(), myMcPLC);
            log.error("Driver point write failed, protocol={}, deviceId={}, pointId={}", driverCode, device.getId(),
                    point.getId(), e);
            return false;
        } finally {
            myMcPLC.lock.unlock();
        }
    }

    private MyMcPLC getMcPLC(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return connectMap.computeIfAbsent(deviceId, id -> {
            String host = driverConfig.get("host").getValue(String.class);
            int port = driverConfig.get("port").getValue(Integer.class);
            String series = driverConfig.containsKey("series")
                    ? driverConfig.get("series").getValue(String.class)
                    : "QnA";

            EMcSeries eMcSeries;
            try {
                eMcSeries = EMcSeries.valueOf(series);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown series '{}', fallback to QnA", series);
                eMcSeries = EMcSeries.QnA;
            }

            log.debug("Driver connection creating, protocol={}, deviceId={}, host={}, port={}, series={}",
                    driverCode, deviceId, host, port, series);
            try {
                McPLC mcPLC = new McPLC(eMcSeries, host, port);
                log.info("Driver connection established, protocol={}, deviceId={}, host={}, port={}, series={}",
                        driverCode, deviceId, host, port, series);
                return new MyMcPLC(new ReentrantLock(), mcPLC);
            } catch (Exception e) {
                log.error("Driver connection failed, protocol={}, deviceId={}, host={}, port={}", driverCode, deviceId,
                        host, port, e);
                throw new ServiceException("Driver connection failed, protocol={}, deviceId={}, host={}, port={}",
                        driverCode, deviceId, host, port, e);
            }
        });
    }

    private MelsecPointVariable buildVariable(Map<String, AttributeBO> pointConfig, String type) {
        String address = pointConfig.get("address").getValue(String.class);
        int length = pointConfig.containsKey("length") ? pointConfig.get("length").getValue(Integer.class) : 0;
        return new MelsecPointVariable(address, type, length);
    }

    private Object readByType(McPLC plc, MelsecPointVariable variable) {
        AttributeTypeEnum type = AttributeTypeEnum.ofCode(variable.getType());
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("Unknown type: " + variable.getType());
        }
        switch (type) {
            case BOOLEAN:
                return plc.readBoolean(variable.getAddress());
            case BYTE:
                return plc.readByte(variable.getAddress());
            case SHORT:
                return plc.readInt16(variable.getAddress());
            case INT:
                return plc.readInt32(variable.getAddress());
            case LONG:
                return plc.readInt64(variable.getAddress());
            case FLOAT:
                return plc.readFloat32(variable.getAddress());
            case DOUBLE:
                return plc.readFloat64(variable.getAddress());
            case STRING:
                int length = variable.getLength() > 0 ? variable.getLength() : 64;
                return plc.readString(variable.getAddress(), length);
            default:
                throw new IllegalArgumentException("Unsupported read type: " + type);
        }
    }

    private void writeByType(McPLC plc, MelsecPointVariable variable, String value) {
        AttributeTypeEnum type = AttributeTypeEnum.ofCode(variable.getType());
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("Unknown type: " + variable.getType());
        }
        switch (type) {
            case BOOLEAN:
                plc.writeBoolean(variable.getAddress(), Boolean.parseBoolean(value));
                break;
            case BYTE:
                plc.writeByte(variable.getAddress(), Byte.parseByte(value));
                break;
            case SHORT:
                plc.writeInt16(variable.getAddress(), Short.parseShort(value));
                break;
            case INT:
                plc.writeInt32(variable.getAddress(), Integer.parseInt(value));
                break;
            case LONG:
                plc.writeInt64(variable.getAddress(), Long.parseLong(value));
                break;
            case FLOAT:
                plc.writeFloat32(variable.getAddress(), Float.parseFloat(value));
                break;
            case DOUBLE:
                plc.writeFloat64(variable.getAddress(), Double.parseDouble(value));
                break;
            case STRING:
                plc.writeString(variable.getAddress(), value);
                break;
            default:
                throw new IllegalArgumentException("Unsupported write type: " + type);
        }
    }

    private void invalidateConnection(Long deviceId, MyMcPLC myMcPLC) {
        connectMap.remove(deviceId, myMcPLC);
        closeConnection(deviceId, myMcPLC);
    }

    private void closeConnection(Long deviceId, MyMcPLC myMcPLC) {
        myMcPLC.lock.lock();
        try {
            myMcPLC.getPlc().close();
        } catch (Exception e) {
            log.warn("Driver connection close failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        } finally {
            myMcPLC.lock.unlock();
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "host", issues);
        checkRequired(driverConfig, "port", issues);
        checkRequired(driverConfig, "series", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "address", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Getter
    @RequiredArgsConstructor
    private static class MyMcPLC {

        private final ReentrantLock lock;

        private final McPLC plc;

    }

}