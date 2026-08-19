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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom driver service implementation for the KNX smart building driver.
 * <p>
 * Maintains one Calimero tunneling link per gateway device and reads/writes group
 * addresses as boolean, unsigned, float, or control values via
 * {@link ProcessCommunicator}.
 * </p>
 *
 * @author pnoker
 * @since 2026.5.22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnxDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;
    @Value("${dc3.driver.code}")
    private String driverCode;

    private Map<Long, KnxLink> linkMap;

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
        linkMap = new ConcurrentHashMap<>(16);
    }

    @Override
    public void schedule() {
        // No custom scheduled task; group addresses are polled on the SDK read schedule.
    }

    @Override
    public DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        if (Objects.isNull(device) || Objects.isNull(device.getId())) {
            return DeviceHealthState.offline();
        }
        try {
            KnxLink link = getLink(device.getId(), driverConfig);
            return link.isOpen() ? DeviceHealthState.online() : DeviceHealthState.offline();
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
            KnxLink removed = linkMap.remove(metadataEvent.getId());
            if (Objects.nonNull(removed)) {
                removed.close();
                log.info("Driver connection destroyed, protocol={}, deviceId={}, operateType={}",
                        driverCode, metadataEvent.getId(), operateType);
            }
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        KnxLink link = getLink(device.getId(), driverConfig);
        try {
            GroupAddress groupAddress = parseGroupAddress(getRequiredConfig(pointConfig, "groupAddress"));
            String dataType = getConfigValue(pointConfig, "dataType", "BOOL");
            String dpt = getConfigValue(pointConfig, "dpt", "");
            String value = switch (dataType.toUpperCase()) {
                case "UINT" -> String.valueOf(link.communicator().readUnsigned(groupAddress, dpt));
                case "FLOAT" -> String.valueOf(link.communicator().readFloat(groupAddress));
                case "CONTROL" -> String.valueOf(link.communicator().readControl(groupAddress));
                default -> String.valueOf(link.communicator().readBool(groupAddress));
            };
            return new ReadPointValue(device, point, value);
        } catch (ReadPointException e) {
            invalidateLink(device.getId(), link);
            throw e;
        } catch (Exception e) {
            invalidateLink(device.getId(), link);
            throw new ReadPointException("KNX read failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        KnxLink link = getLink(device.getId(), driverConfig);
        try {
            GroupAddress groupAddress = parseGroupAddress(getRequiredConfig(pointConfig, "groupAddress"));
            String dataType = getConfigValue(pointConfig, "dataType", "BOOL");
            String rawValue = writePointValue.getValue(String.class);
            switch (dataType.toUpperCase()) {
                case "UINT" -> {
                    String dpt = getRequiredConfig(pointConfig, "dpt");
                    link.communicator().write(groupAddress, Integer.parseInt(rawValue), dpt);
                }
                case "FLOAT" -> link.communicator().write(groupAddress, Double.parseDouble(rawValue), true);
                case "CONTROL" -> link.communicator().write(groupAddress, Boolean.parseBoolean(rawValue), 0);
                default -> link.communicator().write(groupAddress, Boolean.parseBoolean(rawValue));
            }
            return true;
        } catch (Exception e) {
            invalidateLink(device.getId(), link);
            throw new WritePointException("KNX write failed, protocol={}, message={}", driverCode, e.getMessage(), e);
        }
    }

    private KnxLink getLink(Long deviceId, Map<String, AttributeBO> driverConfig) {
        return linkMap.computeIfAbsent(deviceId, id -> {
            String remoteHost = getConfigValue(driverConfig, "remoteHost", "192.168.0.100");
            int remotePort = getConfigIntValue(driverConfig, "remotePort", 3671);
            String localHost = getConfigValue(driverConfig, "localHost", "");
            boolean useNat = getConfigBoolValue(driverConfig, "useNat", false);
            String deviceAddress = getConfigValue(driverConfig, "deviceAddress", "0.0.0");
            try {
                InetSocketAddress local = localHost.isEmpty()
                        ? new InetSocketAddress(0)
                        : new InetSocketAddress(InetAddress.getByName(localHost), 0);
                InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName(remoteHost), remotePort);
                KNXMediumSettings settings = KNXMediumSettings.create(KNXMediumSettings.MEDIUM_TP1,
                        new IndividualAddress(deviceAddress));
                KNXNetworkLink link = KNXNetworkLinkIP.newTunnelingLink(local, remote, useNat, settings);
                ProcessCommunicator communicator = new ProcessCommunicatorImpl(link);
                log.info("Driver connection established, protocol={}, deviceId={}, remote={}:{}",
                        driverCode, deviceId, remoteHost, remotePort);
                return new KnxLink(link, communicator);
            } catch (Exception e) {
                throw new ConnectorException("Failed to open KNX link: {}:{}", remoteHost, remotePort, e);
            }
        });
    }

    private GroupAddress parseGroupAddress(String address) {
        try {
            return GroupAddress.from(address.trim());
        } catch (Exception e) {
            throw new ConnectorException("Invalid KNX group address: {}", address, e);
        }
    }

    private void invalidateLink(Long deviceId, KnxLink link) {
        linkMap.remove(deviceId, link);
        try {
            if (Objects.nonNull(link)) {
                link.close();
            }
        } catch (Exception e) {
            log.warn("Driver connection destroy failed, protocol={}, deviceId={}", driverCode, deviceId, e);
        }
    }

    private String getRequiredConfig(Map<String, AttributeBO> config, String code) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
            throw new ConnectorException("Required attribute '{}' is missing", code);
        }
        return attr.getValue(String.class);
    }

    private String getConfigValue(Map<String, AttributeBO> config, String code, String defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue()) || attr.getValue().isEmpty()) {
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

    private boolean getConfigBoolValue(Map<String, AttributeBO> config, String code, boolean defaultValue) {
        AttributeBO attr = config.get(code);
        if (Objects.isNull(attr) || Objects.isNull(attr.getValue())) {
            return defaultValue;
        }
        return attr.getValue(Boolean.class);
    }

    @Override
    public ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(driverConfig, "remoteHost", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    @Override
    public ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        List<ValidationReport.AttributeIssue> issues = new ArrayList<>();
        checkRequired(pointConfig, "groupAddress", issues);
        return ValidationReport.builder()
                .passed(issues.stream().noneMatch(i -> i.getLevel() == ValidationReport.IssueLevel.ERROR))
                .issues(issues).build();
    }

    /**
     * Holder pairing a KNX network link with its process communicator.
     */
    private static final class KnxLink {

        private final KNXNetworkLink networkLink;
        private final ProcessCommunicator communicator;

        KnxLink(KNXNetworkLink networkLink, ProcessCommunicator communicator) {
            this.networkLink = networkLink;
            this.communicator = communicator;
        }

        ProcessCommunicator communicator() {
            return communicator;
        }

        boolean isOpen() {
            return Objects.nonNull(networkLink) && networkLink.isOpen();
        }

        void close() {
            if (Objects.nonNull(networkLink)) {
                networkLink.close();
            }
        }
    }
}
