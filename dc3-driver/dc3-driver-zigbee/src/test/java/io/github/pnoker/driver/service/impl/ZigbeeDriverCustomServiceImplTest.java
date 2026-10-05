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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zsmartsystems.zigbee.CommandResult;
import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.ZigBeeEndpoint;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.ZigBeeStatus;
import com.zsmartsystems.zigbee.zcl.ZclAttribute;
import com.zsmartsystems.zigbee.zcl.ZclCluster;
import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.ConnectorException;
import io.github.pnoker.common.exception.WritePointException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZigbeeDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private ZigbeeNetworkManagerFactory networkManagerFactory;

    @Mock
    private ZigBeeNetworkManager networkManager;

    @Mock
    private ZigBeeNode node;

    @Mock
    private ZigBeeEndpoint endpoint;

    @Mock
    private ZclCluster cluster;

    @Mock
    private ZclAttribute attribute;

    @Mock
    private CommandResult commandResult;

    private ZigbeeDriverCustomServiceImpl service;

    private static AttributeBO attribute(String value, AttributeTypeEnum type) {
        return AttributeBO.builder().value(value).type(type).build();
    }

    private static Map<String, AttributeBO> driverConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("serialPort", attribute("/dev/ttyUSB0", AttributeTypeEnum.STRING));
        config.put("baudRate", attribute("115200", AttributeTypeEnum.INT));
        config.put("dongleType", attribute("TELEGESIS", AttributeTypeEnum.STRING));
        return config;
    }

    private static Map<String, AttributeBO> pointConfig() {
        Map<String, AttributeBO> config = new HashMap<>();
        config.put("nodeIeeeAddress", attribute("00158D0001234567", AttributeTypeEnum.STRING));
        config.put("endpointId", attribute("1", AttributeTypeEnum.INT));
        config.put("clusterId", attribute("1026", AttributeTypeEnum.INT));
        config.put("attributeId", attribute("0", AttributeTypeEnum.INT));
        return config;
    }

    private static DeviceBO device() {
        DeviceBO device = new DeviceBO();
        device.setId(7L);
        return device;
    }

    private static PointBO point() {
        PointBO point = new PointBO();
        point.setId(9L);
        point.setPointTypeFlag(PointTypeEnum.STRING);
        return point;
    }

    @BeforeEach
    void setUp() {
        DriverProperties driverProperties = new DriverProperties();
        driverProperties.setCode("ZigbeeDriver");
        service = new ZigbeeDriverCustomServiceImpl(
                driverMetadata, deviceMetadata, driverSenderService, networkManagerFactory, driverProperties);
    }

    @Test
    void reportsOfflineUntilNetworkManagerIsCreated() {
        assertThat(service.health().getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void initialDefersNetworkCreationToFirstUse() {
        service.initial();

        assertThat(service.health().getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
        verifyNoInteractions(networkManagerFactory);
    }

    @Test
    void readCreatesNetworkFromDriverConfigurationOnFirstUse() {
        stubNetworkStartup();
        stubAttributePath();
        when(attribute.readValue(5000L)).thenReturn(23.75);

        ReadPointValue value = service.read(driverConfig(), pointConfig(), device(), point());

        assertThat(value.getValue()).isEqualTo("23.75");
        verify(networkManagerFactory).create("TELEGESIS", "/dev/ttyUSB0", 115200);
        verify(networkManager).initialize();
        verify(networkManager).startup(true);
        verify(attribute).readValue(5000L);
    }

    @Test
    void readRejectsMissingSerialPortBeforeTouchingTheNetwork() {
        Map<String, AttributeBO> incomplete = driverConfig();
        incomplete.remove("serialPort");

        assertThatThrownBy(() -> service.read(incomplete, pointConfig(), device(), point()))
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("serialPort");
        verifyNoInteractions(networkManagerFactory);
    }

    @Test
    void readRejectsDongleTypesWithoutAnAdapter() {
        when(networkManagerFactory.create("EMBER", "/dev/ttyUSB0", 115200))
                .thenThrow(new ConnectorException("Driver Zigbee dongle type 'EMBER' has no adapter"));

        assertThatThrownBy(() -> service.read(driverConfig("EMBER"), pointConfig(), device(), point()))
                .isInstanceOf(ConnectorException.class)
                .hasMessageContaining("EMBER");
    }

    @Test
    void validatesProtocolConfigurationInsteadOfTestFixtures() {
        ValidationReport validDriver = service.validate(driverConfig());
        ValidationReport validPoint = service.validatePoint(pointConfig(), point());
        ValidationReport missingDriver = service.validate(Map.of());
        ValidationReport missingPoint = service.validatePoint(Map.of(), point());

        assertThat(validDriver.isPassed()).isTrue();
        assertThat(validPoint.isPassed()).isTrue();
        assertThat(missingDriver.getIssues())
                .extracting(ValidationReport.AttributeIssue::getAttributeCode)
                .containsExactlyInAnyOrder("serialPort", "baudRate", "dongleType");
        assertThat(missingPoint.getIssues())
                .extracting(ValidationReport.AttributeIssue::getAttributeCode)
                .containsExactlyInAnyOrder("nodeIeeeAddress", "endpointId", "clusterId", "attributeId");
    }

    @Test
    void deviceHealthIsOfflineWhenNoPointCarriesANodeAddress() {
        ReflectionTestUtils.setField(service, "networkManager", networkManager);
        when(deviceMetadata.getPointConfig(7L)).thenReturn(new HashMap<>());

        DeviceHealthState health = service.health(driverConfig(), device());

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void deviceHealthIsOfflineBeforeTheNetworkStarts() {
        DeviceHealthState health = service.health(driverConfig(), device());

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
        verifyNoInteractions(networkManagerFactory, deviceMetadata);
    }

    @Test
    void deviceHealthIsOnlineWhenAnyPointResolvesToAKnownNode() {
        ReflectionTestUtils.setField(service, "networkManager", networkManager);
        Map<Long, Map<String, AttributeBO>> points = new HashMap<>();
        points.put(9L, pointConfig());
        when(deviceMetadata.getPointConfig(7L)).thenReturn(points);
        when(networkManager.getNode(any(IeeeAddress.class))).thenReturn(node);

        DeviceHealthState health = service.health(driverConfig(), device());

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
    }

    @Test
    void writeWaitsForSuccessfulProtocolAcknowledgement() {
        stubNetworkStartup();
        stubAttributePath();
        when(attribute.writeValue("42")).thenReturn(CompletableFuture.completedFuture(commandResult));
        when(commandResult.isSuccess()).thenReturn(true);

        boolean written = service.write(
                driverConfig(),
                pointConfig(),
                device(),
                point(),
                WritePointValue.builder().type(PointTypeEnum.STRING).value("42").build());

        assertThat(written).isTrue();
        verify(attribute).writeValue("42");
    }

    @Test
    void writeRejectsNegativeProtocolAcknowledgement() {
        stubNetworkStartup();
        stubAttributePath();
        when(attribute.writeValue("42")).thenReturn(CompletableFuture.completedFuture(commandResult));
        when(commandResult.isSuccess()).thenReturn(false);

        assertThatThrownBy(() -> service.write(
                        driverConfig(),
                        pointConfig(),
                        device(),
                        point(),
                        WritePointValue.builder()
                                .type(PointTypeEnum.STRING)
                                .value("42")
                                .build()))
                .isInstanceOf(WritePointException.class)
                .hasMessageContaining("write rejected");
    }

    private Map<String, AttributeBO> driverConfig(String dongleType) {
        Map<String, AttributeBO> config = driverConfig();
        config.put("dongleType", attribute(dongleType, AttributeTypeEnum.STRING));
        return config;
    }

    private void stubNetworkStartup() {
        when(networkManagerFactory.create("TELEGESIS", "/dev/ttyUSB0", 115200)).thenReturn(networkManager);
        when(networkManager.initialize()).thenReturn(ZigBeeStatus.SUCCESS);
        when(networkManager.startup(true)).thenReturn(ZigBeeStatus.SUCCESS);
    }

    private void stubAttributePath() {
        when(networkManager.getNode(any(IeeeAddress.class))).thenReturn(node);
        when(node.getEndpoint(1)).thenReturn(endpoint);
        when(endpoint.getInputCluster(1026)).thenReturn(cluster);
        when(cluster.getAttribute(0)).thenReturn(attribute);
    }
}
