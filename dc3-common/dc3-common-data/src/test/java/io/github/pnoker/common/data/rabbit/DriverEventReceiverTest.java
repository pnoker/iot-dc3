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
import io.github.pnoker.common.data.biz.DriverEventService;
import io.github.pnoker.common.entity.dto.DriverEventDTO;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverEventReceiverTest {

    @Mock
    private DriverEventService driverEventService;

    @Mock
    private Channel channel;

    private DriverEventReceiver receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        receiver = new DriverEventReceiver(driverEventService);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
    }

    @Test
    void heartbeatEventDispatchedAndAcked() throws Exception {
        DriverEventDTO dto = new DriverEventDTO();
        dto.setType(DriverEventTypeEnum.HEARTBEAT);
        dto.setContent("{}");
        receiver.driverEventReceive(channel, message, dto);
        verify(driverEventService).heartbeatEvent(dto);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void alarmEventDispatchedAndAcked() throws Exception {
        DriverEventDTO dto = new DriverEventDTO();
        dto.setType(DriverEventTypeEnum.ALARM);
        dto.setContent("{}");
        receiver.driverEventReceive(channel, message, dto);
        verify(driverEventService).alarmEvent(dto);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.driverEventReceive(channel, message, null);
        verifyNoInteractions(driverEventService);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void nacksOnServiceFailure() throws Exception {
        DriverEventDTO dto = new DriverEventDTO();
        dto.setType(DriverEventTypeEnum.HEARTBEAT);
        dto.setContent("{}");
        doThrow(new RuntimeException("downstream offline")).when(driverEventService).heartbeatEvent(dto);
        receiver.driverEventReceive(channel, message, dto);
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
    }
}
