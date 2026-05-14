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

package io.github.pnoker.common.data.rabbit;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.biz.DeviceEventService;
import io.github.pnoker.common.entity.dto.DeviceEventDTO;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeviceEventReceiverTest {

    @Mock
    private DeviceEventService deviceEventService;

    @Mock
    private Channel channel;

    private DeviceEventReceiver receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        receiver = new DeviceEventReceiver(deviceEventService);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
    }

    @Test
    void heartbeatEventIsAckedAfterServiceCall() throws Exception {
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setType(DeviceEventTypeEnum.HEARTBEAT);
        dto.setContent("{}");

        receiver.deviceEventReceive(channel, message, dto);

        verify(deviceEventService).heartbeatEvent(dto);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void alarmEventIsAckedAfterServiceCall() throws Exception {
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setType(DeviceEventTypeEnum.ALARM);
        dto.setContent("{}");

        receiver.deviceEventReceive(channel, message, dto);

        verify(deviceEventService).alarmEvent(dto);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.deviceEventReceive(channel, message, null);
        verifyNoInteractions(deviceEventService);
        verify(channel).basicReject(eq(7L), eq(false));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithoutType() throws Exception {
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setContent("{}");
        receiver.deviceEventReceive(channel, message, dto);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithBlankContent() throws Exception {
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setType(DeviceEventTypeEnum.HEARTBEAT);
        dto.setContent("");
        receiver.deviceEventReceive(channel, message, dto);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void nacksAndRequeuesOnServiceFailure() throws Exception {
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setType(DeviceEventTypeEnum.HEARTBEAT);
        dto.setContent("{}");
        doThrow(new RuntimeException("downstream offline")).when(deviceEventService).heartbeatEvent(dto);

        receiver.deviceEventReceive(channel, message, dto);

        verify(channel).basicNack(eq(7L), eq(false), eq(true));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }

    @Test
    void nacksWhenAckIoFails() throws Exception {
        // Belt-and-braces: even if basicAck throws (e.g. broker disconnected mid-message)
        // we expect the receiver to fall through to basicNack via the catch-all rather
        // than letting the IOException bubble up and stall the listener container.
        DeviceEventDTO dto = new DeviceEventDTO();
        dto.setType(DeviceEventTypeEnum.HEARTBEAT);
        dto.setContent("{}");
        doThrow(new java.io.IOException("broker gone"))
                .when(channel).basicAck(eq(7L), eq(false));
        receiver.deviceEventReceive(channel, message, dto);
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
    }
}
