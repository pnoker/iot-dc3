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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.driver.ScheduleConstant;
import io.github.pnoker.common.data.biz.ScheduleService;
import io.github.pnoker.common.data.job.HourlyJob;
import io.github.pnoker.common.data.job.PointValueJob;
import io.github.pnoker.common.quartz.QuartzService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DateBuilder;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Value("${data.point.batch.interval}")
    private Integer interval;

    @Resource
    private QuartzService quartzService;

    @Override
    public void initial() {
        try {
            quartzService.createJobWithInterval(ScheduleConstant.DATA_SCHEDULE_GROUP, "data-point-value-schedule-job", interval, DateBuilder.IntervalUnit.SECOND, PointValueJob.class);

            // 自定义调度
            quartzService.createJobWithCron(ScheduleConstant.DATA_SCHEDULE_GROUP, "hourly-job", "0 0 0/1 * * ?", HourlyJob.class);

            quartzService.startScheduler();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
