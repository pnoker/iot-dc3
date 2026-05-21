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

import lombok.RequiredArgsConstructor;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
 * @since 2016.10.1
 */
@RequiredArgsConstructor
public class QuartzService {

    private final Scheduler scheduler;

    private static void validateIdentity(String group, String name, Class<? extends Job> jobClass) {
        if (Objects.isNull(group) || group.isBlank()) {
            throw new IllegalArgumentException("Job group must not be blank");
        }
        if (Objects.isNull(name) || name.isBlank()) {
            throw new IllegalArgumentException("Job name must not be blank");
        }
        if (Objects.isNull(jobClass)) {
            throw new IllegalArgumentException("Job class must not be null");
        }
    }

    private static long toMillis(int interval, DateBuilder.IntervalUnit intervalUnit) {
        if (Objects.isNull(intervalUnit)) {
            throw new IllegalArgumentException("Interval unit must not be null");
        }
        return switch (intervalUnit) {
            case MILLISECOND -> interval;
            case SECOND -> TimeUnit.SECONDS.toMillis(interval);
            case MINUTE -> TimeUnit.MINUTES.toMillis(interval);
            case HOUR -> TimeUnit.HOURS.toMillis(interval);
            case DAY -> TimeUnit.DAYS.toMillis(interval);
            case WEEK -> TimeUnit.DAYS.toMillis(interval * 7L);
            case MONTH, YEAR -> throw new IllegalArgumentException(
                    "Interval unit " + intervalUnit + " has variable length and is not supported");
        };
    }

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
        validateIdentity(group, name, jobClass);
        if (Objects.isNull(interval) || interval <= 0) {
            throw new IllegalArgumentException("Interval must be greater than 0");
        }
        long intervalMillis = toMillis(interval, intervalUnit);

        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(intervalMillis)
                        .repeatForever())
                .startNow()
                .build();
        deleteExistingJob(group, name);
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
        validateIdentity(group, name, jobClass);
        if (Objects.isNull(cron) || cron.isBlank()) {
            throw new IllegalArgumentException("Cron expression must not be blank");
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("Cron expression is invalid: " + cron);
        }

        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .startNow()
                .build();
        deleteExistingJob(group, name);
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * Start the scheduler service
     *
     * @throws SchedulerException SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        if (!scheduler.isShutdown() && !scheduler.isStarted()) {
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

    /**
     * Stop the scheduler service.
     *
     * @param waitForJobsToComplete whether to wait for currently executing jobs to finish
     * @throws SchedulerException SchedulerException
     */
    public void stopScheduler(boolean waitForJobsToComplete) throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown(waitForJobsToComplete);
        }
    }

    private void deleteExistingJob(String group, String name) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(name, group);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

}
