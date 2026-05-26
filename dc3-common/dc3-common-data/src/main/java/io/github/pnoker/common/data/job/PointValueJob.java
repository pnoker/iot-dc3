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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Quartz job for point value batch processing.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class PointValueJob extends QuartzJobBean {

    private static final ReentrantReadWriteLock VALUE_LOCK = new ReentrantReadWriteLock();

    private static final AtomicLong VALUE_COUNT = new AtomicLong(0);

    private static final AtomicLong VALUE_SPEED = new AtomicLong(0);

    private static final List<PointValueBO> POINT_VALUE_LIST = new ArrayList<>();

    private final PointBatchProperties pointBatchProperties;

    private final PointValueService pointValueService;

    private final ExecutorService virtualThreadExecutor;

    /**
     * PointValue
     *
     * @return Point Value Size
     */
    public static int getPointValuesSize() {
        VALUE_LOCK.readLock().lock();
        try {
            return POINT_VALUE_LIST.size();
        } finally {
            VALUE_LOCK.readLock().unlock();
        }
    }

    /**
     * PointValue
     */
    public static void clearPointValues() {
        VALUE_LOCK.writeLock().lock();
        try {
            POINT_VALUE_LIST.clear();
        } finally {
            VALUE_LOCK.writeLock().unlock();
        }
    }

    /**
     * PointValue
     *
     * @param pointValueBO PointValue
     */
    public static void addPointValues(PointValueBO pointValueBO) {
        VALUE_LOCK.writeLock().lock();
        try {
            POINT_VALUE_LIST.add(pointValueBO);
        } finally {
            VALUE_LOCK.writeLock().unlock();
        }
    }

    public static void recordPointValue() {
        VALUE_COUNT.getAndIncrement();
    }

    public static long getValueCount() {
        return VALUE_COUNT.get();
    }

    public static long getValueSpeed() {
        return VALUE_SPEED.get();
    }

    public static void resetMetrics() {
        VALUE_COUNT.set(0);
        VALUE_SPEED.set(0);
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // Statistical point value receive rate
        long speed = VALUE_COUNT.getAndSet(0) / pointBatchProperties.getInterval();
        VALUE_SPEED.set(speed);
        if (speed >= pointBatchProperties.getSpeed()) {
            log.debug("Point value receiver speed: {} /s, value size: {}, interval: {}", speed, getPointValuesSize(),
                    pointBatchProperties.getInterval());
        }

        // Swap out the accumulated buffer under the lock; run the save on a private
        // snapshot outside the lock so concurrent addPointValues callers are not blocked
        // by DB I/O.
        List<PointValueBO> snapshot;
        VALUE_LOCK.writeLock().lock();
        try {
            if (POINT_VALUE_LIST.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(POINT_VALUE_LIST);
            POINT_VALUE_LIST.clear();
        } finally {
            VALUE_LOCK.writeLock().unlock();
        }

        virtualThreadExecutor.execute(() -> pointValueService.save(snapshot));
    }

}
