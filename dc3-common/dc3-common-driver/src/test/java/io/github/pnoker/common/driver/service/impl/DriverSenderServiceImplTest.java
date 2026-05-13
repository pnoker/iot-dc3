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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverSenderServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DriverSenderServiceImpl service;
    private DriverProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DriverProperties();
        properties.setService("dc3-driver-modbus-tcp");
        service = new DriverSenderServiceImpl(properties, driverMetadata, rabbitTemplate);
    }

    private DriverBO driver(Long id, Long tenantId) {
        DriverBO bo = new DriverBO();
        bo.setId(id);
        bo.setTenantId(tenantId);
        return bo;
    }

    @Test
    void driverEventSenderSilentlyDropsNullPayload() {
        assertThatNoException().isThrownBy(() -> service.driverEventSender(null));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void driverEventSenderRoutesToServiceSpecificKey() {
        DriverEventDTO dto = new DriverEventDTO();
        service.driverEventSender(dto);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_EVENT),
                eq(RabbitConstant.ROUTING_DRIVER_EVENT_PREFIX + "dc3-driver-modbus-tcp"),
                eq(dto));
    }

    @Test
    void deviceEventSenderRoutesToDeviceEventKey() {
        DeviceEventDTO dto = new DeviceEventDTO();
        service.deviceEventSender(dto);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConstant.TOPIC_EXCHANGE_EVENT),
                eq(RabbitConstant.ROUTING_DEVICE_EVENT_PREFIX + "dc3-driver-modbus-tcp"),
                eq(dto));
    }

    @Test
    void deviceStatusSenderUsesDefaultTimeoutFifteenMinutes() {
        when(driverMetadata.getDriver()).thenReturn(driver(7L, 1L));
        service.deviceStatusSender(10L, DeviceStatusEnum.ONLINE);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), captor.capture());
        DeviceEventDTO published = (DeviceEventDTO) captor.getValue();
        assertThat(published.getType()).isEqualTo(DeviceEventTypeEnum.HEARTBEAT);
    }

    @Test
    void deviceStatusSenderHonoursCustomTimeout() {
        when(driverMetadata.getDriver()).thenReturn(driver(7L, 1L));
        assertThatNoException()
                .isThrownBy(() -> service.deviceStatusSender(10L, DeviceStatusEnum.ONLINE, 5, TimeUnit.SECONDS));
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(DeviceEventDTO.class));
    }

    @Test
    void driverAlarmSenderDropsAlarmWhenDriverNotRegistered() {
        when(driverMetadata.getDriver()).thenReturn(null);
        service.driverAlarmSender("network down");
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void driverAlarmSenderPublishesAlarmEventWithDriverIdentity() {
        DriverBO d = driver(7L, 1L);
        when(driverMetadata.getDriver()).thenReturn(d);
        when(driverMetadata.getDriverStatus()).thenReturn(io.github.pnoker.common.enums.DriverStatusEnum.OFFLINE);
        service.driverAlarmSender("network down");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), captor.capture());
        DriverEventDTO event = (DriverEventDTO) captor.getValue();
        assertThat(event.getType()).isEqualTo(DriverEventTypeEnum.ALARM);
        assertThat(event.getContent()).contains("network down");
    }

    @Test
    void deviceAlarmSenderDropsPayloadWithoutDeviceId() {
        service.deviceAlarmSender(null, "msg");
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void deviceAlarmSenderTagsTenantAndDriverWhenAvailable() {
        when(driverMetadata.getDriver()).thenReturn(driver(7L, 1L));
        service.deviceAlarmSender(10L, "boom");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), captor.capture());
        DeviceEventDTO event = (DeviceEventDTO) captor.getValue();
        assertThat(event.getType()).isEqualTo(DeviceEventTypeEnum.ALARM);
        assertThat(event.getContent()).contains("boom");
    }

    @Test
    void pointValueSenderDropsNullEntity() {
        service.pointValueSender((PointValue) null);
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void pointValueSenderTagsDriverIdentityWhenMissing() {
        when(driverMetadata.getDriver()).thenReturn(driver(7L, 1L));
        PointValue pv = new PointValue();
        pv.setDeviceId(10L);
        pv.setPointId(20L);
        service.pointValueSender(pv);

        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
        assertThat(pv.getDriverId()).isEqualTo(7L);
        assertThat(pv.getTenantId()).isEqualTo(1L);
    }

    @Test
    void pointValueSenderRespectsExplicitlySetIdentity() {
        when(driverMetadata.getDriver()).thenReturn(driver(7L, 1L));
        PointValue pv = new PointValue();
        pv.setDeviceId(10L);
        pv.setPointId(20L);
        pv.setDriverId(99L);
        pv.setTenantId(99L);
        service.pointValueSender(pv);
        // Must not overwrite values set by upstream code.
        assertThat(pv.getDriverId()).isEqualTo(99L);
        assertThat(pv.getTenantId()).isEqualTo(99L);
    }

    @Test
    void batchPointValueSenderForwardsAllEntries() {
        when(driverMetadata.getDriver()).thenReturn(driver(7L, 1L));
        PointValue pv1 = new PointValue();
        pv1.setDeviceId(10L);
        pv1.setPointId(20L);
        PointValue pv2 = new PointValue();
        pv2.setDeviceId(10L);
        pv2.setPointId(21L);
        service.pointValueSender(List.of(pv1, pv2));
        verify(rabbitTemplate, times(2)).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void batchPointValueSenderTolerantOfNullList() {
        assertThatNoException().isThrownBy(() -> service.pointValueSender((List<PointValue>) null));
    }
}
