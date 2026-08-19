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
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import io.github.pnoker.common.mq.sender.MqPublishException;
import io.github.pnoker.common.mq.sender.SendConfirmation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.ReturnedMessage;

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
    private MessageSender messageSender;

    private BufferServiceImpl service;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.getBuffer().setDbPath(tmp.resolve("outbox.db").toString());
        properties.getBuffer().setBatchSize(10);
        properties.getBuffer().setBackoffSeconds(1);
        service = new BufferServiceImpl(properties, messageSender);
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
            SendConfirmation confirmation = invocation.getArgument(1);
            confirmation.onConfirm(invocation.getArgument(0), true, null);
            return null;
        }).when(messageSender).sendAsync(any(MqMessage.class), any(SendConfirmation.class));

        service.publish(pointValue("id-1"), "rk");

        assertThat(service.pendingCount()).isZero();
    }

    @Test
    void nackRetainsRecordForRetry() {
        doAnswer(invocation -> {
            SendConfirmation confirmation = invocation.getArgument(1);
            confirmation.onConfirm(invocation.getArgument(0), false, new MqPublishException("broker nack"));
            return null;
        }).when(messageSender).sendAsync(any(MqMessage.class), any(SendConfirmation.class));

        service.publish(pointValue("id-2"), "rk");

        assertThat(service.pendingCount()).isEqualTo(1);
    }

    @Test
    void returnedMessageRetainsRecordDespiteAck() {
        doAnswer(invocation -> {
            // routed=false covers the returned-message case: the outbox must retain the row
            SendConfirmation confirmation = invocation.getArgument(1);
            confirmation.onConfirm(invocation.getArgument(0), false, null);
            return null;
        }).when(messageSender).sendAsync(any(MqMessage.class), any(SendConfirmation.class));

        service.publish(pointValue("id-3"), "rk");

        assertThat(service.pendingCount()).isEqualTo(1);
    }

    @Test
    void synchronousFailureRetainsRecord() {
        doThrow(new MqPublishException("broker down")).when(messageSender)
                .sendAsync(any(MqMessage.class), any(SendConfirmation.class));

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
            SendConfirmation confirmation = invocation.getArgument(1);
            confirmation.onConfirm(invocation.getArgument(0), true, null);
            return null;
        }).when(messageSender).sendAsync(any(MqMessage.class), any(SendConfirmation.class));

        service.publishBatch(List.of(pointValue("batch-1"), pointValue("batch-2")), "rk");

        assertThat(service.pendingCount()).isZero();
        verify(messageSender, times(2)).sendAsync(any(MqMessage.class), any(SendConfirmation.class));
    }

    @Test
    void publishFailsClosedBeforeOutboxInitialization() {
        BufferServiceImpl uninitialized = new BufferServiceImpl(new DriverProperties(), messageSender);

        assertThatThrownBy(() -> uninitialized.publish(pointValue("id-5"), "rk"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox is not initialized");
        verifyNoInteractions(messageSender);
    }

    private PointValue pointValue(String messageId) {
        return PointValue.builder().messageId(messageId).deviceId(1L).pointId(2L).rawValue("42").build();
    }
}
