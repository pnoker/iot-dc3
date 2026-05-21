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

package io.github.pnoker.common.driver.service.impl;

import io.github.pnoker.common.constant.driver.ScheduleConstant;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.job.DriverCustomScheduleJob;
import io.github.pnoker.common.driver.job.DriverReadScheduleJob;
import io.github.pnoker.common.driver.job.DriverStatusScheduleJob;
import io.github.pnoker.common.driver.service.DriverScheduleService;
import io.github.pnoker.common.exception.CronException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.quartz.QuartzService;
import lombok.RequiredArgsConstructor;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Default {@link DriverScheduleService} implementation that validates cron settings and
 * registers the built-in Quartz jobs required by the driver runtime.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class DriverScheduleServiceImpl implements DriverScheduleService {

    private final DriverProperties driverProperties;

    private final QuartzService quartzService;

    @Override
    public void initial() {
        // Get schedule properties from driver configuration
        DriverProperties.ScheduleProperties property = driverProperties.getSchedule();
        if (Objects.isNull(property)) {
            return;
        }

        try {
            // Create and schedule the driver status monitoring job
            quartzService.createJobWithCron(ScheduleConstant.DRIVER_SCHEDULE_GROUP,
                    ScheduleConstant.DRIVER_STATUS_SCHEDULE_JOB, ScheduleConstant.DRIVER_STATUS_SCHEDULE_CRON,
                    DriverStatusScheduleJob.class);

            // Create and schedule the read job if enabled
            if (Boolean.TRUE.equals(property.getRead().getEnable())) {
                // Validate read job cron expression
                if (!CronExpression.isValidExpression(property.getRead().getCron())) {
                    throw new CronException("Read schedule cron expression is invalid");
                }
                quartzService.createJobWithCron(ScheduleConstant.DRIVER_SCHEDULE_GROUP,
                        ScheduleConstant.DRIVER_READ_SCHEDULE_JOB, property.getRead().getCron(),
                        DriverReadScheduleJob.class);
            }

            // Create and schedule the custom job if enabled
            if (Boolean.TRUE.equals(property.getCustom().getEnable())) {
                // Validate custom job cron expression
                if (!CronExpression.isValidExpression(property.getCustom().getCron())) {
                    throw new CronException("Custom schedule cron expression is invalid");
                }
                quartzService.createJobWithCron(ScheduleConstant.DRIVER_SCHEDULE_GROUP,
                        ScheduleConstant.DRIVER_CUSTOM_SCHEDULE_JOB, property.getCustom().getCron(),
                        DriverCustomScheduleJob.class);
            }

            // Start the scheduler after all jobs are configured
            quartzService.startScheduler();
        } catch (SchedulerException e) {
            throw new ServiceException("Failed to initialize driver scheduler", e);
        }
    }

}
