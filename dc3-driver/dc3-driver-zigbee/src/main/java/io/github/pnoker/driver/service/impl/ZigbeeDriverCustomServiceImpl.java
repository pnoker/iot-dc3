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

import com.zsmartsystems.zigbee.CommandResult;
import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.ZigBeeEndpoint;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.ZigBeeStatus;
import com.zsmartsystems.zigbee.zcl.ZclAttribute;
import com.zsmartsystems.zigbee.zcl.ZclCluster;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.DriverHealthState;
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
import io.github.pnoker.common.exception.ReadPointException;
import io.github.pnoker.common.exception.WritePointException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Custom driver service implementation for the Zigbee driver.
 * <p>
 * Manages Zigbee network connections via serial coordinator dongles, reads point
 * values from Zigbee devices via ZCL attributes, and writes values to ZCL attributes.
 * </p>
 *
 *
 * <p>
 * <b>WARNING:</b> This driver is a work-in-progress skeleton. Protocol-level
 * I/O is not yet fully implemented — see TODO markers in method bodies.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
public class ZigbeeDriverCustomServiceImpl implements DriverCustomService {

    private static final long ATTRIBUTE_TIMEOUT_SECONDS = 5;

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    private final ZigbeeNetworkManagerFactory networkManagerFactory;

    @Value("${dc3.driver.code}")
    private String driverCode;

    private ZigBeeNetworkManager networkManager;

    /**
     * Constructs a new Zigbee driver custom service.
     *
     * @param driverMetadata      driver metadata context
     * @param driverSenderService service for sending data to the DC3 platform
     */
    public ZigbeeDriverCustomServiceImpl(
            DriverMetadata driverMetadata,
            DriverSenderService driverSenderService,
            ZigbeeNetworkManagerFactory networkManagerFactory) {
        this.driverMetadata = driverMetadata;
        this.driverSenderService = driverSenderService;
        this.networkManagerFactory = networkManagerFactory;
    }

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
        // TODO: Read serial port and baud rate from driver configuration
        String serialPort = "/dev/ttyUSB0";
        int baudRate = 115200;

        networkManager = networkManagerFactory.create(serialPort, baudRate);

        // Add network state listener
        networkManager.addNetworkStateListener(state -> {
            log.info("Driver Zigbee network state changed, protocol={}, state={}", driverCode, state);
        });

        ZigBeeStatus initStatus = networkManager.initialize();
        log.info("Driver Zigbee network initialized, protocol={}, status={}", driverCode, initStatus);

        ZigBeeStatus startupStatus = networkManager.startup(true);
        log.info("Driver Zigbee network startup, protocol={}, status={}", driverCode, startupStatus);

        log.info("Driver initialized, protocol={}, serialPort={}, baudRate={}", driverCode, serialPort, baudRate);
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    @Override
    public DriverHealthState health() {
        if (Objects.isNull(networkManager)) {
            return DriverHealthState.offline();
        }
        return DriverHealthState.online();
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            // TODO: Verify node lookup API using driverConfig for IEEE address
            return DeviceHealthState.online();
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

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)) {
                // TODO: Cleanup node resources when device is deleted
                log.info("Driver device deleted, protocol={}, deviceId={}", driverCode, metadataEvent.getId());
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
        try {
            String nodeIeeeAddress = pointConfig.get("nodeIeeeAddress").getValue(String.class);
            int endpointId = pointConfig.get("endpointId").getValue(Integer.class);
            int clusterId = pointConfig.get("clusterId").getValue(Integer.class);
            int attributeId = pointConfig.get("attributeId").getValue(Integer.class);

            String value = readAttribute(nodeIeeeAddress, endpointId, clusterId, attributeId);
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            throw e;
        } catch (Exception e) {
            log.error("Driver point read failed, protocol={}", driverCode, e);
            throw new ReadPointException(
                    "Driver point read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        try {
            String nodeIeeeAddress = pointConfig.get("nodeIeeeAddress").getValue(String.class);
            int endpointId = pointConfig.get("endpointId").getValue(Integer.class);
            int clusterId = pointConfig.get("clusterId").getValue(Integer.class);
            int attributeId = pointConfig.get("attributeId").getValue(Integer.class);

            writeAttribute(nodeIeeeAddress, endpointId, clusterId, attributeId, writePointValue.getValue(String.class));
            return true;
        } catch (WritePointException e) {
            throw e;
        } catch (Exception e) {
            log.error("Driver point write failed, protocol={}", driverCode, e);
            throw new WritePointException(
                    "Driver point write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    /**
     * Read a ZCL attribute value from a Zigbee device.
     *
     * @param nodeIeeeAddress IEEE address of the Zigbee node
     * @param endpointId      endpoint ID on the node
     * @param clusterId       ZCL cluster ID
     * @param attributeId     ZCL attribute ID within the cluster
     * @return the read value as a string
     */
    private String readAttribute(String nodeIeeeAddress, int endpointId, int clusterId, int attributeId) {
        if (Objects.isNull(networkManager)) {
            throw new ReadPointException("Driver Zigbee network not initialized, protocol={}", driverCode);
        }

        ZigBeeNode node = networkManager.getNode(new IeeeAddress(nodeIeeeAddress));
        if (Objects.isNull(node)) {
            throw new ReadPointException(
                    "Driver Zigbee node not found, protocol={}, nodeIeeeAddress={}", driverCode, nodeIeeeAddress);
        }

        ZigBeeEndpoint endpoint = node.getEndpoint(endpointId);
        if (Objects.isNull(endpoint)) {
            throw new ReadPointException(
                    "Driver Zigbee endpoint not found, protocol={}, endpointId={}", driverCode, endpointId);
        }

        // TODO: Verify ZclCluster lookup by cluster ID API
        ZclCluster cluster = endpoint.getInputCluster(clusterId);
        if (Objects.isNull(cluster)) {
            throw new ReadPointException(
                    "Driver Zigbee cluster not found, protocol={}, clusterId={}", driverCode, clusterId);
        }

        ZclAttribute attribute = cluster.getAttribute(attributeId);
        if (Objects.isNull(attribute)) {
            throw new ReadPointException(
                    "Driver Zigbee attribute not found, protocol={}, attributeId={}", driverCode, attributeId);
        }

        Object value = attribute.readValue(TimeUnit.SECONDS.toMillis(ATTRIBUTE_TIMEOUT_SECONDS));
        return Objects.nonNull(value) ? String.valueOf(value) : "0";
    }

    /**
     * Write a value to a ZCL attribute on a Zigbee device.
     *
     * @param nodeIeeeAddress IEEE address of the Zigbee node
     * @param endpointId      endpoint ID on the node
     * @param clusterId       ZCL cluster ID
     * @param attributeId     ZCL attribute ID within the cluster
     * @param value           value to write as a string
     */
    private void writeAttribute(String nodeIeeeAddress, int endpointId, int clusterId, int attributeId, String value) {
        if (Objects.isNull(networkManager)) {
            throw new WritePointException("Driver Zigbee network not initialized, protocol={}", driverCode);
        }

        ZigBeeNode node = networkManager.getNode(new IeeeAddress(nodeIeeeAddress));
        if (Objects.isNull(node)) {
            throw new WritePointException(
                    "Driver Zigbee node not found, protocol={}, nodeIeeeAddress={}", driverCode, nodeIeeeAddress);
        }

        ZigBeeEndpoint endpoint = node.getEndpoint(endpointId);
        if (Objects.isNull(endpoint)) {
            throw new WritePointException(
                    "Driver Zigbee endpoint not found, protocol={}, endpointId={}", driverCode, endpointId);
        }

        ZclCluster cluster = endpoint.getInputCluster(clusterId);
        if (Objects.isNull(cluster)) {
            throw new WritePointException(
                    "Driver Zigbee cluster not found, protocol={}, clusterId={}", driverCode, clusterId);
        }

        ZclAttribute attribute = cluster.getAttribute(attributeId);
        if (Objects.isNull(attribute)) {
            throw new WritePointException(
                    "Driver Zigbee attribute not found, protocol={}, attributeId={}", driverCode, attributeId);
        }

        try {
            CommandResult result = attribute.writeValue(value).get(ATTRIBUTE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (Objects.isNull(result) || !result.isSuccess()) {
                throw new WritePointException(
                        "Driver Zigbee attribute write rejected, protocol={}, nodeIeeeAddress={}, attributeId={}",
                        driverCode,
                        nodeIeeeAddress,
                        attributeId);
            }
            log.info(
                    "Driver Zigbee write completed, protocol={}, nodeIeeeAddress={}, clusterId={}, attributeId={}",
                    driverCode,
                    nodeIeeeAddress,
                    clusterId,
                    attributeId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WritePointException(
                    "Driver Zigbee attribute write interrupted, protocol={}, attributeId={}",
                    driverCode,
                    attributeId,
                    e);
        } catch (ExecutionException | TimeoutException e) {
            throw new WritePointException(
                    "Driver Zigbee attribute write failed, protocol={}, attributeId={}", driverCode, attributeId, e);
        }
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "serialPort", issues);
        checkRequired(driverConfig, "baudRate", issues);
        checkRequired(driverConfig, "dongleType", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "nodeIeeeAddress", issues);
        checkRequired(pointConfig, "endpointId", issues);
        checkRequired(pointConfig, "clusterId", issues);
        checkRequired(pointConfig, "attributeId", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues)
                .build();
    }
}
