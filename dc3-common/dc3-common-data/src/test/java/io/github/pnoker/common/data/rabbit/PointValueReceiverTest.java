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
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PointValueReceiverTest {

    @Mock
    private PointValueService pointValueService;

    @Mock
    private Channel channel;

    private PointValueReceiver receiver;

    @BeforeEach
    void setUp() {
        receiver = new PointValueReceiver(pointValueService);
    }

    @Test
    void persistsCompleteBatchBeforeAcknowledging() throws Exception {
        Message first = message(validValue("m-1", 1L), 7L);
        Message second = message(validValue("m-2", 2L), 8L);

        receiver.pointValueReceive(List.of(first, second), channel);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointValueBO>> captor = ArgumentCaptor.forClass(List.class);
        verify(pointValueService).save(captor.capture());
        assertThat(captor.getValue()).extracting(PointValueBO::getMessageId)
                .containsExactly("m-1", "m-2");
        verify(channel).basicAck(8L, true);
    }

    @Test
    void doesNotAcknowledgeWhenPersistenceFails() throws Exception {
        doThrow(new IllegalStateException("database unavailable"))
                .when(pointValueService).save(anyList());

        Message message = message(validValue("m-1", 1L), 7L);

        assertThatThrownBy(() -> receiver.pointValueReceive(List.of(message), channel))
                .isInstanceOf(IllegalStateException.class);
        verify(channel, never()).basicAck(7L, true);
    }

    @Test
    void rejectsEntireBatchWhenWireContractIsInvalid() {
        PointValueBO invalid = validValue("m-1", 1L);
        invalid.setDriverNode(null);

        assertThatThrownBy(() -> receiver.pointValueReceive(List.of(message(invalid, 7L)), channel))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(pointValueService);
    }

    @Test
    void rejectsMalformedJson() {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        Message malformed = new Message("{".getBytes(StandardCharsets.UTF_8), properties);

        assertThatThrownBy(() -> receiver.pointValueReceive(List.of(malformed), channel))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(pointValueService);
    }

    private PointValueBO validValue(String messageId, long sequence) {
        return PointValueBO.builder()
                .messageId(messageId)
                .schemaVersion(1)
                .driverNode("node-a")
                .sequence(sequence)
                .fencingToken(77L)
                .tenantId(100L)
                .driverId(200L)
                .deviceId(10L)
                .pointId(20L)
                .rawValue("42")
                .calValue("42")
                .createTime(LocalDateTime.now())
                .build();
    }

    private Message message(PointValueBO value, long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(JsonUtil.toJsonString(value).getBytes(StandardCharsets.UTF_8), properties);
    }
}
