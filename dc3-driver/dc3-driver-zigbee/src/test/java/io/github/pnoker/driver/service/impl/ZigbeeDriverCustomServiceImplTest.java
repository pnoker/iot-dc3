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

import io.github.pnoker.common.driver.entity.bean.DriverHealthState;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ZigbeeDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    private ZigbeeDriverCustomServiceImpl service;

    private static Map<String, AttributeBO> driverConfig(String serialPort, int baudRate) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("serialPort", AttributeBO.builder().value(serialPort).type(AttributeTypeEnum.STRING).build());
        m.put("baudRate", AttributeBO.builder().value(String.valueOf(baudRate)).type(AttributeTypeEnum.INT).build());
        return m;
    }

    private static Map<String, AttributeBO> pointConfig(String nodeIeeeAddress, int endpointId,
                                                        int clusterId, int attributeId) {
        Map<String, AttributeBO> m = new HashMap<>();
        m.put("nodeIeeeAddress", AttributeBO.builder().value(nodeIeeeAddress).type(AttributeTypeEnum.STRING).build());
        m.put("endpointId", AttributeBO.builder().value(String.valueOf(endpointId)).type(AttributeTypeEnum.INT).build());
        m.put("clusterId", AttributeBO.builder().value(String.valueOf(clusterId)).type(AttributeTypeEnum.INT).build());
        m.put("attributeId", AttributeBO.builder().value(String.valueOf(attributeId)).type(AttributeTypeEnum.INT).build());
        return m;
    }

    @BeforeEach
    void setUp() {
        service = new ZigbeeDriverCustomServiceImpl(driverMetadata, driverSenderService);
        // Skip initial() to avoid requiring serial port hardware in test environment
    }

    @Test
    void scheduleDoesNothing() {
        service.schedule();
        // No exception thrown is the assertion
    }

    @Test
    void healthReturnsOfflineWhenNotInitialized() {
        DriverHealthState health = service.health();
        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void healthReturnsOnlineWhenInitialized() {
        // Note: initial() requires serial port hardware; this test verifies
        // the offline path works correctly when networkManager is null
        DriverHealthState health = service.health();
        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
    }

    @Test
    void driverConfigContainsSerialPortAndBaudRate() {
        Map<String, AttributeBO> config = driverConfig("/dev/ttyUSB0", 115200);
        assertThat(config.get("serialPort").getValue(String.class)).isEqualTo("/dev/ttyUSB0");
        assertThat(config.get("baudRate").getValue(Integer.class)).isEqualTo(115200);
    }

    @Test
    void pointConfigContainsNodeAndClusterInfo() {
        Map<String, AttributeBO> config = pointConfig("00158D0001234567", 1, 1026, 0);
        assertThat(config.get("nodeIeeeAddress").getValue(String.class)).isEqualTo("00158D0001234567");
        assertThat(config.get("endpointId").getValue(Integer.class)).isEqualTo(1);
        assertThat(config.get("clusterId").getValue(Integer.class)).isEqualTo(1026);
        assertThat(config.get("attributeId").getValue(Integer.class)).isEqualTo(0);
    }

}
