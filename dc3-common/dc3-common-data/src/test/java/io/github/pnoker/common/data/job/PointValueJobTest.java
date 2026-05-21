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

package io.github.pnoker.common.data.job;

import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import io.github.pnoker.common.entity.bo.PointValueBO;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointValueJobTest {

    @Mock
    private PointValueService pointValueService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private PointValueJob job;
    private ExecutorService executor;
    private PointBatchProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PointBatchProperties();
        properties.setSpeed(100);
        properties.setInterval(5);
        executor = Executors.newSingleThreadExecutor();

        job = new PointValueJob(properties, pointValueService, executor);

        PointValueJob.resetMetrics();
        PointValueJob.clearPointValues();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        PointValueJob.resetMetrics();
        PointValueJob.clearPointValues();
    }

    @Test
    void executeRotatesValueCountIntoSpeed() throws Exception {
        for (int i = 0; i < 50; i++) {
            PointValueJob.recordPointValue();
        }
        job.executeInternal(jobExecutionContext);
        assertThat(PointValueJob.getValueSpeed()).isEqualTo(10);
        assertThat(PointValueJob.getValueCount()).isEqualTo(0);
    }

    @Test
    void executeWithEmptyBufferDoesNotCallSave() throws Exception {
        job.executeInternal(jobExecutionContext);
        // Yield once so any spurious async submission would land before the assertion.
        verify(pointValueService, never().description("save must not run for an empty buffer")).save(any(List.class));
    }

    @Test
    void executeFlushesBufferedValuesAsynchronously() throws Exception {
        PointValueJob.addPointValues(PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build());
        PointValueJob.addPointValues(PointValueBO.builder().deviceId(10L).pointId(21L).rawValue("v").build());

        job.executeInternal(jobExecutionContext);

        // Saved on the executor — wait for the async submission to land.
        verify(pointValueService, timeout(Duration.ofSeconds(2).toMillis())).save(any(List.class));
        // Buffer is drained even before the async save completes.
        Awaitility.await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> assertThat(PointValueJob.getPointValuesSize()).isZero());
    }

    @Test
    void addPointValuesAppendsAndClearResetsBuffer() {
        PointValueJob.addPointValues(PointValueBO.builder().deviceId(10L).pointId(20L).rawValue("v").build());
        assertThat(PointValueJob.getPointValuesSize()).isEqualTo(1);
        PointValueJob.clearPointValues();
        assertThat(PointValueJob.getPointValuesSize()).isZero();
    }

    @Test
    void hourlyJobForDataExecutesWithoutThrowing() {
        HourlyJobForData hourly = new HourlyJobForData();
        assertThatNoException().isThrownBy(() -> hourly.executeInternal(jobExecutionContext));
    }
}
