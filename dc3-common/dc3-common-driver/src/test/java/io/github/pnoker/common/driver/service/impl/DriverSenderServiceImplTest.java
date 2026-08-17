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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.driver.buffer.BufferService;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverSenderServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private BufferService bufferService;

    private DriverMetadata metadata;
    private DriverSenderServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.setNode("node-a");
        properties.setService("tenant/driver");
        metadata = new DriverMetadata();
        DriverBO driver = new DriverBO();
        driver.setId(20L);
        driver.setTenantId(1L);
        metadata.setDriver(driver);
        metadata.setDeviceLeases(Map.of(10L, 77L), System.currentTimeMillis() + 10_000, 5L);
        service = new DriverSenderServiceImpl(properties, metadata, rabbitTemplate, bufferService);
    }

    @Test
    void stampsStableWireIdentityAndFenceBeforeOutboxPublish() {
        PointValue value = PointValue.builder()
                .deviceId(10L)
                .pointId(30L)
                .rawValue("42")
                .calValue("42")
                .build();

        service.pointValueSender(value);

        ArgumentCaptor<PointValue> captor = ArgumentCaptor.forClass(PointValue.class);
        verify(bufferService).publish(captor.capture(),
                org.mockito.ArgumentMatchers.eq("dc3.r.value.point.tenant/driver"));
        PointValue sent = captor.getValue();
        assertThat(sent.getMessageId()).isNotBlank();
        assertThat(sent.getSchemaVersion()).isEqualTo(1);
        assertThat(sent.getDriverNode()).isEqualTo("node-a");
        assertThat(sent.getSequence()).isPositive();
        assertThat(sent.getFencingToken()).isEqualTo(77L);
        assertThat(sent.getDriverId()).isEqualTo(20L);
        assertThat(sent.getTenantId()).isEqualTo(1L);
    }

    @Test
    void rejectsTelemetryAfterLeaseExpiry() {
        metadata.renewLeaseDeadline(System.currentTimeMillis() - 1);

        service.pointValueSender(PointValue.builder().deviceId(10L).pointId(30L).rawValue("42").build());

        verify(bufferService, never()).publish(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void stampsAndPersistsListAsOneOutboxBatch() {
        PointValue first = PointValue.builder().deviceId(10L).pointId(30L).rawValue("1").build();
        PointValue second = PointValue.builder().deviceId(10L).pointId(31L).rawValue("2").build();

        service.pointValueSender(List.of(first, second));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointValue>> captor = ArgumentCaptor.forClass(List.class);
        verify(bufferService).publishBatch(captor.capture(),
                org.mockito.ArgumentMatchers.eq("dc3.r.value.point.tenant/driver"));
        assertThat(captor.getValue()).hasSize(2);
        assertThat(first.getMessageId()).isNotBlank().isNotEqualTo(second.getMessageId());
        assertThat(first.getSequence()).isLessThan(second.getSequence());
        assertThat(captor.getValue()).allSatisfy(value -> {
            assertThat(value.getFencingToken()).isEqualTo(77L);
            assertThat(value.getDriverId()).isEqualTo(20L);
            assertThat(value.getTenantId()).isEqualTo(1L);
        });
        verify(bufferService, never()).publish(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
