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
import org.quartz.*;
import org.springframework.stereotype.Component;

/**
 * Scheduler 工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class QuartzService {

    @Resource
    private Scheduler scheduler;

    /**
     * 创建调度任务
     *
     * @param group        任务分组
     * @param name         任务名称
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @param jobClass     任务执行类
     * @throws SchedulerException SchedulerException
     */
    public void createJobWithInterval(String group, String name, Integer interval, DateBuilder.IntervalUnit intervalUnit, Class<? extends Job> jobClass) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, intervalUnit))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 创建调度任务
     *
     * @param group    任务分组
     * @param name     任务名称
     * @param cron     Cron 表达式
     * @param jobClass 任务执行类
     * @throws SchedulerException SchedulerException
     */
    public void createJobWithCron(String group, String name, String cron, Class<? extends Job> jobClass) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 启动调度服务
     *
     * @throws SchedulerException SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * 关闭调度服务
     * <p>
     * 直接关闭, 不等待未执行完的任务
     *
     * @throws SchedulerException SchedulerException
     */
    public void stopScheduler() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}
