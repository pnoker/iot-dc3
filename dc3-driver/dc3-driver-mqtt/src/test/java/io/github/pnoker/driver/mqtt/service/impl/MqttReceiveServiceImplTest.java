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

package io.github.pnoker.driver.mqtt.service.impl;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeDTO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.grpc.client.DeviceClient;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.mqtt.entity.MessageHeader;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttReceiveServiceImplTest {

    @Mock
    private DriverSenderService driverSenderService;
    @Mock
    private DeviceClient deviceClient;
    @Mock
    private EventFacade eventFacade;

    private DriverMetadata driverMetadata;
    private DeviceMetadata deviceMetadata;
    private MqttReceiveServiceImpl service;

    private static MqttMessage mqttMessage(String topic, int qos, String payload) {
        MessageHeader header = new MessageHeader(null);
        header.setMqttReceivedTopic(topic);
        header.setMqttReceivedQos(qos);
        MqttMessage msg = new MqttMessage();
        msg.setHeader(header);
        msg.setPayload(payload);
        return msg;
    }

    private static EventAttributeDTO eventAttribute(Long id, String code) {
        EventAttributeDTO attribute = new EventAttributeDTO();
        attribute.setId(id);
        attribute.setAttributeCode(code);
        attribute.setAttributeTypeFlag(AttributeTypeEnum.STRING);
        return attribute;
    }

    private static EventAttributeConfigDTO eventConfig(Long attributeId, Long deviceId, Long eventId, String value) {
        EventAttributeConfigDTO config = new EventAttributeConfigDTO();
        config.setAttributeId(attributeId);
        config.setDeviceId(deviceId);
        config.setEventId(eventId);
        config.setConfigValue(value);
        return config;
    }

    @BeforeEach
    void setUp() {
        driverMetadata = new DriverMetadata();
        deviceMetadata = new DeviceMetadata(new DriverProperties(), driverMetadata, deviceClient);
        service = new MqttReceiveServiceImpl(driverSenderService, driverMetadata, deviceMetadata, eventFacade);
    }

    @Test
    void singleReceiveStampsCreateTimeAndForwardsToSender() {
        MqttMessage msg = mqttMessage("dc3/temp", 1,
                "{\"deviceId\":10,\"pointId\":20,\"rawValue\":\"23.5\",\"calValue\":\"23.5\"}");

        service.receiveValue(msg);

        ArgumentCaptor<PointValue> captor = ArgumentCaptor.forClass(PointValue.class);
        verify(driverSenderService).pointValueSender(captor.capture());
        PointValue pv = captor.getValue();
        assertThat(pv.getDeviceId()).isEqualTo(10L);
        assertThat(pv.getPointId()).isEqualTo(20L);
        assertThat(pv.getRawValue()).isEqualTo("23.5");
        assertThat(pv.getCreateTime()).isNotNull();
    }

    @Test
    void batchReceiveStampsEachAndForwardsListInOrder() {
        MqttMessage one = mqttMessage("dc3/temp", 1,
                "{\"deviceId\":10,\"pointId\":1,\"rawValue\":\"1\",\"calValue\":\"1\"}");
        MqttMessage two = mqttMessage("dc3/temp", 1,
                "{\"deviceId\":10,\"pointId\":2,\"rawValue\":\"2\",\"calValue\":\"2\"}");

        service.receiveValues(List.of(one, two));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<PointValue>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(driverSenderService).pointValueSender(captor.capture());
        List<PointValue> values = captor.getValue();
        assertThat(values).hasSize(2);
        assertThat(values.get(0).getPointId()).isEqualTo(1L);
        assertThat(values.get(1).getPointId()).isEqualTo(2L);
        assertThat(values).allSatisfy(pv -> assertThat(pv.getCreateTime()).isNotNull());
    }

    @Test
    void singleReceiveToleratesNullHeader() {
        MqttMessage msg = new MqttMessage();
        msg.setHeader(null);
        msg.setPayload("{\"deviceId\":1,\"pointId\":2,\"rawValue\":\"x\",\"calValue\":\"x\"}");

        service.receiveValue(msg);

        verify(driverSenderService).pointValueSender(org.mockito.ArgumentMatchers.any(PointValue.class));
    }

    @Test
    void receiveEventMessageMatchesConfiguredTopicAndReportsEvent() {
        driverMetadata.getDeviceIds().add(10L);
        driverMetadata.setEventAttributeIdMap(Map.of(
                1L, eventAttribute(1L, "sourceTopic"),
                2L, eventAttribute(2L, "eventCodePath"),
                3L, eventAttribute(3L, "payloadPath")
        ));

        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setTenantId(1L);
        device.setDeviceCode("device-a");
        device.setEventAttributeConfigIdMap(Map.of(20L, Map.of(
                1L, eventConfig(1L, 10L, 20L, "dc3/event/device-a"),
                2L, eventConfig(2L, 10L, 20L, "$.eventCode"),
                3L, eventConfig(3L, 10L, 20L, "$.payload")
        )));
        when(deviceClient.getById(10L)).thenReturn(device);

        FacadeEventBO event = new FacadeEventBO();
        event.setId(20L);
        event.setTenantId(1L);
        event.setEventCode("alarm");
        event.setEventTypeFlag(EventTypeFlagEnum.ALERT);
        event.setEventLevelFlag(EventLevelEnum.HIGH);
        event.setEnableFlag(EnableFlagEnum.ENABLE);
        when(eventFacade.getById(1L, 20L)).thenReturn(event);

        MqttMessage msg = mqttMessage("dc3/event/device-a", 1,
                "{\"eventCode\":\"alarm\",\"payload\":{\"temperature\": \"92\", \"source\":\"mqtt\"}}");

        service.receiveValue(msg);

        ArgumentCaptor<EventReportDTO> captor = ArgumentCaptor.forClass(EventReportDTO.class);
        verify(driverSenderService).eventReportSender(captor.capture());
        verify(driverSenderService, never()).pointValueSender(any(PointValue.class));
        EventReportDTO report = captor.getValue();
        assertThat(report.deviceId()).isEqualTo(10L);
        assertThat(report.eventId()).isEqualTo(20L);
        assertThat(report.eventCode()).isEqualTo("alarm");
        assertThat(report.paramValues()).containsEntry("temperature", "92").containsEntry("source", "mqtt");
        assertThat(report.configSnapshot()).contains("sourceTopic").contains("eventCodePath").contains("payloadPath");
    }

    @Test
    void eventMessageWithPointIdentityReportsBothEventAndPointValue() {
        driverMetadata.getDeviceIds().add(10L);
        driverMetadata.setEventAttributeIdMap(Map.of(
                1L, eventAttribute(1L, "sourceTopic"),
                2L, eventAttribute(2L, "eventCodePath"),
                3L, eventAttribute(3L, "payloadPath")
        ));

        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setTenantId(1L);
        device.setDeviceCode("device-a");
        device.setEventAttributeConfigIdMap(Map.of(20L, Map.of(
                1L, eventConfig(1L, 10L, 20L, "dc3/event/device-a"),
                2L, eventConfig(2L, 10L, 20L, "$.eventCode"),
                3L, eventConfig(3L, 10L, 20L, "$.payload")
        )));
        when(deviceClient.getById(10L)).thenReturn(device);

        FacadeEventBO event = new FacadeEventBO();
        event.setId(20L);
        event.setTenantId(1L);
        event.setEventCode("alarm");
        event.setEventTypeFlag(EventTypeFlagEnum.ALERT);
        event.setEventLevelFlag(EventLevelEnum.HIGH);
        event.setEnableFlag(EnableFlagEnum.ENABLE);
        when(eventFacade.getById(1L, 20L)).thenReturn(event);

        MqttMessage msg = mqttMessage("dc3/event/device-a", 1,
                "{\"deviceId\":10,\"pointId\":30,\"rawValue\":\"92\",\"eventCode\":\"alarm\",\"payload\":{\"temperature\":\"92\"}}");

        service.receiveValue(msg);

        verify(driverSenderService).eventReportSender(any(EventReportDTO.class));
        ArgumentCaptor<PointValue> pointCaptor = ArgumentCaptor.forClass(PointValue.class);
        verify(driverSenderService).pointValueSender(pointCaptor.capture());
        assertThat(pointCaptor.getValue().getDeviceId()).isEqualTo(10L);
        assertThat(pointCaptor.getValue().getPointId()).isEqualTo(30L);
    }

    @Test
    void eventReportFailureDoesNotDropPointValue() {
        driverMetadata.getDeviceIds().add(10L);
        driverMetadata.setEventAttributeIdMap(Map.of(
                1L, eventAttribute(1L, "sourceTopic"),
                2L, eventAttribute(2L, "eventCodePath"),
                3L, eventAttribute(3L, "payloadPath")
        ));

        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setTenantId(1L);
        device.setEventAttributeConfigIdMap(Map.of(20L, Map.of(
                1L, eventConfig(1L, 10L, 20L, "dc3/event/device-a"),
                2L, eventConfig(2L, 10L, 20L, "$.eventCode"),
                3L, eventConfig(3L, 10L, 20L, "$.payload")
        )));
        when(deviceClient.getById(10L)).thenReturn(device);
        when(eventFacade.getById(1L, 20L)).thenThrow(new RuntimeException("metadata unavailable"));

        MqttMessage msg = mqttMessage("dc3/event/device-a", 1,
                "{\"deviceId\":10,\"pointId\":30,\"rawValue\":\"92\",\"eventCode\":\"alarm\",\"payload\":{\"temperature\":\"92\"}}");

        service.receiveValue(msg);

        verify(driverSenderService).pointValueSender(any(PointValue.class));
    }
}
