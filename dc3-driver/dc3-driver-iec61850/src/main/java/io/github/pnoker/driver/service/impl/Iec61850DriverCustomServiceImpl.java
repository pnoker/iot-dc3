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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openmuc.openiec61850.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Custom driver service implementation for the IEC 61850 MMS client driver.
 * <p>
 * Maintains one MMS association per IED device and reads/writes data attributes addressed
 * by an object reference and functional constraint (e.g. {@code S1MMXU1.TotW.actVal} / {@code MX}).
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Iec61850DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, Iec61850Association> associationMap;

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
        associationMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // No custom scheduled task; data attributes are polled on the SDK read schedule.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            Iec61850Association association = getAssociation(device.getId(), driverConfig);
            return association.client().isOpen() ? DeviceHealthState.online() : DeviceHealthState.offline();
        } catch (Exception e) {
            log.warn("Driver health check failed, protocol={}, deviceId={}", driverCode, device.getId(), e);
            return DeviceHealthState.offline();
        }
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)
                && (MetadataOperateTypeEnum.DELETE.equals(operateType)
                        || MetadataOperateTypeEnum.UPDATE.equals(operateType))) {
            Iec61850Association removed = associationMap.remove(metadataEvent.getId());
            if (Objects.nonNull(removed)) {
                removed.client().close();
                log.info(
                        "Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                        driverCode,
                        metadataEvent.getId(),
                        operateType);
            }
        }
    }

    @Override
    public ReadPointValue read(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point) {
        Iec61850Association association = getAssociation(device.getId(), driverConfig);
        try {
            ModelNode node = resolveNode(association, pointConfig);
            association.client().getDataValues((FcModelNode) node);
            String value = node.getBasicDataAttributes().isEmpty()
                    ? node.toString()
                    : node.getBasicDataAttributes().get(0).getValueString();
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateAssociation(device.getId(), association);
            throw e;
        } catch (Exception e) {
            invalidateAssociation(device.getId(), association);
            throw new ReadPointException(
                    "IEC 61850 read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        Iec61850Association association = getAssociation(device.getId(), driverConfig);
        try {
            ModelNode node = resolveNode(association, pointConfig);
            association.client().getDataValues((FcModelNode) node);
            if (node.getBasicDataAttributes().isEmpty()) {
                throw new WritePointException("IEC 61850 node has no writable data attribute, protocol={}", driverCode);
            }
            BasicDataAttribute bda = node.getBasicDataAttributes().get(0);
            String rawValue = writePointValue.getValue(String.class);
            setValue(bda, rawValue);
            association.client().setDataValues((FcModelNode) node);
            return true;
        } catch (WritePointException e) {
            invalidateAssociation(device.getId(), association);
            throw e;
        } catch (Exception e) {
            invalidateAssociation(device.getId(), association);
            throw new WritePointException(
                    "IEC 61850 write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private ModelNode resolveNode(Iec61850Association association, Map<String, AttributeBO> pointConfig)
            throws Exception {
        String objectReference = getRequiredConfig(pointConfig, "objectReference");
        String fcName = getConfigValue(pointConfig, "functionalConstraint", "MX");
        ServerModel serverModel = association.serverModel();
        ModelNode node = serverModel.findModelNode(objectReference, Fc.valueOf(fcName.toUpperCase()));
        if (Objects.isNull(node)) {
            throw new ConnectorException("IEC 61850 object reference not found: {}", objectReference);
        }
        return node;
    }

    private void setValue(BasicDataAttribute bda, String rawValue) {
        if (bda instanceof BdaBoolean) {
            ((BdaBoolean) bda).setValue(Boolean.parseBoolean(rawValue));
        } else if (bda instanceof BdaInt8) {
            ((BdaInt8) bda).setValue(Byte.parseByte(rawValue));
        } else if (bda instanceof BdaInt16) {
            ((BdaInt16) bda).setValue(Short.parseShort(rawValue));
        } else if (bda instanceof BdaInt32) {
            ((BdaInt32) bda).setValue(Integer.parseInt(rawValue));
        } else if (bda instanceof BdaInt64) {
            ((BdaInt64) bda).setValue(Long.parseLong(rawValue));
        } else if (bda instanceof BdaFloat32) {
            ((BdaFloat32) bda).setFloat(Float.parseFloat(rawValue));
        } else if (bda instanceof BdaFloat64) {
            ((BdaFloat64) bda).setDouble(Double.parseDouble(rawValue));
        } else if (bda instanceof BdaVisibleString) {
            ((BdaVisibleString) bda).setValue(rawValue);
        } else {
            throw new ConnectorException("Unsupported IEC 61850 data type: {}", bda.getBasicType());
        }
    }

    private Iec61850Association getAssociation(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return associationMap.computeIfAbsent(deviceId, id -> {
            String host = getRequiredConfig(driverConfig, "host");
            int port = getConfigIntValue(driverConfig, "port", 102);
            try {
                ClientSap sap = new ClientSap();
                ClientAssociation client = sap.associate(InetAddress.getByName(host), port, null, null);
                ServerModel serverModel = client.retrieveModel();
                log.info(
                        "Driver connection established, protocol={}, deviceId={}, host={}:{}",
                        driverCode,
                        deviceId,
                        host,
                        port);
                return new Iec61850Association(client, serverModel);
            } catch (Exception e) {
                throw new ConnectorException("Failed to associate IEC 61850 server: {}:{}", host, port, e);
            }
        });
    }

    private void invalidateAssociation(Long deviceId, Iec61850Association association) {
        associationMap.remove(deviceId, association);
        try {
            if (Objects.nonNull(association)) {
                association.client().close();
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
        checkRequired(driverConfig, "host", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "objectReference", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    /**
     * Holder pairing an MMS client association with its cached server model.
     */
    private static final class Iec61850Association {

        private final ClientAssociation client;
        private final ServerModel serverModel;

        Iec61850Association(ClientAssociation client, ServerModel serverModel) {
            this.client = client;
            this.serverModel = serverModel;
        }

        ClientAssociation client() {
            return client;
        }

        ServerModel serverModel() {
            return serverModel;
        }
    }
}
