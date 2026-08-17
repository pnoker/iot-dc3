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

package io.github.pnoker.common.driver.buffer;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BufferServiceImplTest {

    @TempDir
    Path tmp;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BufferServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.getBuffer().setDbPath(tmp.resolve("outbox.db").toString());
        properties.getBuffer().setBatchSize(10);
        properties.getBuffer().setBackoffSeconds(1);
        service = new BufferServiceImpl(properties, rabbitTemplate);
        service.initialize();
    }

    @AfterEach
    void tearDown() {
        service.destroy();
    }

    @Test
    void publishPersistsBeforeSendAndDeletesOnlyAfterRoutedConfirm() {
        doAnswer(invocation -> {
            assertThat(service.pendingCount()).isEqualTo(1);
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));

        service.publish(pointValue("id-1"), "rk");

        assertThat(service.pendingCount()).isZero();
    }

    @Test
    void nackRetainsRecordForRetry() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));

        service.publish(pointValue("id-2"), "rk");

        assertThat(service.pendingCount()).isEqualTo(1);
    }

    @Test
    void returnedMessageRetainsRecordDespiteAck() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                    312, "NO_ROUTE", "exchange", "rk"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));

        service.publish(pointValue("id-3"), "rk");

        assertThat(service.pendingCount()).isEqualTo(1);
    }

    @Test
    void synchronousFailureRetainsRecord() {
        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));

        service.publish(pointValue("id-4"), "rk");

        assertThat(service.pendingCount()).isEqualTo(1);
    }

    @Test
    void batchPersistsEverythingBeforeFirstPublish() {
        AtomicInteger sends = new AtomicInteger();
        doAnswer(invocation -> {
            if (sends.getAndIncrement() == 0) {
                assertThat(service.pendingCount()).isEqualTo(2);
            }
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));

        service.publishBatch(List.of(pointValue("batch-1"), pointValue("batch-2")), "rk");

        assertThat(service.pendingCount()).isZero();
        verify(rabbitTemplate, times(2))
                .convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));
    }

    @Test
    void publishFailsClosedBeforeOutboxInitialization() {
        BufferServiceImpl uninitialized = new BufferServiceImpl(new DriverProperties(), rabbitTemplate);

        assertThatThrownBy(() -> uninitialized.publish(pointValue("id-5"), "rk"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox is not initialized");
        verifyNoInteractions(rabbitTemplate);
    }

    private PointValue pointValue(String messageId) {
        return PointValue.builder().messageId(messageId).deviceId(1L).pointId(2L).rawValue("42").build();
    }
}
