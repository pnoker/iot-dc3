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

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.driver.coap.entity.CoapMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CoapReceiveServiceImplTest {

    @Mock
    private DriverSenderService driverSenderService;

    @Captor
    private ArgumentCaptor<PointValue> pointValueCaptor;

    @Captor
    private ArgumentCaptor<List<PointValue>> pointValueListCaptor;

    private CoapReceiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CoapReceiveServiceImpl(driverSenderService);
    }

    @Test
    void receiveValueParsesJsonAndForwardsToSender() {
        CoapMessage message = CoapMessage.builder()
                .sourceAddress("192.168.1.10")
                .sourcePort(56830)
                .uriPath("data")
                .payload("{\"deviceId\":1,\"pointId\":1,\"rawValue\":\"25.3\"}")
                .contentType("json")
                .method("POST")
                .build();

        service.receiveValue(message);

        verify(driverSenderService).pointValueSender(pointValueCaptor.capture());
        PointValue pv = pointValueCaptor.getValue();
        assertThat(pv.getDeviceId()).isEqualTo(1L);
        assertThat(pv.getPointId()).isEqualTo(1L);
        assertThat(pv.getRawValue()).isEqualTo("25.3");
        assertThat(pv.getCreateTime()).isNotNull();
    }

    @Test
    void receiveValueHandlesNullSourceAddress() {
        CoapMessage message = CoapMessage.builder()
                .payload("{\"deviceId\":2,\"pointId\":3,\"rawValue\":\"ON\"}")
                .build();

        assertThatNoException().isThrownBy(() -> service.receiveValue(message));
        verify(driverSenderService).pointValueSender(pointValueCaptor.capture());
        assertThat(pointValueCaptor.getValue().getDeviceId()).isEqualTo(2L);
    }

    @Test
    void receiveValuesProcessesBatchCorrectly() {
        CoapMessage msg1 = CoapMessage.builder()
                .sourceAddress("192.168.1.10")
                .sourcePort(56830)
                .uriPath("data")
                .payload("{\"deviceId\":1,\"pointId\":1,\"rawValue\":\"25.3\"}")
                .build();
        CoapMessage msg2 = CoapMessage.builder()
                .sourceAddress("192.168.1.11")
                .sourcePort(56831)
                .uriPath("data")
                .payload("{\"deviceId\":2,\"pointId\":2,\"rawValue\":\"42.0\"}")
                .build();

        service.receiveValues(List.of(msg1, msg2));

        verify(driverSenderService).pointValueSender(pointValueListCaptor.capture());
        List<PointValue> values = pointValueListCaptor.getValue();
        assertThat(values).hasSize(2);
        assertThat(values.get(0).getDeviceId()).isEqualTo(1L);
        assertThat(values.get(1).getDeviceId()).isEqualTo(2L);
        assertThat(values.get(0).getCreateTime()).isNotNull();
        assertThat(values.get(1).getCreateTime()).isNotNull();
    }

    @Test
    void receiveValueSkipsMalformedPayload() {
        CoapMessage message = CoapMessage.builder()
                .sourceAddress("192.168.1.10")
                .sourcePort(56830)
                .payload("not-json")
                .build();

        assertThatNoException().isThrownBy(() -> service.receiveValue(message));

        verify(driverSenderService, never()).pointValueSender(org.mockito.ArgumentMatchers.any(PointValue.class));
    }

}
