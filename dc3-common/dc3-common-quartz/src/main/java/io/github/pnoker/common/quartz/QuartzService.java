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

package io.github.pnoker.common.quartz;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Quartz Scheduler Service Utility
 * <p>
 * Service class for managing Quartz scheduler operations in Spring Boot applications.
 * Provides methods for creating jobs with intervals and cron expressions, as well as
 * starting and stopping the scheduler. Supports flexible job scheduling with different
 * time-based triggers.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@AutoConfiguration
public class QuartzService {

    @Resource
    private Scheduler scheduler;

    /**
     * Create scheduled job with interval trigger
     *
     * @param group        Task group for job organization
     * @param name         Task name for job identification
     * @param interval     Time interval between job executions
     * @param intervalUnit Time unit for the interval
     * @param jobClass     Job execution class
     * @throws SchedulerException SchedulerException
     */
    public void createJobWithInterval(String group, String name, Integer interval,
                                      DateBuilder.IntervalUnit intervalUnit, Class<? extends Job> jobClass) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, intervalUnit))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .startNow()
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * Create scheduled job with cron trigger
     *
     * @param group    Task group for job organization
     * @param name     Task name for job identification
     * @param cron     Cron expression for scheduling
     * @param jobClass Job execution class
     * @throws SchedulerException SchedulerException
     */
    public void createJobWithCron(String group, String name, String cron, Class<? extends Job> jobClass)
            throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .startNow()
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * Start the scheduler service
     *
     * @throws SchedulerException SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * Stop the scheduler service
     * <p>
     * Shutdown immediately without waiting for currently executing jobs to complete
     *
     * @throws SchedulerException SchedulerException
     */
    public void stopScheduler() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}
