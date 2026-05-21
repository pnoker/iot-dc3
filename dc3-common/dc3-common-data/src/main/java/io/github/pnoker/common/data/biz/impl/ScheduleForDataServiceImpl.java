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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.driver.ScheduleConstant;
import io.github.pnoker.common.data.biz.ScheduleForDataService;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import io.github.pnoker.common.data.job.HourlyJobForData;
import io.github.pnoker.common.data.job.PointValueJob;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.quartz.QuartzService;
import lombok.RequiredArgsConstructor;
import org.quartz.DateBuilder;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

/**
 * Business service implementation for data-center scheduled jobs.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class ScheduleForDataServiceImpl implements ScheduleForDataService {

    private final PointBatchProperties pointBatchProperties;

    private final QuartzService quartzService;

    /**
     * Initialize data scheduling
     */
    @Override
    public void initial() {
        try {
            quartzService.createJobWithInterval(ScheduleConstant.DATA_SCHEDULE_GROUP, "data-point-value-schedule-job",
                    pointBatchProperties.getInterval(), DateBuilder.IntervalUnit.SECOND, PointValueJob.class);

            // Custom scheduling
            quartzService.createJobWithCron(ScheduleConstant.DATA_SCHEDULE_GROUP, "hourly-job", "0 0 0/1 * * ?",
                    HourlyJobForData.class);

            quartzService.startScheduler();
        } catch (SchedulerException e) {
            throw new ServiceException("Failed to initialize data scheduler", e);
        }
    }

}
