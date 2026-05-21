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

package io.github.pnoker.common.mqtt.service.job;

import io.github.pnoker.common.mqtt.entity.MessageHeader;
import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.entity.property.MqttProperties;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqttScheduleJobTest {

    @Mock
    private MqttReceiveService mqttReceiveService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private MqttScheduleJob job;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        MqttProperties properties = new MqttProperties();
        properties.getBatch().setSpeed(100);
        properties.getBatch().setInterval(5);

        executor = Executors.newSingleThreadExecutor();
        job = new MqttScheduleJob(properties, mqttReceiveService, executor);

        MqttScheduleJob.resetMetrics();
        MqttScheduleJob.clearMqttMessages();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        MqttScheduleJob.resetMetrics();
        MqttScheduleJob.clearMqttMessages();
    }

    @Test
    void executeRotatesMessageCountIntoPerSecondSpeed() throws Exception {
        for (int i = 0; i < 50; i++) {
            MqttScheduleJob.recordMessage();
        }

        job.executeInternal(jobExecutionContext);

        assertThat(MqttScheduleJob.getMessageSpeed()).isEqualTo(10);
        assertThat(MqttScheduleJob.getMessageCount()).isZero();
    }

    @Test
    void executeWithEmptyBufferDoesNotCallReceiveValues() throws Exception {
        job.executeInternal(jobExecutionContext);

        verify(mqttReceiveService, never()).receiveValues(anyList());
    }

    @Test
    void executeFlushesSnapshotAndClearsSharedBuffer() throws Exception {
        MqttScheduleJob.addMqttMessages(mqttMessage("first"));
        MqttScheduleJob.addMqttMessages(mqttMessage("second"));

        job.executeInternal(jobExecutionContext);

        ArgumentCaptor<List<MqttMessage>> captor = mqttMessageListCaptor();
        verify(mqttReceiveService, timeout(Duration.ofSeconds(2).toMillis())).receiveValues(captor.capture());
        assertThat(captor.getValue()).extracting(MqttMessage::getPayload).containsExactly("first", "second");
        assertThat(MqttScheduleJob.getMqttMessagesSize()).isZero();
    }

    @Test
    void addAndClearAreThreadSafeFacadeMethods() {
        MqttScheduleJob.addMqttMessages(mqttMessage("payload"));
        assertThat(MqttScheduleJob.getMqttMessagesSize()).isEqualTo(1);

        MqttScheduleJob.clearMqttMessages();

        assertThat(MqttScheduleJob.getMqttMessagesSize()).isZero();
    }

    private MqttMessage mqttMessage(String payload) {
        return MqttMessage.builder().header(new MessageHeader(null)).payload(payload).build();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<MqttMessage>> mqttMessageListCaptor() {
        return ArgumentCaptor.forClass((Class<List<MqttMessage>>) (Class<?>) List.class);
    }

}
