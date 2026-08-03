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
import io.github.pnoker.common.data.buffer.PointValueIngestBuffer;
import io.github.pnoker.common.entity.bo.PointValueBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the receiver routes messages to the ingest buffer and applies back-pressure
 * (nack-requeue) when the buffer is full.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@ExtendWith(MockitoExtension.class)
class PointValueReceiverTest {

    @Mock
    private PointValueIngestBuffer buffer;

    @Mock
    private Channel channel;

    private PointValueReceiver receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        receiver = new PointValueReceiver(buffer);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.pointValueReceive(channel, message, null);
        verifyNoInteractions(buffer);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithoutDeviceId() throws Exception {
        PointValueBO bo = PointValueBO.builder().pointId(20L).build();
        receiver.pointValueReceive(channel, message, bo);
        verifyNoInteractions(buffer);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void offersAndAcks() throws Exception {
        PointValueBO bo = PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build();
        when(buffer.offer(bo)).thenReturn(true);
        receiver.pointValueReceive(channel, message, bo);
        verify(buffer).offer(bo);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void nacksAndRequeuesWhenBufferFull() throws Exception {
        PointValueBO bo = PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build();
        when(buffer.offer(bo)).thenReturn(false);
        receiver.pointValueReceive(channel, message, bo);
        verify(buffer).offer(bo);
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }
}
