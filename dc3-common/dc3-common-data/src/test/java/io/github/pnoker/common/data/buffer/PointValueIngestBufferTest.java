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

package io.github.pnoker.common.data.buffer;

import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import io.github.pnoker.common.entity.bo.PointValueBO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Verifies the ingest buffer offer/flush/re-queue/back-pressure lifecycle with a mocked
 * {@link PointValueService}.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@ExtendWith(MockitoExtension.class)
class PointValueIngestBufferTest {

    @Mock
    private PointValueService pointValueService;

    private PointBatchProperties properties;
    private PointValueIngestBuffer buffer;

    @BeforeEach
    void setUp() {
        properties = new PointBatchProperties();
        properties.setQueueCapacity(1000);
        properties.setBatchSize(2);
        properties.setFlushIntervalMillis(200);
        properties.setWorkerCount(1);
        buffer = new PointValueIngestBuffer(properties, pointValueService);
        buffer.start();
    }

    @AfterEach
    void tearDown() {
        buffer.stop();
    }

    @Test
    void flushesBatchedValues() {
        buffer.offer(bo(1));
        buffer.offer(bo(2));
        verify(pointValueService, timeout(1000)).save(anyList());
    }

    @Test
    void flushesSingleRecordPromptly() {
        // Below batchSize — the first record still triggers a flush via the poll-then-drain loop.
        buffer.offer(bo(1));
        verify(pointValueService, timeout(1000)).save(anyList());
    }

    @Test
    void requeuesBatchOnSaveFailure() {
        doThrow(new RuntimeException("db down")).when(pointValueService).save(anyList());
        buffer.offer(bo(1));
        // Failed save re-queues the batch, which is then re-drained and re-saved.
        verify(pointValueService, timeout(1000).atLeast(2)).save(anyList());
    }

    @Test
    void offerReturnsFalseWhenQueueFull() throws Exception {
        PointBatchProperties small = new PointBatchProperties();
        small.setQueueCapacity(2);
        small.setBatchSize(100);
        small.setFlushIntervalMillis(10_000);
        small.setWorkerCount(1);
        PointValueIngestBuffer full = new PointValueIngestBuffer(small, pointValueService);
        full.start();
        try {
            CountDownLatch firstTaken = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            doAnswer(inv -> {
                firstTaken.countDown();
                release.await();
                return null;
            }).when(pointValueService).save(anyList());

            assertThat(full.offer(bo(1))).isTrue();
            // Wait until the worker has taken the first record and is blocked inside save(),
            // so the queue is empty and we can deterministically fill it to capacity.
            assertThat(firstTaken.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(full.offer(bo(2))).isTrue();
            assertThat(full.offer(bo(3))).isTrue();
            // Queue capacity is 2 — the 4th offer must be rejected (back-pressure signal).
            assertThat(full.offer(bo(4))).isFalse();
            release.countDown();
        } finally {
            full.stop();
        }
    }

    private PointValueBO bo(int i) {
        return PointValueBO.builder().deviceId((long) i).pointId((long) i).rawValue("v" + i).build();
    }
}
