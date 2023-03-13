/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service.job;

import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.common.entity.point.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
public class PointValueScheduleJob extends QuartzJobBean {

    @Value("${data.point.batch.speed}")
    private Integer batchSpeed;
    @Value("${data.point.batch.interval}")
    private Integer interval;

    @Resource
    private PointValueService pointValueService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    public static final ReentrantReadWriteLock valueLock = new ReentrantReadWriteLock();
    public static final AtomicLong valueCount = new AtomicLong(0);
    public static final AtomicLong valueSpeed = new AtomicLong(0);

    private static final List<PointValue> pointValues = new ArrayList<>();

    /**
     * 获取 PointValue 长度
     *
     * @return Point Value Size
     */
    public static int getPointValuesSize() {
        return pointValues.size();
    }

    /**
     * 清空 PointValue
     */
    public static void clearPointValues() {
        pointValues.clear();
    }

    /**
     * 添加 PointValue
     *
     * @param pointValue PointValue
     */
    public static void addPointValues(PointValue pointValue) {
        pointValues.add(pointValue);
    }

    @Override
    protected void executeInternal(@NotNull JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // Statistical point value receive rate
        long speed = valueCount.getAndSet(0);
        valueSpeed.set(speed);
        speed /= interval;
        if (speed >= batchSpeed) {
            log.debug("Point value receiver speed: {} /s, value size: {}, interval: {}", speed, getPointValuesSize(), interval);
        }

        // Save point value array to Redis & MongoDB
        threadPoolExecutor.execute(() -> {
            valueLock.writeLock().lock();
            if (!pointValues.isEmpty()) {
                pointValueService.savePointValues(pointValues);
                clearPointValues();
            }
            valueLock.writeLock().unlock();
        });
    }
}
