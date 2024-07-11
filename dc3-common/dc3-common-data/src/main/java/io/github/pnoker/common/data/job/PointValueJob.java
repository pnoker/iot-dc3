/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.job;

import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
    protected void executeInternal(@NotNull JobExecutionContext jobExecutionContext) throws JobExecutionException {
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
