package com.dc3.center.data.service.impl;

import com.dc3.center.data.service.ScheduleService;
import com.dc3.center.data.service.job.PointValueScheduleJob;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Value("${data.point.batch.interval}")
    private Integer interval;

    @Resource
    private Scheduler scheduler;

    @Override
    public void initial() {
        createScheduleJobWithInterval("ScheduleGroup", "PointValueScheduleJob", interval, PointValueScheduleJob.class);
        try {
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Create schedule job
     *
     * @param group    group
     * @param name     name
     * @param interval interval
     * @param jobClass class
     */
    @SneakyThrows
    public void createScheduleJobWithInterval(String group, String name, Integer interval, Class<? extends Job> jobClass) {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

}
