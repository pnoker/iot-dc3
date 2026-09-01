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

import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqPoisonException;
import io.github.pnoker.common.mq.listener.MqReceived;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueReceiverTest {

    @Mock
    private PointValueService pointValueService;

    @Mock
    private Acknowledgment ack;

    private PointValueReceiver receiver;

    @BeforeEach
    void setUp() {
        receiver = new PointValueReceiver(pointValueService);
    }

    @Test
    void persistsCompleteBatchBeforeAcknowledging() {
        MqReceived<PointValueBO> first = received(validValue("m-1", 1L));
        MqReceived<PointValueBO> second = received(validValue("m-2", 2L));
        when(pointValueService.save(anyList())).thenReturn(Mono.empty());

        StepVerifier.create(receiver.pointValueReceive(List.of(first, second), ack))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointValueBO>> captor = ArgumentCaptor.forClass(List.class);
        verify(pointValueService).save(captor.capture());
        assertThat(captor.getValue()).extracting(PointValueBO::getMessageId)
                .containsExactly("m-1", "m-2");
    }

    @Test
    void doesNotAcknowledgeWhenPersistenceFails() {
        when(pointValueService.save(anyList()))
                .thenReturn(Mono.error(new IllegalStateException("database unavailable")));

        StepVerifier.create(receiver.pointValueReceive(List.of(received(validValue("m-1", 1L))), ack))
                .expectErrorMessage("database unavailable")
                .verify();
    }

    @Test
    void rejectsEntireBatchWhenWireContractIsInvalid() {
        PointValueBO invalid = validValue("m-1", 1L);
        invalid.setDriverNode(null);

        assertThatThrownBy(() -> receiver.pointValueReceive(List.of(received(invalid)), ack))
                .isInstanceOf(MqPoisonException.class);
        verifyNoInteractions(pointValueService);
    }

    @Test
    void ignoresEmptyBatch() {
        StepVerifier.create(receiver.pointValueReceive(List.of(), ack)).verifyComplete();

        verifyNoInteractions(pointValueService, ack);
    }

    private MqReceived<PointValueBO> received(PointValueBO value) {
        return new MqReceived<>(value, Map.of(), false);
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
}
