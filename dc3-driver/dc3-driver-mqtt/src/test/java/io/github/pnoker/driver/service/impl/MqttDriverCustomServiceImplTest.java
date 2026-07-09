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
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.driver.service.MqttSendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MqttDriverCustomServiceImplTest {

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private MqttSendService mqttSendService;

    private MqttDriverCustomServiceImpl service;

    private static DeviceBO device(Long id) {
        DeviceBO device = new DeviceBO();
        device.setId(id);
        return device;
    }

    private static PointBO point(Long id) {
        PointBO point = new PointBO();
        point.setId(id);
        return point;
    }

    private static MetadataEventDTO metadataEvent(MetadataTypeEnum type, MetadataOperateTypeEnum op, Long id) {
        MetadataEventDTO event = new MetadataEventDTO();
        event.setMetadataType(type);
        event.setOperateType(op);
        event.setId(id);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MqttPahoMessageDrivenChannelAdapter> adapterProvider(
            MqttPahoMessageDrivenChannelAdapter adapter) {
        ObjectProvider<MqttPahoMessageDrivenChannelAdapter> provider = mock(ObjectProvider.class);
        lenient().when(provider.getIfAvailable()).thenReturn(adapter);
        return provider;
    }

    @BeforeEach
    void setUp() {
        // Publish-only driver: no inbound adapter configured.
        service = new MqttDriverCustomServiceImpl(driverMetadata, driverSenderService, mqttSendService,
                adapterProvider(null));
    }

    private MqttDriverCustomServiceImpl serviceWithAdapter() {
        return new MqttDriverCustomServiceImpl(driverMetadata, driverSenderService, mqttSendService,
                adapterProvider(mock(MqttPahoMessageDrivenChannelAdapter.class)));
    }

    @Test
    void initialIsNoOp() {
        assertThatNoException().isThrownBy(() -> service.initial());
        verifyNoInteractions(mqttSendService, driverSenderService, driverMetadata);
    }

    @Test
    void scheduleDoesNotReportDeviceStatus() {
        assertThatNoException().isThrownBy(() -> service.schedule());
        verifyNoInteractions(driverSenderService);
    }

    @Test
    void deviceAndPointEventsAreLoggedOnly() {
        assertThatNoException().isThrownBy(
                () -> service.event(metadataEvent(MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, 1L)));
        assertThatNoException().isThrownBy(
                () -> service.event(metadataEvent(MetadataTypeEnum.POINT, MetadataOperateTypeEnum.UPDATE, 2L)));
        verifyNoInteractions(mqttSendService, driverSenderService, driverMetadata);
    }

    @Test
    void readReturnsNullBecauseMqttIsPassive() {
        assertThat(service.read(null, null, device(1L), point(1L))).isNull();
    }

    @Test
    void writePublishesWithConfiguredQosWhenAvailable() {
        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("commandTopic",
                AttributeBO.builder().value("dc3/cmd/temp").type(AttributeTypeEnum.STRING).build());
        pointConfig.put("commandQos", AttributeBO.builder().value("1").type(AttributeTypeEnum.INT).build());

        Boolean ok = service.write(null, pointConfig, device(1L), point(1L),
                WritePointValue.builder().value("23.5").type(PointTypeEnum.STRING).build());

        assertThat(ok).isTrue();
        verify(mqttSendService).sendToMqtt("dc3/cmd/temp", 1, "23.5");
        verify(mqttSendService, never()).sendToMqtt(eq("dc3/cmd/temp"), eq("23.5"));
    }

    @Test
    void writeFallsBackToDefaultQosWhenLookupFails() {
        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("commandTopic",
                AttributeBO.builder().value("dc3/cmd/temp").type(AttributeTypeEnum.STRING).build());
        // commandQos missing entirely → service catches NPE and falls back

        Boolean ok = service.write(null, pointConfig, device(1L), point(1L),
                WritePointValue.builder().value("23.5").type(PointTypeEnum.STRING).build());

        assertThat(ok).isTrue();
        verify(mqttSendService).sendToMqtt("dc3/cmd/temp", "23.5");
    }

    @Test
    void writeFallsBackToDefaultQosWhenQosTypeIsWrong() {
        Map<String, AttributeBO> pointConfig = new HashMap<>();
        pointConfig.put("commandTopic",
                AttributeBO.builder().value("dc3/cmd/temp").type(AttributeTypeEnum.STRING).build());
        // commandQos present but as STRING -> AttributeBO.getValue(Integer.class) throws
        // TypeException, service catches and falls back to default-QoS overload.
        pointConfig.put("commandQos",
                AttributeBO.builder().value("not-a-number").type(AttributeTypeEnum.STRING).build());

        Boolean ok = service.write(null, pointConfig, device(1L), point(1L),
                WritePointValue.builder().value("23.5").type(PointTypeEnum.STRING).build());

        assertThat(ok).isTrue();
        verify(mqttSendService).sendToMqtt("dc3/cmd/temp", "23.5");
    }

    @Test
    void healthReportsOfflineWhenAdapterDisconnected() {
        MqttDriverCustomServiceImpl svc = serviceWithAdapter();
        // Inbound adapter emits MqttConnectionFailedEvent when the broker drops.
        svc.onApplicationEvent(new MqttConnectionFailedEvent(new Object(), new RuntimeException("broker down")));

        DriverHealthState health = svc.health();

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.OFFLINE);
        assertThat(health.getDescription()).isNull();
    }

    @Test
    void healthReportsOnlineWhenAdapterConnectedAndRunning() {
        MqttDriverCustomServiceImpl svc = serviceWithAdapter();
        // Inbound adapter emits MqttSubscribedEvent once connected and subscribed.
        svc.onApplicationEvent(new MqttSubscribedEvent(new Object(), "subscribed"));

        DriverHealthState health = svc.health();

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
        assertThat(health.getDescription()).isNull();
    }

    @Test
    void healthFallsBackToOnlineWhenNoAdapterConfigured() {
        // service built in setUp() uses adapterProvider(null) — publish-only driver.

        DriverHealthState health = service.health();

        assertThat(health.getStatus()).isEqualTo(EntityStatusEnum.ONLINE);
        assertThat(health.getDescription()).contains("no inbound adapter");
    }

}
