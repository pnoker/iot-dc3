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
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import io.github.pnoker.common.data.job.PointValueJob;
import io.github.pnoker.common.entity.bo.PointValueBO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private Message message;
    private PointBatchProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PointBatchProperties();
        properties.setSpeed(100);
        properties.setInterval(5);
        receiver = new PointValueReceiver(properties, pointValueService);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
        // Reset shared state used by the rate-throttling branch
        PointValueJob.resetMetrics();
        PointValueJob.clearPointValues();
    }

    @AfterEach
    void resetSharedState() {
        PointValueJob.resetMetrics();
        PointValueJob.clearPointValues();
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.pointValueReceive(channel, message, null);
        verifyNoInteractions(pointValueService);
        verify(channel).basicReject(eq(7L), eq(false));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithoutDeviceId() throws Exception {
        PointValueBO bo = PointValueBO.builder().pointId(20L).build();
        receiver.pointValueReceive(channel, message, bo);
        verifyNoInteractions(pointValueService);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void belowSpeedThresholdSavesImmediatelyAndAcks() throws Exception {
        PointValueBO bo = PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build();
        // VALUE_SPEED defaults to 0 in setUp, below the 100 threshold
        receiver.pointValueReceive(channel, message, bo);
        verify(pointValueService).save(bo);
        verify(channel).basicAck(eq(7L), eq(false));
        // Counter is incremented per message — pinned so the job-side rate calculation
        // stays accurate.
        assertThat(PointValueJob.getValueCount()).isEqualTo(1);
    }

    @Test
    void aboveSpeedThresholdBuffersToScheduleAndAcks() throws Exception {
        properties.setSpeed(0);
        PointValueBO bo = PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build();
        receiver.pointValueReceive(channel, message, bo);
        verify(pointValueService, never()).save(any(PointValueBO.class));
        verify(channel).basicAck(eq(7L), eq(false));
        assertThat(PointValueJob.getPointValuesSize()).isEqualTo(1);
    }

    @Test
    void nacksAndRequeuesOnServiceFailure() throws Exception {
        PointValueBO bo = PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build();
        org.mockito.Mockito.doThrow(new RuntimeException("downstream offline"))
                .when(pointValueService).save(any(PointValueBO.class));
        receiver.pointValueReceive(channel, message, bo);
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }
}
