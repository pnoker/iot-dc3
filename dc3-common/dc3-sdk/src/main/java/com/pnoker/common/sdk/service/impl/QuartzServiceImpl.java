package com.pnoker.common.sdk.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.feign.ScheduleClient;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ScheduleDto;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Schedule;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.QuartzService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class QuartzServiceImpl implements QuartzService {

    @Resource
    private Scheduler scheduler;
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private ScheduleClient scheduleClient;

    @Override
    @SneakyThrows
    public void initial() {
        List<Device> devices = deviceDriver.getDevices();
        for (Device device : devices) {
            List<Schedule> schedules = scheduleList(device);
            for (Schedule schedule : schedules) {
                runJob(device.getName(), schedule);
            }
        }
    }

    @Override
    @SneakyThrows
    public void start(String group, Schedule schedule) {
        JobKey jobKey = JobKey.jobKey(schedule.getName(), group);
        scheduler.triggerJob(jobKey);
    }

    @Override
    @SneakyThrows
    public void delete(String group, Schedule schedule) {
        JobKey jobKey = JobKey.jobKey(schedule.getName(), group);
        scheduler.deleteJob(jobKey);
    }

    @Override
    @SneakyThrows
    public void update(String group, Schedule schedule) {
        TriggerKey triggerKey = TriggerKey.triggerKey(schedule.getName(), group);
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(schedule.getCornExpression());
        trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
        scheduler.rescheduleJob(triggerKey, trigger);
    }

    @Override
    @SneakyThrows
    public void stop(String group, Schedule schedule) {
        JobKey jobKey = JobKey.jobKey(schedule.getName(), group);
        scheduler.pauseJob(jobKey);
    }

    @Override
    @SneakyThrows
    public void resume(String group, Schedule schedule) {
        JobKey jobKey = JobKey.jobKey(schedule.getName(), group);
        scheduler.resumeJob(jobKey);
    }

    @SneakyThrows
    public void runJob(String group, Schedule schedule) {
        Class<? extends Job> jobClass = (Class<? extends Job>) (Class.forName(Common.SDK_JOB_PREFIX + schedule.getBeanName()).newInstance().getClass());
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(schedule.getName(), group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(schedule.getName(), group)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCornExpression()))
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    public List<Schedule> scheduleList(Device device) {
        ScheduleDto scheduleDto = new ScheduleDto();
        scheduleDto.setDeviceId(device.getId());
        scheduleDto.setPage(new Pages().setSize(-1L));
        R<Page<Schedule>> r = scheduleClient.list(scheduleDto);
        if (r.isOk()) {
            return r.getData().getRecords();
        }
        return new ArrayList<>();
    }
}
