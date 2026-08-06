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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded in-memory buffer that decouples point-value consumption from repository persistence.
 *
 * <p>Replaces the legacy speed-threshold dual path (single-row save vs. unbounded list + Quartz
 * tick). Every received point value enters a bounded {@link ArrayBlockingQueue}; worker threads
 * drain it on a size-or-time trigger and call {@link PointValueService#save(List)}. When the
 * queue is full {@link #offer} returns {@code false} so the receiver can nack-requeue and
 * back-pressure RabbitMQ instead of OOM-ing. A failed save re-queues the batch for retry.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2026.7.8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueIngestBuffer {

    private final PointBatchProperties pointBatchProperties;
    private final PointValueService pointValueService;
    private final AtomicLong droppedCount = new AtomicLong(0);
    private ArrayBlockingQueue<PointValueBO> queue;
    private ExecutorService worker;
    private volatile boolean running;

    /**
     * Start the worker pool and begin draining.
     */
    @PostConstruct
    void start() {
        queue = new ArrayBlockingQueue<>(pointBatchProperties.getQueueCapacity());
        int workers = pointBatchProperties.getWorkerCount();
        worker = Executors.newFixedThreadPool(workers, r -> {
            Thread thread = new Thread(r, "dc3-point-value-ingest");
            thread.setDaemon(true);
            return thread;
        });
        running = true;
        for (int i = 0; i < workers; i++) {
            worker.submit(this::drainLoop);
        }
        log.info("PointValueIngestBuffer started, queueCapacity={}, batchSize={}, flushIntervalMillis={}, workerCount={}",
                pointBatchProperties.getQueueCapacity(), pointBatchProperties.getBatchSize(),
                pointBatchProperties.getFlushIntervalMillis(), workers);
    }

    /**
     * Stop workers and flush whatever remains in the queue.
     */
    @PreDestroy
    void stop() {
        running = false;
        if (Objects.nonNull(worker)) {
            worker.shutdown();
            try {
                worker.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                worker.shutdownNow();
            }
        }
        if (Objects.nonNull(queue)) {
            List<PointValueBO> remaining = new ArrayList<>();
            queue.drainTo(remaining);
            if (!remaining.isEmpty()) {
                log.warn("PointValueIngestBuffer flushing {} remaining records on shutdown", remaining.size());
                try {
                    pointValueService.save(remaining);
                } catch (Exception e) {
                    log.error("PointValueIngestBuffer failed to flush {} remaining records on shutdown",
                            remaining.size(), e);
                }
            }
        }
    }

    /**
     * Enqueue a point value without blocking.
     *
     * @param pointValueBO the value to buffer
     * @return {@code true} if accepted, {@code false} if the queue is full (caller should nack-requeue)
     */
    public boolean offer(PointValueBO pointValueBO) {
        return queue.offer(pointValueBO);
    }

    /**
     * @return number of point values currently buffered, awaiting the next flush
     */
    public int pendingCount() {
        return Objects.nonNull(queue) ? queue.size() : 0;
    }

    /**
     * @return cumulative count of records dropped because the queue was full on re-queue
     */
    public long droppedCount() {
        return droppedCount.get();
    }

    /**
     * Worker loop: block on the first record (up to the flush interval), then non-blockingly
     * drain up to {@code batchSize-1} more, then persist. Triggers on either a full batch or
     * the flush-interval timeout.
     */
    private void drainLoop() {
        int batchSize = pointBatchProperties.getBatchSize();
        long flushMillis = pointBatchProperties.getFlushIntervalMillis();
        while (running) {
            try {
                PointValueBO first = queue.poll(flushMillis, TimeUnit.MILLISECONDS);
                if (Objects.isNull(first)) {
                    continue;
                }
                List<PointValueBO> batch = new ArrayList<>(batchSize);
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);
                saveWithRetry(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("PointValueIngestBuffer drain loop error", e);
            }
        }
    }

    /**
     * Persist a batch; on failure, re-queue it for retry so a transient DB outage does not
     * lose data.
     */
    private void saveWithRetry(List<PointValueBO> batch) {
        try {
            pointValueService.save(batch);
        } catch (Exception e) {
            log.error("Save point values batch failed, size={}, re-queuing for retry", batch.size(), e);
            requeue(batch);
        }
    }

    /**
     * Re-queue a failed batch entry by entry. If the queue is full (sustained DB outage with
     * continued inflow), drop the record and count it so it surfaces in monitoring.
     */
    private void requeue(List<PointValueBO> batch) {
        for (PointValueBO pointValueBO : batch) {
            if (!queue.offer(pointValueBO)) {
                long dropped = droppedCount.incrementAndGet();
                log.error("PointValueIngestBuffer re-queue full, dropping record, deviceId={}, pointId={}, totalDropped={}",
                        pointValueBO.getDeviceId(), pointValueBO.getPointId(), dropped);
            }
        }
    }
}
