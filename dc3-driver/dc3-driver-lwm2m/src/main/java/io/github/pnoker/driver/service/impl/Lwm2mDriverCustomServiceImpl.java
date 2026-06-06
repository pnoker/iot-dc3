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
import io.github.pnoker.common.driver.entity.bean.DriverHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.driver.lwm2m.Lwm2mServerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LwM2M Driver Custom Service Implementation.
 * <p>
 * Implements the DriverCustomService interface for the LwM2M protocol.
 * Supports active read/write operations via the embedded Leshan server.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Lwm2mDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DeviceMetadata deviceMetadata;
    private final DriverSenderService driverSenderService;
    private final Lwm2mServerManager lwm2mServerManager;

    @Value("${dc3.driver.code}")
    private String driverCode;

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
        // Lwm2mServerManager auto-starts via @PostConstruct
        log.info("LwM2M driver initialized");
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public DriverHealthState health() {
        if (lwm2mServerManager.isServerStarted()) {
            return DriverHealthState.online();
        }
        return DriverHealthState.offline();
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        String endpoint = getConfigValue(driverConfig, "endpoint", "");
        if (endpoint.isEmpty()) {
            return DeviceHealthState.offline();
        }
        if (lwm2mServerManager.isDeviceRegistered(endpoint)) {
            return DeviceHealthState.online();
        }
        return DeviceHealthState.offline();
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
            if (MetadataOperateTypeEnum.DELETE.equals(operateType)) {
                cleanupDevice(metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        String endpoint = getConfigValue(driverConfig, "endpoint", "");
        if (endpoint.isEmpty()) {
            log.warn("LwM2M read failed: endpoint not configured, deviceId={}", device.getId());
            return null;
        }

        int objectId = getConfigIntValue(pointConfig, "objectId", 0);
        int objectInstanceId = getConfigIntValue(pointConfig, "objectInstanceId", 0);
        int resourceId = getConfigIntValue(pointConfig, "resourceId", 0);

        log.debug("LwM2M read: endpoint={}, path=/{}/{}/{}, deviceId={}, pointId={}",
                endpoint, objectId, objectInstanceId, resourceId, device.getId(), point.getId());

        String value = lwm2mServerManager.read(endpoint, objectId, objectInstanceId, resourceId);
        if (Objects.isNull(value)) {
            log.warn("LwM2M read returned empty value: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId);
            return null;
        }

        return new ReadPointValue(device, point, value);
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        String endpoint = getConfigValue(driverConfig, "endpoint", "");
        if (endpoint.isEmpty()) {
            log.warn("LwM2M write failed: endpoint not configured, deviceId={}", device.getId());
            return false;
        }

        int objectId = getConfigIntValue(pointConfig, "objectId", 0);
        int objectInstanceId = getConfigIntValue(pointConfig, "objectInstanceId", 0);
        int resourceId = getConfigIntValue(pointConfig, "resourceId", 0);
        String value = writePointValue.getValue(String.class);

        log.debug("LwM2M write: endpoint={}, path=/{}/{}/{}, deviceId={}, pointId={}",
                endpoint, objectId, objectInstanceId, resourceId, device.getId(), point.getId());

        return lwm2mServerManager.write(endpoint, objectId, objectInstanceId, resourceId, value);
    }

    /**
     * Clean up device resources when a device is deleted.
     *
     * @param deviceId the deleted device ID
     */
    private void cleanupDevice(Long deviceId) {
        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        if (Objects.nonNull(driverConfig)) {
            String endpoint = getConfigValue(driverConfig, "endpoint", "");
            if (!endpoint.isEmpty()) {
                log.info("Cleaning up LwM2M device: endpoint={}, deviceId={}", endpoint, deviceId);
            }
        }
    }

    private String getConfigValue(Map<String, AttributeBO> config, String key, String defaultValue) {
        AttributeBO attribute = config.get(key);
        if (Objects.isNull(attribute) || Objects.isNull(attribute.getValue()) || attribute.getValue().isEmpty()) {
            return defaultValue;
        }
        return attribute.getValue(String.class);
    }

    private int getConfigIntValue(Map<String, AttributeBO> config, String key, int defaultValue) {
        AttributeBO attribute = config.get(key);
        if (Objects.isNull(attribute) || Objects.isNull(attribute.getValue())) {
            return defaultValue;
        }
        try {
            return attribute.getValue(Integer.class);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "endpoint", issues);
        checkRequired(driverConfig, "serverHost", issues);
        checkRequired(driverConfig, "serverPort", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "objectId", issues);
        checkRequired(pointConfig, "objectInstanceId", issues);
        checkRequired(pointConfig, "resourceId", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

}