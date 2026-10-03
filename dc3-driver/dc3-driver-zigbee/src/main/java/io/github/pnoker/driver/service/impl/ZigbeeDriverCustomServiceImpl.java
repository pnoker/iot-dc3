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
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
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
import org.springframework.stereotype.Service;

/**
 * Custom driver service implementation for the Zigbee driver.
 * <p>
 * Manages the Zigbee network connection via a serial coordinator dongle, reads point
 * values from Zigbee devices via ZCL attributes, and writes values to ZCL attributes.
 * The coordinator is a driver-global resource, so the network manager is created
 * lazily on first use from the device driver configuration (serial port, baud rate,
 * dongle type) rather than from hard-coded values.
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
    private final DeviceMetadata deviceMetadata;
    private final DriverSenderService driverSenderService;
    private final ZigbeeNetworkManagerFactory networkManagerFactory;
    private final DriverProperties driverProperties;

    private ZigBeeNetworkManager networkManager;

    /**
     * Constructs a new Zigbee driver custom service.
     *
     * @param driverMetadata      driver metadata context
     * @param deviceMetadata      device metadata used for per-device health lookups
     * @param driverSenderService service for sending data to the DC3 platform
     * @param networkManagerFactory serial network manager factory
     * @param driverProperties    typed driver properties
     */
    public ZigbeeDriverCustomServiceImpl(
            DriverMetadata driverMetadata,
            DeviceMetadata deviceMetadata,
            DriverSenderService driverSenderService,
            ZigbeeNetworkManagerFactory networkManagerFactory,
            DriverProperties driverProperties) {
        this.driverProperties = driverProperties;
        this.driverMetadata = driverMetadata;
        this.deviceMetadata = deviceMetadata;
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
        // The coordinator binds one serial dongle shared by every device. The network
        // manager is created lazily on first use so the serial settings come from the
        // device driver configuration instead of being hard-coded here.
        log.info("Driver initialized, protocol={} (network starts lazily on first use)", driverProperties.getCode());
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
        if (Objects.isNull(networkManager)) {
            return DeviceHealthState.offline();
        }
        IeeeAddress nodeAddress = resolveNodeAddress(device.getId());
        if (Objects.isNull(nodeAddress)) {
            return DeviceHealthState.offline();
        }
        return Objects.nonNull(networkManager.getNode(nodeAddress))
                ? DeviceHealthState.online()
                : DeviceHealthState.offline();
    }

    /**
     * Resolve the IEEE address of any point configured on the device; the Zigbee
     * model attaches node addressing to points, so a device is reachable when at
     * least one of its points resolves to a known node.
     */
    private IeeeAddress resolveNodeAddress(Long deviceId) {
        Map<Long, Map<String, AttributeBO>> pointConfig = deviceMetadata.getPointConfig(deviceId);
        if (Objects.isNull(pointConfig)) {
            return null;
        }
        for (Map<String, AttributeBO> attributes : pointConfig.values()) {
            AttributeBO attribute = attributes.get("nodeIeeeAddress");
            if (Objects.isNull(attribute)
                    || Objects.isNull(attribute.getValue())
                    || attribute.getValue().isEmpty()) {
                continue;
            }
            try {
                return new IeeeAddress(attribute.getValue(String.class));
            } catch (IllegalArgumentException e) {
                log.warn(
                        "Driver Zigbee point carries a malformed node address, protocol={}, deviceId={}",
                        driverProperties.getCode(),
                        deviceId);
            }
        }
        return null;
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverProperties.getCode(),
                    metadataType,
                    operateType,
                    metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)) {
                // Coordinator-side discovery owns node lifecycle: the driver retains no
                // per-device Zigbee resources after deletion, so there is nothing to
                // tear down here beyond the point configuration held by the platform.
                log.info(
                        "Driver device deleted, protocol={}, deviceId={}",
                        driverProperties.getCode(),
                        metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info(
                    "Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverProperties.getCode(),
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
        ensureNetwork(driverConfig);
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
            throw new ReadPointException(
                    "Driver point read failed, protocol={}, message={}", driverProperties.getCode(), e.getMessage(), e);
        }
    }

    /**
     * Write the point value to the ZCL attribute addressed by the point configuration;
     * the coordinator network is created on first use from the driver configuration.
     */
    @Override
    public Boolean write(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> pointConfig,
            DeviceBO device,
            PointBO point,
            WritePointValue writePointValue) {
        ensureNetwork(driverConfig);
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
            throw new WritePointException(
                    "Driver point write failed, protocol={}, message={}",
                    driverProperties.getCode(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Create and start the Zigbee network manager on first use, from the driver
     * configuration carried by the device. The coordinator is driver-global, so the
     * configuration of the first device that touches the network wins; later devices
     * reuse the established network.
     */
    private synchronized void ensureNetwork(Map<String, AttributeBO> driverConfig) {
        if (Objects.nonNull(networkManager)) {
            return;
        }
        String serialPort = requiredText(driverConfig, "serialPort");
        int baudRate = requiredInt(driverConfig, "baudRate");
        String dongleType = requiredText(driverConfig, "dongleType");

        networkManager = networkManagerFactory.create(dongleType, serialPort, baudRate);
        networkManager.addNetworkStateListener(state -> log.info(
                "Driver Zigbee network state changed, protocol={}, state={}", driverProperties.getCode(), state));

        ZigBeeStatus initStatus = networkManager.initialize();
        log.info("Driver Zigbee network initialized, protocol={}, status={}", driverProperties.getCode(), initStatus);

        ZigBeeStatus startupStatus = networkManager.startup(true);
        log.info("Driver Zigbee network startup, protocol={}, status={}", driverProperties.getCode(), startupStatus);
        log.info(
                "Driver Zigbee network started, protocol={}, serialPort={}, baudRate={}, dongleType={}",
                driverProperties.getCode(),
                serialPort,
                baudRate,
                dongleType);
    }

    private String requiredText(Map<String, AttributeBO> config, String code) {
        AttributeBO attribute = config.get(code);
        if (Objects.isNull(attribute)
                || Objects.isNull(attribute.getValue())
                || attribute.getValue().isEmpty()) {
            throw new ConnectorException("Driver Zigbee attribute '{}' is required to start the network", code);
        }
        return attribute.getValue(String.class);
    }

    private int requiredInt(Map<String, AttributeBO> config, String code) {
        AttributeBO attribute = config.get(code);
        if (Objects.isNull(attribute)
                || Objects.isNull(attribute.getValue())
                || attribute.getValue().isEmpty()) {
            throw new ConnectorException("Driver Zigbee attribute '{}' is required to start the network", code);
        }
        try {
            return attribute.getValue(Integer.class);
        } catch (RuntimeException e) {
            throw new ConnectorException("Driver Zigbee attribute '{}' must be an integer", code);
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
            throw new ReadPointException(
                    "Driver Zigbee network not initialized, protocol={}", driverProperties.getCode());
        }

        ZigBeeNode node = networkManager.getNode(new IeeeAddress(nodeIeeeAddress));
        if (Objects.isNull(node)) {
            throw new ReadPointException(
                    "Driver Zigbee node not found, protocol={}, nodeIeeeAddress={}",
                    driverProperties.getCode(),
                    nodeIeeeAddress);
        }

        ZigBeeEndpoint endpoint = node.getEndpoint(endpointId);
        if (Objects.isNull(endpoint)) {
            throw new ReadPointException(
                    "Driver Zigbee endpoint not found, protocol={}, endpointId={}",
                    driverProperties.getCode(),
                    endpointId);
        }

        // getInputCluster returns null for unknown cluster ids (zsmartsystems 1.4.16)
        ZclCluster cluster = endpoint.getInputCluster(clusterId);
        if (Objects.isNull(cluster)) {
            throw new ReadPointException(
                    "Driver Zigbee cluster not found, protocol={}, clusterId={}",
                    driverProperties.getCode(),
                    clusterId);
        }

        ZclAttribute attribute = cluster.getAttribute(attributeId);
        if (Objects.isNull(attribute)) {
            throw new ReadPointException(
                    "Driver Zigbee attribute not found, protocol={}, attributeId={}",
                    driverProperties.getCode(),
                    attributeId);
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
            throw new WritePointException(
                    "Driver Zigbee network not initialized, protocol={}", driverProperties.getCode());
        }

        ZigBeeNode node = networkManager.getNode(new IeeeAddress(nodeIeeeAddress));
        if (Objects.isNull(node)) {
            throw new WritePointException(
                    "Driver Zigbee node not found, protocol={}, nodeIeeeAddress={}",
                    driverProperties.getCode(),
                    nodeIeeeAddress);
        }

        ZigBeeEndpoint endpoint = node.getEndpoint(endpointId);
        if (Objects.isNull(endpoint)) {
            throw new WritePointException(
                    "Driver Zigbee endpoint not found, protocol={}, endpointId={}",
                    driverProperties.getCode(),
                    endpointId);
        }

        ZclCluster cluster = endpoint.getInputCluster(clusterId);
        if (Objects.isNull(cluster)) {
            throw new WritePointException(
                    "Driver Zigbee cluster not found, protocol={}, clusterId={}",
                    driverProperties.getCode(),
                    clusterId);
        }

        ZclAttribute attribute = cluster.getAttribute(attributeId);
        if (Objects.isNull(attribute)) {
            throw new WritePointException(
                    "Driver Zigbee attribute not found, protocol={}, attributeId={}",
                    driverProperties.getCode(),
                    attributeId);
        }

        try {
            CommandResult result = attribute.writeValue(value).get(ATTRIBUTE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (Objects.isNull(result) || !result.isSuccess()) {
                throw new WritePointException(
                        "Driver Zigbee attribute write rejected, protocol={}, nodeIeeeAddress={}, attributeId={}",
                        driverProperties.getCode(),
                        nodeIeeeAddress,
                        attributeId);
            }
            log.info(
                    "Driver Zigbee write completed, protocol={}, nodeIeeeAddress={}, clusterId={}, attributeId={}",
                    driverProperties.getCode(),
                    nodeIeeeAddress,
                    clusterId,
                    attributeId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WritePointException(
                    "Driver Zigbee attribute write interrupted, protocol={}, attributeId={}",
                    driverProperties.getCode(),
                    attributeId,
                    e);
        } catch (ExecutionException | TimeoutException e) {
            throw new WritePointException(
                    "Driver Zigbee attribute write failed, protocol={}, attributeId={}",
                    driverProperties.getCode(),
                    attributeId,
                    e);
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
