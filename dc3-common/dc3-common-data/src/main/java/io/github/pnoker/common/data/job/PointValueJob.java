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
import io.github.pnoker.common.entity.bo.PointValueBO;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class PointValueJob extends QuartzJobBean {

    public static final ReentrantReadWriteLock VALUE_LOCK = new ReentrantReadWriteLock();
    public static final AtomicLong VALUE_COUNT = new AtomicLong(0);
    public static final AtomicLong VALUE_SPEED = new AtomicLong(0);
    private static final List<PointValueBO> POINT_VALUE_LIST = new ArrayList<>();
    private final PointValueService pointValueService;
    private final ThreadPoolExecutor threadPoolExecutor;
    @Value("${data.point.batch.speed}")
    private Integer batchSpeed;
    @Value("${data.point.batch.interval}")
    private Integer interval;

    public PointValueJob(PointValueService pointValueService, ThreadPoolExecutor threadPoolExecutor) {
        this.pointValueService = pointValueService;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * 获取 PointValue 长度
     *
     * @return Point Value Size
     */
    public static int getPointValuesSize() {
        return POINT_VALUE_LIST.size();
    }

    /**
     * 清空 PointValue
     */
    public static void clearPointValues() {
        POINT_VALUE_LIST.clear();
    }

    /**
     * 添加 PointValue
     *
     * @param pointValueBO PointValue
     */
    public static void addPointValues(PointValueBO pointValueBO) {
        POINT_VALUE_LIST.add(pointValueBO);
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // Statistical point value receive rate
        long speed = VALUE_COUNT.getAndSet(0);
        VALUE_SPEED.set(speed);
        speed /= interval;
        if (speed >= batchSpeed) {
            log.debug("Point value receiver speed: {} /s, value size: {}, interval: {}", speed, getPointValuesSize(), interval);
        }

        // Save point value array to Redis & MongoDB
        threadPoolExecutor.execute(() -> {
            VALUE_LOCK.writeLock().lock();
            if (!POINT_VALUE_LIST.isEmpty()) {
                pointValueService.save(POINT_VALUE_LIST);
                clearPointValues();
            }
            VALUE_LOCK.writeLock().unlock();
        });
    }
}
