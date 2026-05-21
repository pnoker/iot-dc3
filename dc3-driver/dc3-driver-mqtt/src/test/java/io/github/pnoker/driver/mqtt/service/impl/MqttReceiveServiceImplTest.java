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
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.mqtt.entity.MessageHeader;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqttReceiveServiceImplTest {

    @Mock
    private DriverSenderService driverSenderService;

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

    @BeforeEach
    void setUp() {
        service = new MqttReceiveServiceImpl(driverSenderService);
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
}
